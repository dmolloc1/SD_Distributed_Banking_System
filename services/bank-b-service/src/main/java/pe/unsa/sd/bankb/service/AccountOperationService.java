package pe.unsa.sd.bankb.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.unsa.sd.bankb.exception.InsufficientFundsException;
import pe.unsa.sd.bankb.exception.InvalidAmountException;
import pe.unsa.sd.bankb.model.Account;
import pe.unsa.sd.bankb.model.Transaction;
import pe.unsa.sd.persistence.exception.StorageException;
import pe.unsa.sd.persistence.io.JsonAtomicFileWriter;
import pe.unsa.sd.persistence.journal.FileJournalingService;
import pe.unsa.sd.persistence.journal.TransactionLogEntry;
import pe.unsa.sd.persistence.locking.NioFileLockManager;

@Service
public class AccountOperationService {

    private static final int MONEY_SCALE = 2;

    private final AccountValidationService accountValidationService;
    private final FileAccountService fileAccountService;
    private final NioFileLockManager lockManager;
    private final JsonAtomicFileWriter atomicFileWriter;
    private final FileJournalingService journalingService;
    private final Path transactionsLogPath;

    public AccountOperationService(
            AccountValidationService accountValidationService,
            FileAccountService fileAccountService,
            NioFileLockManager lockManager,
            JsonAtomicFileWriter atomicFileWriter,
            FileJournalingService journalingService,
            @Value("${bank.data.transactions-log}") String transactionsLogPath) {
        this.accountValidationService = accountValidationService;
        this.fileAccountService = fileAccountService;
        this.lockManager = lockManager;
        this.atomicFileWriter = atomicFileWriter;
        this.journalingService = journalingService;
        this.transactionsLogPath = Path.of(transactionsLogPath);
    }

    public Transaction deposit(String accountId, BigDecimal amount) {
        Account account = loadAccount(accountId);
        BigDecimal normalizedAmount = normalizeAmount(amount);
        accountValidationService.validateDeposit(account, normalizedAmount);

        UUID transactionId = UUID.randomUUID();

        try (AutoCloseable ignored = lockManager.acquireExclusiveLock()) {
            List<Account> accounts = loadAllAccounts();
            Account lockedAccount = findAccount(accounts, accountId);

            BigDecimal balanceBefore = normalizeAmount(lockedAccount.getBalance());
            BigDecimal balanceAfter =
                    accountValidationService.calculateNewBalance(lockedAccount, "DEPOSIT", normalizedAmount);

            appendJournalEntry(
                    transactionId.toString(),
                    accountId,
                    "CREDIT",
                    normalizedAmount,
                    balanceBefore,
                    balanceAfter);

            lockedAccount.setBalance(balanceAfter);
            atomicFileWriter.writeAtomic(fileAccountService.getAccountsPath(), accounts);

            return buildTransaction(
                    transactionId,
                    accountId,
                    "CREDIT",
                    normalizedAmount,
                    balanceBefore,
                    balanceAfter,
                    "SUCCESS");
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Error al procesar depósito para la cuenta: " + accountId, e);
        }
    }

    public Transaction withdraw(String accountId, BigDecimal amount) {
        Account account = loadAccount(accountId);
        BigDecimal normalizedAmount = normalizeAmount(amount);
        accountValidationService.validateWithdraw(account, normalizedAmount);

        UUID transactionId = UUID.randomUUID();
        Transaction transaction = buildTransaction(
                transactionId,
                accountId,
                "DEBIT",
                normalizedAmount,
                normalizeAmount(account.getBalance()),
                null,
                "PENDING");

        try (AutoCloseable ignored = lockManager.acquireExclusiveLock()) {
            List<Account> accounts = loadAllAccounts();
            Account lockedAccount = findAccount(accounts, accountId);

            // Revalidación mínima bajo lock para evitar inconsistencias por actualizaciones concurrentes.
            accountValidationService.validateWithdraw(lockedAccount, normalizedAmount);

            BigDecimal balanceBefore = normalizeAmount(lockedAccount.getBalance());
            BigDecimal balanceAfter =
                    accountValidationService.calculateNewBalance(lockedAccount, "WITHDRAW", normalizedAmount);

            appendJournalEntry(
                    transactionId.toString(),
                    accountId,
                    "DEBIT",
                    normalizedAmount,
                    balanceBefore,
                    balanceAfter);

            lockedAccount.setBalance(balanceAfter);
            atomicFileWriter.writeAtomic(fileAccountService.getAccountsPath(), accounts);

            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(balanceAfter);
            transaction.setStatus("SUCCESS");
            transaction.setTimestamp(LocalDateTime.now());
            return transaction;
        } catch (InvalidAmountException | InsufficientFundsException e) {
            transaction.setStatus("FAILED");
            throw e;
        } catch (StorageException e) {
            transaction.setStatus("FAILED");
            throw e;
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            throw new StorageException("Error al procesar retiro para la cuenta: " + accountId, e);
        }
    }

