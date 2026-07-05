package pe.unsa.sd.coordinator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class SingleOperationRequest {

    @NotBlank
    @Size(max = 40)
    private String accountId;

    @NotBlank
    @Pattern(regexp = "BANK_A|BANK_B|BANK_C")
    private String bankId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 10)
    private String currency;

    public SingleOperationRequest() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
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
