package pe.unsa.sd.banka.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.unsa.sd.banka.model.Account;

@Service
public class FileAccountService {

    private static final int MONEY_SCALE = 2;

    private final ObjectMapper objectMapper;
    private final Path accountsPath;

    public FileAccountService(ObjectMapper objectMapper, @Value("${bank.data.accounts-file}") String accountsFile) {
        this.objectMapper = objectMapper;
        this.accountsPath = Path.of(accountsFile);
    }

    public List<Account> findAllAccounts() throws IOException {
        return objectMapper.readValue(accountsPath.toFile(), new TypeReference<List<Account>>() {});
    }

    public Account findAccountById(String accountId) throws IOException {
        return findAllAccounts().stream()
                .filter(account -> accountId.equals(account.getAccountId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + accountId));
    }

    public Path getAccountsPath() {
        return accountsPath;
    }

    public List<Account> findAccountsByClientId(String clientId) throws IOException {
        return findAllAccounts().stream()
                .filter(account -> clientId.equals(account.getClientId()))
                .toList();
    }

    public Account debit(String accountId, double amount) throws IOException {
        BigDecimal normalizedAmount = normalizeAmount(BigDecimal.valueOf(amount));
        validateAmount(normalizedAmount);

        List<Account> accounts = findAllAccounts();
        Account account = findAccount(accounts, accountId);

        BigDecimal currentBalance = normalizeAmount(account.getBalance());
        if (currentBalance.compareTo(normalizedAmount) < 0) {
            throw new IllegalStateException("INSUFFICIENT_FUNDS");
        }

        account.setBalance(currentBalance.subtract(normalizedAmount).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN));
        writeAllAccounts(accounts);
        return account;
    }

    public Account credit(String accountId, double amount) throws IOException {
        BigDecimal normalizedAmount = normalizeAmount(BigDecimal.valueOf(amount));
        validateAmount(normalizedAmount);

        List<Account> accounts = findAllAccounts();
        Account account = findAccount(accounts, accountId);

        BigDecimal currentBalance = normalizeAmount(account.getBalance());
        account.setBalance(currentBalance.add(normalizedAmount).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN));
        writeAllAccounts(accounts);
        return account;
    }

    private Account findAccount(List<Account> accounts, String accountId) {
        return accounts.stream()
                .filter(account -> accountId.equals(account.getAccountId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));
    }

    private void writeAllAccounts(List<Account> accounts) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(accountsPath.toFile(), accounts);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}
