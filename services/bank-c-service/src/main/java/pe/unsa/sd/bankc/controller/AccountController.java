package pe.unsa.sd.bankc.controller;

import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.bankc.model.Account;
import pe.unsa.sd.bankc.service.FileAccountService;

@RestController
public class AccountController {

    private final FileAccountService fileAccountService;

    public AccountController(FileAccountService fileAccountService) {
        this.fileAccountService = fileAccountService;
    }

    @GetMapping("/clients/{clientId}/accounts")
    public List<Account> getAccountsByClientId(@PathVariable String clientId) throws IOException {
        return fileAccountService.findAccountsByClientId(clientId);
    }
}
