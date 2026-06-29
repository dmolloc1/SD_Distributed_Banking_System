package pe.unsa.sd.coordinator.dto;

import java.math.BigDecimal;
//  Cada paso tiene su operacion normal y, en caso de fallo, una operación compensatoria que revierte el efecto.
// Cuando una transferencia no se completa y el orquestador debe revertir la operación en el banco origen para recibir lo que se desconto
//  (Rollback lógico): Se ejecuta en orden inverso de los pasos exitosos.
public class CompensationRequest {

    private String transactionId;
    private String originalOperation;
    private String bankId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String reason;

    public CompensationRequest() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOriginalOperation() {
        return originalOperation;
    }

    public void setOriginalOperation(String originalOperation) {
        this.originalOperation = originalOperation;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
