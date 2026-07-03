package pe.unsa.sd.bankb.model;

import java.math.BigDecimal;

public class AccountOperationRequest {

    private String accountId;
    private BigDecimal amount;
    private String operationType;

    public AccountOperationRequest() {
    }

    public AccountOperationRequest(String accountId, BigDecimal amount, String operationType) {
        this.accountId = accountId;
        this.amount = amount;
        this.operationType = operationType;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
}
