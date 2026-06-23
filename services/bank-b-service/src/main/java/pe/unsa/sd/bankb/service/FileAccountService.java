package pe.unsa.sd.bankb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
        List<Account> accounts = objectMapper.readValue(accountsPath.toFile(), new TypeReference<List<Account>>() {});
        return accounts.stream()
                .filter(account -> clientId.equals(account.getClientId()))
                .toList();
    }
}
