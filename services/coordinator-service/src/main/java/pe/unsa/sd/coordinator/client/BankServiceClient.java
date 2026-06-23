package pe.unsa.sd.coordinator.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pe.unsa.sd.coordinator.dto.AccountDto;

@Component
public class BankServiceClient {

    private final RestTemplate restTemplate;

    public BankServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<AccountDto> getAccounts(String bankUrl, String clientId) {
        String url = bankUrl + "/clients/" + clientId + "/accounts";
        try {
            AccountDto[] accounts = restTemplate.getForObject(url, AccountDto[].class);
            if (accounts == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(accounts);
        } catch (RestClientException exception) {
            System.out.println("Bank service unavailable: " + bankUrl + " - " + exception.getMessage());
            return Collections.emptyList();
        }
    }
}
