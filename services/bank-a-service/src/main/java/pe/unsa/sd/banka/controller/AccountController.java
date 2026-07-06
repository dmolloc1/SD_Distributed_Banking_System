package pe.unsa.sd.banka.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.banka.exception.InsufficientFundsException;
import pe.unsa.sd.banka.exception.InvalidAmountException;
import pe.unsa.sd.banka.model.Account;
import pe.unsa.sd.banka.service.AccountOperationService;
import pe.unsa.sd.banka.service.FileAccountService;

@RestController
public class AccountController {

    private final FileAccountService fileAccountService;
    private final AccountOperationService accountOperationService;

    public AccountController(FileAccountService fileAccountService, AccountOperationService accountOperationService) {
        this.fileAccountService = fileAccountService;
        this.accountOperationService = accountOperationService;
    }

    @GetMapping("/clients/{clientId}/accounts")
    public List<Account> getAccountsByClientId(@PathVariable String clientId) throws IOException {
        return fileAccountService.findAccountsByClientId(clientId);
    }

    @PostMapping("/api/v1/bank/accounts/{accountId}/debit")
    public ResponseEntity<?> debit(@PathVariable String accountId, @RequestBody BankOperationRequest request) {
        try {
            accountOperationService.debit(request.getTransactionId(), accountId, request.getAmount());
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (InvalidAmountException | InsufficientFundsException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "INTERNAL_ERROR"));
        }
    }

    @PostMapping("/api/v1/bank/accounts/{accountId}/credit")
    public ResponseEntity<?> credit(@PathVariable String accountId, @RequestBody BankOperationRequest request) {
        try {
            accountOperationService.credit(request.getTransactionId(), accountId, request.getAmount());
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } catch (InvalidAmountException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "INTERNAL_ERROR"));
        }
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
