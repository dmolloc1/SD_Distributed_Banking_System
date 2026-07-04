package pe.unsa.sd.persistence.domain.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String accountId;
    private String clientId;
    private String bankCode;
    private String type;
    private String currency;
    private BigDecimal balance;
    private String status;
}
