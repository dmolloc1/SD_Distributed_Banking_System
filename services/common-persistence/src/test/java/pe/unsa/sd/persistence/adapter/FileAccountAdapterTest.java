package pe.unsa.sd.persistence.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.unsa.sd.persistence.domain.model.Account;
import pe.unsa.sd.persistence.exception.InsufficientFundsException;
import pe.unsa.sd.persistence.exception.LockAcquisitionException;
import pe.unsa.sd.persistence.exception.StorageException;
import pe.unsa.sd.persistence.io.AtomicFileWriter;
import pe.unsa.sd.persistence.journal.JournalingService;
import pe.unsa.sd.persistence.journal.TransactionLogEntry;
import pe.unsa.sd.persistence.locking.LockManager;

@ExtendWith(MockitoExtension.class)
class FileAccountAdapterTest {

    @Mock
    private LockManager lockManager;

    @Mock
    private AtomicFileWriter atomicFileWriter;

    @Mock
    private JournalingService journalingService;

    @Mock
    private AutoCloseable mockLock;

    private ObjectMapper objectMapper;

    private FileAccountAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new FileAccountAdapter(lockManager, atomicFileWriter, journalingService, objectMapper);
        ReflectionTestUtils.setField(adapter, "baseDir", tempDir.toString());
    }

    @Test
    void testUpdateBalanceAtomic_Success_Credit() throws Exception {
        // Arrange
        String accountId = "12345";
        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal amount = new BigDecimal("50.00");
        String operationType = "CREDIT";
        String transactionId = "tx-1";

        setupAccountFile(accountId, initialBalance);
        when(lockManager.acquireExclusiveLock()).thenReturn(mockLock);

        // Act
        Account updatedAccount = adapter.updateBalanceAtomic(accountId, amount, operationType, transactionId);

        // Assert
        assertNotNull(updatedAccount);
        assertEquals(new BigDecimal("150.00"), updatedAccount.getBalance());

        // Verificar bloqueo
        verify(lockManager).acquireExclusiveLock();
        verify(mockLock).close();

        // Verificar registro en journal
        verify(journalingService).appendEntry(any(Path.class), any(TransactionLogEntry.class));

        // Verificar escritura atómica
        verify(atomicFileWriter).writeAtomic(any(Path.class), eq(updatedAccount));
    }

    @Test
    void testUpdateBalanceAtomic_Success_Debit() throws Exception {
        // Arrange
        String accountId = "12345";
        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal amount = new BigDecimal("-50.00");
        String operationType = "DEBIT";
        String transactionId = "tx-2";

        setupAccountFile(accountId, initialBalance);
        when(lockManager.acquireExclusiveLock()).thenReturn(mockLock);

        // Act
        Account updatedAccount = adapter.updateBalanceAtomic(accountId, amount, operationType, transactionId);

        // Assert
        assertEquals(new BigDecimal("50.00"), updatedAccount.getBalance());
        verify(atomicFileWriter).writeAtomic(any(Path.class), eq(updatedAccount));
    }

    @Test
    void testUpdateBalanceAtomic_InsufficientFunds_ThrowsException() throws Exception {
        // Arrange
        String accountId = "12345";
        BigDecimal initialBalance = new BigDecimal("100.00");
        BigDecimal amount = new BigDecimal("-150.00"); // Mayor que el saldo
        String operationType = "DEBIT";
        String transactionId = "tx-3";

        setupAccountFile(accountId, initialBalance);
        when(lockManager.acquireExclusiveLock()).thenReturn(mockLock);

        // Act & Assert
        assertThrows(InsufficientFundsException.class, () -> 
            adapter.updateBalanceAtomic(accountId, amount, operationType, transactionId)
        );

        // Verificar que el bloqueo se libera
        verify(mockLock).close();
        
        // Verificar que NO se llamó al journal ni a la escritura
        verify(journalingService, never()).appendEntry(any(), any());
        verify(atomicFileWriter, never()).writeAtomic(any(), any());
    }

    @Test
    void testUpdateBalanceAtomic_LockAcquisitionFails_ThrowsException() throws Exception {
        // Arrange
        String accountId = "12345";
        when(lockManager.acquireExclusiveLock()).thenThrow(new LockAcquisitionException("Timeout"));

        // Act & Assert
        assertThrows(LockAcquisitionException.class, () -> 
            adapter.updateBalanceAtomic(accountId, new BigDecimal("10"), "CREDIT", "tx-4")
        );
    }

    @Test
    void testUpdateBalanceAtomic_AccountFileNotFound_ThrowsStorageException() throws Exception {
        // Arrange
        String accountId = "99999"; // El archivo no existe
        when(lockManager.acquireExclusiveLock()).thenReturn(mockLock);

        // Act & Assert
        assertThrows(StorageException.class, () -> 
            adapter.updateBalanceAtomic(accountId, new BigDecimal("10"), "CREDIT", "tx-5")
        );
        
        verify(mockLock).close();
    }

    /**
     * Configura un archivo de cuenta con el saldo especificado.
     */
    private void setupAccountFile(String accountId, BigDecimal balance) throws Exception {
        Path ledgerDir = tempDir.resolve("ledger");
        Files.createDirectories(ledgerDir);
        File accountFile = ledgerDir.resolve("account_" + accountId + ".json").toFile();
        
        Account account = Account.builder()
                .accountId(accountId)
                .balance(balance)
                .currency("PEN")
                .build();
                
        objectMapper.writeValue(accountFile, account);
    }
}