package pe.unsa.sd.coordinator.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
/* 
Define los datos de entrada: cuentas, bancos, monto, moneda.

Es el contrato de la API.
*/
public class TransferRequest {

    @NotBlank
    @Size(max = 40)
    private String originAccountId;

    @NotBlank
    @Pattern(regexp = "BANK_A|BANK_B|BANK_C")
    private String originBankId;

    @NotBlank
    @Size(max = 40)
    private String destinationAccountId;

    @NotBlank
    @Pattern(regexp = "BANK_A|BANK_B|BANK_C")
    private String destinationBankId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 10)
    private String currency;

    public TransferRequest() {
    }

    public String getOriginAccountId() {
        return originAccountId;
    }

    public void setOriginAccountId(String originAccountId) {
        this.originAccountId = originAccountId;
    }

    public String getOriginBankId() {
        return originBankId;
    }

    public void setOriginBankId(String originBankId) {
        this.originBankId = originBankId;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
