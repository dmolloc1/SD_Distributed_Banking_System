package pe.unsa.sd.bankb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.unsa.sd.bankb.model.Account;

@Service
public class FileAccountService {

    private final ObjectMapper objectMapper;
    private final Path accountsPath;

    public FileAccountService(ObjectMapper objectMapper, @Value("${bank.data.accounts-file}") String accountsFile) {
        this.objectMapper = objectMapper;
        this.accountsPath = Path.of(accountsFile);
    }

    public List<Account> findAccountsByClientId(String clientId) throws IOException {
        List<Account> accounts = readAllAccounts();
        return accounts.stream()
                .filter(account -> clientId.equals(account.getClientId()))
                .toList();
    }

    public Account debit(String accountId, double amount) throws IOException {
        validateAmount(amount);
        List<Account> accounts = readAllAccounts();
        Account account = findByAccountId(accounts, accountId)
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));

        if (account.getBalance() < amount) {
            throw new IllegalStateException("INSUFFICIENT_FUNDS");
        }

        account.setBalance(account.getBalance() - amount);
        writeAllAccounts(accounts);
        return account;
    }

    public Account credit(String accountId, double amount) throws IOException {
        validateAmount(amount);
        List<Account> accounts = readAllAccounts();
        Account account = findByAccountId(accounts, accountId)
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));

        account.setBalance(account.getBalance() + amount);
        writeAllAccounts(accounts);
        return account;
    }

    private List<Account> readAllAccounts() throws IOException {
        return objectMapper.readValue(accountsPath.toFile(), new TypeReference<List<Account>>() {
        });
    }

    private void writeAllAccounts(List<Account> accounts) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(accountsPath.toFile(), accounts);
    }

    private Optional<Account> findByAccountId(List<Account> accounts, String accountId) {
        return accounts.stream()
                .filter(account -> accountId.equals(account.getAccountId()))
                .findFirst();
    }

    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
    }
}
