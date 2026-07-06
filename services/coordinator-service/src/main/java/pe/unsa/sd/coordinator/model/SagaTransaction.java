package pe.unsa.sd.coordinator.model;
//Representa toda la transacción: origen, destino, monto, estado, lista de pasos.
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class SagaTransaction {

    private String transactionId;
    private String sourceAccountId;
    private BankId sourceBankId;
    private String destinationAccountId;
    private BankId destinationBankId;
    private BigDecimal amount;
    private SagaStatus status;
    private OffsetDateTime initiatedAt;
    private OffsetDateTime completedAt;
    private List<SagaStep> steps = new ArrayList<>();
    private String error;

    public SagaTransaction() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(String sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public BankId getSourceBankId() {
        return sourceBankId;
    }

    public void setSourceBankId(BankId sourceBankId) {
        this.sourceBankId = sourceBankId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BankId getDestinationBankId() {
        return destinationBankId;
    }

    public void setDestinationBankId(BankId destinationBankId) {
        this.destinationBankId = destinationBankId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    public OffsetDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(OffsetDateTime initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<SagaStep> getSteps() {
        return steps;
    }

    public void setSteps(List<SagaStep> steps) {
        this.steps = steps;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
