package pe.unsa.sd.coordinator.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
// Representa toda la transacción: origen, destino, monto, estado, lista de pasos. Estados: PENDING, COMMITTED, ABORTED, COMPENSATING.
public class SagaTransactionDTO {

    private String transactionId;
    private String sourceAccountId;
    private String sourceBankId;
    private String destinationAccountId;
    private String destinationBankId;
    private BigDecimal amount;
    private String status;
    private String message;        // Human-readable status for frontend
    private int currentStep;       // Current step being executed (0 = not started)
    private int totalSteps;        // Total steps in the saga (typically 2)
    private OffsetDateTime initiatedAt;
    private OffsetDateTime completedAt;
    private String error;
    private List<SagaStepDTO> steps = new ArrayList<>();

    public SagaTransactionDTO() {
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

    public String getSourceBankId() {
        return sourceBankId;
    }

    public void setSourceBankId(String sourceBankId) {
        this.sourceBankId = sourceBankId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public String getDestinationBankId() {
        return destinationBankId;
    }

    public void setDestinationBankId(String destinationBankId) {
        this.destinationBankId = destinationBankId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<SagaStepDTO> getSteps() {
        return steps;
    }

    public void setSteps(List<SagaStepDTO> steps) {
        this.steps = steps;
    }

    public static class SagaStepDTO {

        private String stepId;
        private int stepNumber;
        private String bankId;
        private String accountId;
        private String operationType;
        private BigDecimal amount;
        private String stepStatus;
        private OffsetDateTime executedAt;
        private String errorMessage;
        private boolean compensationApplied;
        private OffsetDateTime compensationTimestamp;

        public SagaStepDTO() {
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

        public String getBankId() {
            return bankId;
        }

        public void setBankId(String bankId) {
            this.bankId = bankId;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getOperationType() {
            return operationType;
        }

        public void setOperationType(String operationType) {
            this.operationType = operationType;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getStepStatus() {
            return stepStatus;
        }

        public void setStepStatus(String stepStatus) {
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
}
