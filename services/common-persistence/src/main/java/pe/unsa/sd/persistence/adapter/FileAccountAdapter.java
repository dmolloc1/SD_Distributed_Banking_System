package pe.unsa.sd.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.unsa.sd.persistence.domain.model.Account;
import pe.unsa.sd.persistence.domain.ports.AccountPersistencePort;
import pe.unsa.sd.persistence.exception.InsufficientFundsException;
import pe.unsa.sd.persistence.exception.StorageException;
import pe.unsa.sd.persistence.io.AtomicFileWriter;
import pe.unsa.sd.persistence.journal.JournalingService;
import pe.unsa.sd.persistence.journal.TransactionLogEntry;
import pe.unsa.sd.persistence.locking.LockManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileAccountAdapter implements AccountPersistencePort {

    private final LockManager lockManager;
    private final AtomicFileWriter atomicFileWriter;
    private final JournalingService journalingService;
    private final ObjectMapper objectMapper;

    @Value("${bank.data.base-dir:data}")
    private String baseDir;

    @Override
    public Account updateBalanceAtomic(String accountId, BigDecimal amount, String operationType, String transactionId) throws StorageException {
        // 1. Adquirir bloqueo exclusivo del archivo
        try (AutoCloseable lock = lockManager.acquireExclusiveLock()) {
            
            Path accountFilePath = Path.of(baseDir, "ledger", "account_" + accountId + ".json");
            
            // 2. Leer y validar existencia de la cuenta
            if (!Files.exists(accountFilePath)) {
                throw new StorageException("El archivo de la cuenta no existe: " + accountFilePath);
            }
            
            Account account;
            try {
                account = objectMapper.readValue(accountFilePath.toFile(), Account.class);
            } catch (IOException e) {
                throw new StorageException("Error al leer el archivo de la cuenta: " + accountFilePath, e);
            }
            
            BigDecimal previousBalance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            BigDecimal newBalance = previousBalance.add(amount);
            
            // Validar saldo insuficiente (si el monto es negativo y el nuevo saldo es negativo)
            if (amount.compareTo(BigDecimal.ZERO) < 0 && newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientFundsException("Saldo insuficiente para la cuenta: " + accountId);
            }
            
            account.setBalance(newBalance);
            
            // 3. Registrar en el journal (auditoría)
            Path journalPath = Path.of(baseDir, "journal", "transactions.log");
            
            // Generar checksum 
            String checksum = UUID.randomUUID().toString().substring(0, 8); 
            
            TransactionLogEntry logEntry = TransactionLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .transactionId(transactionId != null ? transactionId : UUID.randomUUID().toString())
                .accountId(accountId)
                .operationType(operationType)
                .amount(amount)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .checksum(checksum)
                .build();
                
            journalingService.appendEntry(journalPath, logEntry);
            
            // 4. Escritura atómica 
            atomicFileWriter.writeAtomic(accountFilePath, account);
            
            return account;
            
        } catch (InsufficientFundsException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof StorageException) {
                throw (StorageException) e;
            }
            throw new StorageException("Error durante la actualización atómica de saldo para la cuenta: " + accountId, e);
        }
    }
}