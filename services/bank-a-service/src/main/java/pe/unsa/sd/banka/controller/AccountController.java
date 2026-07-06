package pe.unsa.sd.banka.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/api/v1/bank/accounts/{accountId}/debit")
    public Map<String, Object> debit(@PathVariable String accountId, @RequestBody BankOperationRequest request) throws IOException {
        return applyOperation(() -> fileAccountService.debit(accountId, request.getAmount().doubleValue()), request);
    }

    @PostMapping("/api/v1/bank/accounts/{accountId}/credit")
    public Map<String, Object> credit(@PathVariable String accountId, @RequestBody BankOperationRequest request) throws IOException {
        return applyOperation(() -> fileAccountService.credit(accountId, request.getAmount().doubleValue()), request);
    }

    private Map<String, Object> applyOperation(AccountOperation operation, BankOperationRequest request) throws IOException {
        if (request == null || request.getAmount() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AMOUNT");
        }

        try {
            Account account = operation.execute();
            return Map.of(
                    "status", "SUCCESS",
                    "accountId", account.getAccountId(),
                    "balance", account.getBalance(),
                    "transactionId", request.getTransactionId() == null ? "" : request.getTransactionId());
        } catch (IllegalArgumentException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
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