    public BigDecimal debit(String sagaId, String accountId, BigDecimal amount) {
        BigDecimal normalizedAmount = validateSagaAmount(amount, "El monto del débito debe ser mayor a cero");

        try (AutoCloseable ignored = lockManager.acquireExclusiveLock()) {
            List<Account> accounts = loadAllAccounts();
            Account lockedAccount = findAccount(accounts, accountId);

            accountValidationService.validateWithdraw(lockedAccount, normalizedAmount);

            BigDecimal balanceBefore = normalizeAmount(lockedAccount.getBalance());
            BigDecimal balanceAfter = balanceBefore.subtract(normalizedAmount).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

            appendJournalEntry(
                    buildSagaTransactionId(sagaId, "DEBIT"),
                    accountId,
                    "DEBIT",
                    normalizedAmount,
                    balanceBefore,
                    balanceAfter);

            lockedAccount.setBalance(balanceAfter);
            atomicFileWriter.writeAtomic(fileAccountService.getAccountsPath(), accounts);
            return balanceAfter;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Error al procesar débito de saga para la cuenta: " + accountId, e);
        }
    }

    public BigDecimal credit(String sagaId, String accountId, BigDecimal amount) {
        BigDecimal normalizedAmount = validateSagaAmount(amount, "El monto del crédito debe ser mayor a cero");

        try (AutoCloseable ignored = lockManager.acquireExclusiveLock()) {
            List<Account> accounts = loadAllAccounts();
            Account lockedAccount = findAccount(accounts, accountId);

            BigDecimal balanceBefore = normalizeAmount(lockedAccount.getBalance());
            BigDecimal balanceAfter = balanceBefore.add(normalizedAmount).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

            appendJournalEntry(
                    buildSagaTransactionId(sagaId, "CREDIT"),
                    accountId,
                    "CREDIT",
                    normalizedAmount,
                    balanceBefore,
                    balanceAfter);

            lockedAccount.setBalance(balanceAfter);
            atomicFileWriter.writeAtomic(fileAccountService.getAccountsPath(), accounts);
            return balanceAfter;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Error al procesar crédito de saga para la cuenta: " + accountId, e);
        }
    }

    private void appendJournalEntry(
            String transactionId,
            String accountId,
            String operationType,
            BigDecimal amount,
            BigDecimal previousBalance,
            BigDecimal newBalance) {
        TransactionLogEntry entry = TransactionLogEntry.builder()
                .timestamp(LocalDateTime.now())
                .transactionId(transactionId)
                .accountId(accountId)
                .operationType(operationType)
                .amount(amount)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .build();

        journalingService.appendEntry(transactionsLogPath, entry);
    }

    private Transaction buildTransaction(
            UUID transactionId,
            String accountId,
            String type,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String status) {
        return new Transaction(
                transactionId,
                accountId,
                type,
                amount,
                balanceBefore,
                balanceAfter,
                LocalDateTime.now(),
                status);
    }

    private String buildSagaTransactionId(String sagaId, String operationType) {
        if (sagaId == null || sagaId.isBlank()) {
            throw new IllegalArgumentException("El sagaId no puede ser nulo o vacío");
        }
        return sagaId + "-" + operationType + "-" + UUID.randomUUID();
    }

    private Account loadAccount(String accountId) {
        try {
            return fileAccountService.findAccountById(accountId);
        } catch (IOException e) {
            throw new StorageException("Error al leer la cuenta: " + accountId, e);
        }
    }

    private List<Account> loadAllAccounts() {
        try {
            return fileAccountService.findAllAccounts();
        } catch (IOException e) {
            throw new StorageException("Error al leer el archivo de cuentas", e);
        }
    }

    private Account findAccount(List<Account> accounts, String accountId) {
        return accounts.stream()
                .filter(account -> accountId.equals(account.getAccountId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + accountId));
    }

    private BigDecimal validateSagaAmount(BigDecimal amount, String message) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(message);
        }
        return normalizedAmount;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}
