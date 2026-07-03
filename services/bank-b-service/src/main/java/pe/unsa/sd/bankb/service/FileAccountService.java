package pe.unsa.sd.bankb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.unsa.sd.bankb.model.Account;

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

    public List<Account> findAccountsByClientId(String clientId) throws IOException {
        return findAllAccounts().stream()
                .filter(account -> clientId.equals(account.getClientId()))
                .toList();
    }

    public Path getAccountsPath() {
        return accountsPath;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}
