package pe.unsa.sd.coordinator.model;
//Representa cada paso individual (ej. DEBIT origen, CREDIT destino).
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class SagaStep {

    private String stepId;
    private int stepNumber;
    private BankId bankId;
    private String accountId;
    private OperationType operationType;
    private BigDecimal amount;
    private SagaStepStatus stepStatus;
    private OffsetDateTime executedAt;
    private String errorMessage;
    private boolean compensationApplied;
    private OffsetDateTime compensationTimestamp;

    public SagaStep() {
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(int stepNumber) {
        this.stepNumber = stepNumber;
    }

    public BankId getBankId() {
        return bankId;
    }

    public void setBankId(BankId bankId) {
        this.bankId = bankId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public SagaStepStatus getStepStatus() {
        return stepStatus;
    }

    public void setStepStatus(SagaStepStatus stepStatus) {
        this.stepStatus = stepStatus;
    }

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isCompensationApplied() {
        return compensationApplied;
    }

    public void setCompensationApplied(boolean compensationApplied) {
        this.compensationApplied = compensationApplied;
    }

    public OffsetDateTime getCompensationTimestamp() {
        return compensationTimestamp;
    }

    public void setCompensationTimestamp(OffsetDateTime compensationTimestamp) {
        this.compensationTimestamp = compensationTimestamp;
    }
}
