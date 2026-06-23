package pe.unsa.sd.coordinator.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.unsa.sd.coordinator.client.BankServiceClient;
import pe.unsa.sd.coordinator.dto.AccountDto;

@Service
public class DistributedAccountService {

    private final BankServiceClient bankServiceClient;
    private final String bankAUrl;
    private final String bankBUrl;
    private final String bankCUrl;

    public DistributedAccountService(
            BankServiceClient bankServiceClient,
            @Value("${banks.bank-a-url}") String bankAUrl,
            @Value("${banks.bank-b-url}") String bankBUrl,
            @Value("${banks.bank-c-url}") String bankCUrl) {
        this.bankServiceClient = bankServiceClient;
        this.bankAUrl = bankAUrl;
        this.bankBUrl = bankBUrl;
        this.bankCUrl = bankCUrl;
    }

    public List<AccountDto> findAccountsByClientId(String clientId) {
        List<AccountDto> accounts = new ArrayList<>();
        accounts.addAll(bankServiceClient.getAccounts(bankAUrl, clientId));
        accounts.addAll(bankServiceClient.getAccounts(bankBUrl, clientId));
        accounts.addAll(bankServiceClient.getAccounts(bankCUrl, clientId));
        return accounts;
    }
}
