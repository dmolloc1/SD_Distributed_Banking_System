package pe.unsa.sd.banka.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.banka.model.Account;
import pe.unsa.sd.banka.service.FileAccountService;

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


    @FunctionalInterface
    private interface AccountOperation {
        Account execute() throws IOException;
    }

    public static class BankOperationRequest {

        private String transactionId;
        private BigDecimal amount;
        private String currency;

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}
