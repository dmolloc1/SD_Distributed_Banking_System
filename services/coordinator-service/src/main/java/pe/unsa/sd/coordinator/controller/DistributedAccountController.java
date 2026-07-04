package pe.unsa.sd.coordinator.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.coordinator.dto.AccountDto;
import pe.unsa.sd.coordinator.service.DistributedAccountService;

@RestController
public class DistributedAccountController {

    private final DistributedAccountService distributedAccountService;

    public DistributedAccountController(DistributedAccountService distributedAccountService) {
        this.distributedAccountService = distributedAccountService;
    }

    @GetMapping("/distributed/accounts/{clientId}")
    public List<AccountDto> getDistributedAccounts(@PathVariable String clientId) {
        return distributedAccountService.findAccountsByClientId(clientId);
    }
}
