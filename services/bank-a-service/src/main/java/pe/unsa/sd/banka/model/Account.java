package pe.unsa.sd.banka.model;

import java.math.BigDecimal;

public class Account {

    private String accountId;
    private String clientId;
    private String bankCode;
    private String type;
    private String currency;
    private BigDecimal balance;

    public Account() {
    }

    public Account(String accountId, String clientId, String bankCode, String type, String currency, BigDecimal balance) {
        this.accountId = accountId;
        this.clientId = clientId;
        this.bankCode = bankCode;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
