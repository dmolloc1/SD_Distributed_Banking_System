package pe.unsa.sd.persistence.recovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.unsa.sd.persistence.domain.model.Account;
import pe.unsa.sd.persistence.journal.JournalingService;
import pe.unsa.sd.persistence.journal.TransactionLogEntry;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {

    @Mock
    private JournalingService journalingService;

    private ObjectMapper objectMapper;
    private RecoveryService recoveryService;

    @TempDir
    Path tempDir;

    private Path ledgerDir;
    private String lockFilePath;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        recoveryService = new RecoveryService(journalingService, objectMapper);
        ReflectionTestUtils.setField(recoveryService, "baseDir", tempDir.toString());
        
        lockFilePath = tempDir.resolve(".accounts.json.lock").toString();
        ReflectionTestUtils.setField(recoveryService, "globalLockFile", lockFilePath);

        ledgerDir = tempDir.resolve("ledger");
        Files.createDirectories(ledgerDir);
    }

    @Test
    void testPerformBootRecovery_CleansOrphanLockFile() throws Exception {
        // Arrange
        Files.createFile(Path.of(lockFilePath));
        assertTrue(Files.exists(Path.of(lockFilePath)));

        // Act
        recoveryService.performBootRecovery();

        // Assert
        assertFalse(Files.exists(Path.of(lockFilePath)), "El archivo lock debió ser eliminado");
    }

    @Test
    void testPerformBootRecovery_RollbackOrphanTmpFile_WhenNoJournalEntry() throws Exception {
        // Arrange
        String accountId = "111";
        Path tmpPath = ledgerDir.resolve("account_" + accountId + ".tmp");
        Account tmpAccount = Account.builder().accountId(accountId).balance(new BigDecimal("100")).build();
        objectMapper.writeValue(tmpPath.toFile(), tmpAccount);
        
        when(journalingService.readHistory(any())).thenReturn(List.of());

        // Act
        recoveryService.performBootRecovery();

        // Assert
        assertFalse(Files.exists(tmpPath), "El archivo .tmp debió ser eliminado al no haber registro en el journal");
    }

    @Test
    void testPerformBootRecovery_CommitOrphanTmpFile_WhenMatchesJournalEntry() throws Exception {
        // Arrange
        String accountId = "222";
        Path tmpPath = ledgerDir.resolve("account_" + accountId + ".tmp");
        Path originalPath = ledgerDir.resolve("account_" + accountId + ".json");
        
        BigDecimal expectedBalance = new BigDecimal("200");
        Account tmpAccount = Account.builder().accountId(accountId).balance(expectedBalance).build();
        objectMapper.writeValue(tmpPath.toFile(), tmpAccount);
        
        TransactionLogEntry logEntry = TransactionLogEntry.builder()
                .accountId(accountId)
                .newBalance(expectedBalance)
                .build();
                
        when(journalingService.readHistory(any())).thenReturn(List.of(logEntry));

        // Act
        recoveryService.performBootRecovery();

        // Assert
        assertFalse(Files.exists(tmpPath), "El archivo .tmp debió desaparecer");
        assertTrue(Files.exists(originalPath), "El archivo original debió ser creado a partir del .tmp");
    }
    
    @Test
    void testPerformBootRecovery_IntegrityFails_AutoRestoresFromJson() throws Exception {
        // Arrange
        String accountId = "333";
        Path jsonPath = ledgerDir.resolve("account_" + accountId + ".json");
        
        // El JSON tiene 100 de saldo (corrupto/desactualizado)
        Account corruptAccount = Account.builder().accountId(accountId).balance(new BigDecimal("100")).build();
        objectMapper.writeValue(jsonPath.toFile(), corruptAccount);
        
        // El Journal dice que el saldo real es 500
        BigDecimal correctBalance = new BigDecimal("500");
        TransactionLogEntry logEntry = TransactionLogEntry.builder()
                .accountId(accountId)
                .newBalance(correctBalance)
                .build();
                
        when(journalingService.readHistory(any())).thenReturn(List.of(logEntry));

        // Act
        recoveryService.performBootRecovery();

        // Assert
        Account restoredAccount = objectMapper.readValue(jsonPath.toFile(), Account.class);
        assertEquals(correctBalance, restoredAccount.getBalance(), "El saldo debió ser auto-restaurado desde el Journal");
    }
}
