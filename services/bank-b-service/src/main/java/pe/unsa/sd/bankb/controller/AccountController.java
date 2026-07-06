package pe.unsa.sd.bankb.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.bankb.exception.InvalidAmountException;
import pe.unsa.sd.bankb.model.Account;
import pe.unsa.sd.bankb.service.AccountOperationService;
import pe.unsa.sd.bankb.service.FileAccountService;

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
    public Map<String, Object> debit(@PathVariable String accountId, @RequestBody BankOperationRequest request) throws IOException {
        String transactionId = resolveTransactionId(request);
        return applyOperation(
                () -> accountOperationService.debit(transactionId, accountId, request.getAmount()),
                accountId,
                transactionId,
                request);
    }

    @PostMapping("/api/v1/bank/accounts/{accountId}/credit")
    public Map<String, Object> credit(@PathVariable String accountId, @RequestBody BankOperationRequest request) throws IOException {
        String transactionId = resolveTransactionId(request);
        return applyOperation(
                () -> accountOperationService.credit(transactionId, accountId, request.getAmount()),
                accountId,
                transactionId,
                request);
    }

    private String resolveTransactionId(BankOperationRequest request) {
        if (request == null || request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            return UUID.randomUUID().toString();
        }
        return request.getTransactionId();
    }

    private Map<String, Object> applyOperation(
            AccountOperation operation,
            String accountId,
            String transactionId,
            BankOperationRequest request) throws IOException {
        if (request == null || request.getAmount() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AMOUNT");
        }

        try {
            BigDecimal balance = operation.execute();
            return Map.of(
                    "status", "SUCCESS",
                    "accountId", accountId,
                    "balance", balance,
                    "transactionId", transactionId);
        } catch (InvalidAmountException exception) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
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
        BigDecimal execute() throws IOException;
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
