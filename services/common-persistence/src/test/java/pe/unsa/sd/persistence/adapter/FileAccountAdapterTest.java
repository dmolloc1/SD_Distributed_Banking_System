package pe.unsa.sd.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import pe.unsa.sd.persistence.domain.model.Account;
import pe.unsa.sd.persistence.exception.InsufficientFundsException;
import pe.unsa.sd.persistence.exception.LockAcquisitionException;
import pe.unsa.sd.persistence.exception.StorageException;
import pe.unsa.sd.persistence.io.AtomicFileWriter;
import pe.unsa.sd.persistence.journal.JournalingService;
import pe.unsa.sd.persistence.journal.TransactionLogEntry;
import pe.unsa.sd.persistence.locking.LockManager;

class FileAccountAdapterTest {

    private FakeLockManager lockManager;
    private FakeAtomicFileWriter atomicFileWriter;
    private FakeJournalingService journalingService;
    private ObjectMapper objectMapper;
    private FileAccountAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        lockManager = new FakeLockManager();
        atomicFileWriter = new FakeAtomicFileWriter();
        journalingService = new FakeJournalingService();
        objectMapper = new ObjectMapper();
        adapter = new FileAccountAdapter(lockManager, atomicFileWriter, journalingService, objectMapper);
        ReflectionTestUtils.setField(adapter, "baseDir", tempDir.toString());
    }

    @Test
    void testUpdateBalanceAtomicSuccessCredit() throws Exception {
        String accountId = "12345";
        setupAccountFile(accountId, new BigDecimal("100.00"));

        Account updatedAccount = adapter.updateBalanceAtomic(
                accountId, new BigDecimal("50.00"), "CREDIT", "tx-1");

        assertNotNull(updatedAccount);
        assertEquals(new BigDecimal("150.00"), updatedAccount.getBalance());
        assertEquals(1, lockManager.acquireCount);
        assertEquals(1, lockManager.closeCount);
        assertEquals(1, journalingService.appendCount);
        assertEquals(1, atomicFileWriter.writeCount);
        assertEquals(updatedAccount, atomicFileWriter.lastData);
    }

    @Test
    void testUpdateBalanceAtomicSuccessDebit() throws Exception {
        String accountId = "12345";
        setupAccountFile(accountId, new BigDecimal("100.00"));

        Account updatedAccount = adapter.updateBalanceAtomic(
                accountId, new BigDecimal("-50.00"), "DEBIT", "tx-2");

        assertEquals(new BigDecimal("50.00"), updatedAccount.getBalance());
        assertEquals(1, atomicFileWriter.writeCount);
    }

    @Test
    void testUpdateBalanceAtomicInsufficientFundsThrowsException() throws Exception {
        String accountId = "12345";
        setupAccountFile(accountId, new BigDecimal("100.00"));

        assertThrows(InsufficientFundsException.class,
                () -> adapter.updateBalanceAtomic(accountId, new BigDecimal("-150.00"), "DEBIT", "tx-3"));

        assertEquals(1, lockManager.closeCount);
        assertEquals(0, journalingService.appendCount);
        assertEquals(0, atomicFileWriter.writeCount);
    }

    @Test
    void testUpdateBalanceAtomicLockAcquisitionFailsThrowsException() {
        lockManager.failOnAcquire = true;

        assertThrows(LockAcquisitionException.class,
                () -> adapter.updateBalanceAtomic("12345", new BigDecimal("10"), "CREDIT", "tx-4"));
    }

    @Test
    void testUpdateBalanceAtomicAccountFileNotFoundThrowsStorageException() {
        assertThrows(StorageException.class,
                () -> adapter.updateBalanceAtomic("99999", new BigDecimal("10"), "CREDIT", "tx-5"));

        assertEquals(1, lockManager.closeCount);
    }

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

    private static class FakeLockManager implements LockManager {
        private int acquireCount;
        private int closeCount;
        private boolean failOnAcquire;

        @Override
        public AutoCloseable acquireExclusiveLock() throws LockAcquisitionException {
            acquireCount++;
            if (failOnAcquire) {
                throw new LockAcquisitionException("Timeout");
            }
            return () -> closeCount++;
        }

        @Override
        public void close() {
        }
    }

    private static class FakeAtomicFileWriter implements AtomicFileWriter {
        private int writeCount;
        private Path lastPath;
        private Object lastData;

        @Override
        public void writeAtomic(Path targetPath, Object data) {
            writeCount++;
            lastPath = targetPath;
            lastData = data;
        }
    }

    private static class FakeJournalingService implements JournalingService {
        private int appendCount;
        private Path lastPath;
        private TransactionLogEntry lastEntry;

        @Override
        public void appendEntry(Path logPath, TransactionLogEntry entry) {
            appendCount++;
            lastPath = logPath;
            lastEntry = entry;
        }

        @Override
        public java.util.List<TransactionLogEntry> readHistory(Path logPath) {
            return java.util.List.of();
        }
    }
}
