package pe.unsa.sd.persistence.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.unsa.sd.persistence.domain.model.Account;
import pe.unsa.sd.persistence.exception.StorageException;
import pe.unsa.sd.persistence.journal.JournalingService;
import pe.unsa.sd.persistence.journal.TransactionLogEntry;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final JournalingService journalingService;
    private final ObjectMapper objectMapper;

    @Value("${bank.data.base-dir:data}")
    private String baseDir;
    
    @Value("${bank.data.accounts-lock-file:data/.accounts.json.lock}")
    private String globalLockFile;

    @EventListener(ApplicationReadyEvent.class)
    public void performBootRecovery() {
        log.info("Iniciando Boot Recovery (Crash Recovery)...");
        
        // 1. Limpiar global lock si existe tras un crash
        Path lockPath = Path.of(globalLockFile);
        if (Files.exists(lockPath)) {
            try {
                Files.delete(lockPath);
                log.info("Archivo .lock global huérfano eliminado.");
            } catch (IOException e) {
                log.warn("No se pudo eliminar el archivo .lock global", e);
            }
        }
        
        Path ledgerDir = Path.of(baseDir, "ledger");
        if (!Files.exists(ledgerDir)) {
            log.info("Directorio ledger no existe, saltando recovery.");
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ledgerDir)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                
                // Procesar temporales huérfanos (.tmp) de Shadow Paging
                if (fileName.endsWith(".tmp")) {
                    processOrphanTmpFile(path);
                } 
                // Verificar integridad de archivos principales (.json)
                else if (fileName.endsWith(".json")) {
                    verifyIntegrity(path);
                }
            }
        } catch (IOException e) {
            log.error("Error durante Boot Recovery", e);
            throw new StorageException("Error en Boot Recovery", e);
        }
        
        log.info("Boot Recovery finalizado con éxito.");
    }

    private void processOrphanTmpFile(Path tmpPath) throws IOException {
        String fileName = tmpPath.getFileName().toString();
        String accountId = extractAccountId(fileName.replace(".tmp", ".json"));
        
        Path originalPath = tmpPath.resolveSibling("account_" + accountId + ".json");
        
        log.info("Archivo .tmp huérfano detectado para la cuenta: {}", accountId);
        
        List<TransactionLogEntry> history = getAccountHistory(accountId);
        if (history.isEmpty()) {
            log.warn("No hay historial en el journal para {}. Eliminando .tmp (Rollback).", accountId);
            Files.delete(tmpPath);
            return;
        }
        
        TransactionLogEntry lastEntry = history.get(history.size() - 1);
        Account tmpAccount;
        try {
            tmpAccount = objectMapper.readValue(tmpPath.toFile(), Account.class);
        } catch (Exception e) {
            log.error("El archivo .tmp de {} está corrupto. Eliminando.", accountId);
            Files.delete(tmpPath);
            return;
        }
        
        // Si el saldo del .tmp coincide con el último registro del journal, la transacción se había confirmado (Commit)
        if (tmpAccount.getBalance() != null && tmpAccount.getBalance().compareTo(lastEntry.getNewBalance()) == 0) {
            log.info("El .tmp de {} coincide con el Journal. Consolidando archivo (Commit).", accountId);
            
            // Asegurar que el directorio existe antes de mover
            if (originalPath.getParent() != null) {
                Files.createDirectories(originalPath.getParent());
            }
            
            Files.move(tmpPath, originalPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archivo consolidado exitosamente: {}", originalPath);
        } else {
            log.info("El .tmp de {} NO coincide con el Journal o fue parcial. Eliminando temporal (Rollback).", accountId);
            Files.delete(tmpPath);
        }
    }

    private void verifyIntegrity(Path jsonPath) throws IOException {
        String accountId = extractAccountId(jsonPath.getFileName().toString());
        Account account;
        try {
            account = objectMapper.readValue(jsonPath.toFile(), Account.class);
        } catch (Exception e) {
            log.error("DIAGNÓSTICO DE INTEGRIDAD: Fallo. Archivo JSON corrupto para {}", accountId);
            log.info("Auto-Restaurando cuenta {} desde el Journal...", accountId);
            restoreFromLedger(accountId);
            return;
        }
        
        List<TransactionLogEntry> history = getAccountHistory(accountId);
        if (history.isEmpty()) return;
        
        TransactionLogEntry lastEntry = history.get(history.size() - 1);
        
        if (account.getBalance() == null || account.getBalance().compareTo(lastEntry.getNewBalance()) != 0) {
            log.error("DIAGNÓSTICO DE INTEGRIDAD: Fallo para la cuenta {}. Saldo JSON={} pero Journal={}", 
                      accountId, account.getBalance(), lastEntry.getNewBalance());
            log.info("Auto-Restaurando cuenta {} desde el Journal...", accountId);
            restoreFromLedger(accountId);
        } else {
            log.info("DIAGNÓSTICO DE INTEGRIDAD: Cuenta {} OK.", accountId);
        }
    }

    public void restoreFromLedger(String accountId) {
        log.info("Iniciando restauración manual desde el Journal (Source of Truth) para la cuenta: {}", accountId);
        List<TransactionLogEntry> history = getAccountHistory(accountId);
        
        if (history.isEmpty()) {
            throw new StorageException("No se encontró historial en el Journal para la cuenta " + accountId);
        }
        
        TransactionLogEntry lastEntry = history.get(history.size() - 1);
        
        Path accountPath = Path.of(baseDir, "ledger", "account_" + accountId + ".json");
        
        Account account;
        if (Files.exists(accountPath)) {
            try {
                account = objectMapper.readValue(accountPath.toFile(), Account.class);
            } catch (IOException e) {
                account = Account.builder()
                        .accountId(accountId)
                        .currency("PEN") 
                        .build();
            }
        } else {
            account = Account.builder()
                    .accountId(accountId)
                    .currency("PEN")
                    .build();
        }
        
        account.setBalance(lastEntry.getNewBalance());
        
        try {
            if (accountPath.getParent() != null) {
                Files.createDirectories(accountPath.getParent());
            }
            objectMapper.writeValue(accountPath.toFile(), account);
            log.info("Restauración exitosa para la cuenta: {}. Nuevo saldo reconstruido: {}", accountId, account.getBalance());
        } catch (IOException e) {
            throw new StorageException("Fallo al restaurar la cuenta " + accountId + " desde el Journal", e);
        }
    }

    private List<TransactionLogEntry> getAccountHistory(String accountId) {
        Path journalPath = Path.of(baseDir, "journal", "transactions.log");
        return journalingService.readHistory(journalPath)
                .stream()
                .filter(entry -> accountId.equals(entry.getAccountId()))
                .toList();
    }
    
    private String extractAccountId(String fileName) {
        return fileName.replace("account_", "").replace(".json", "");
    }
}
