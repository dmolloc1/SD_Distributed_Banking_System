package pe.unsa.sd.persistence.journal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/** Representa una entrada inmutable en el registro de auditoría (Ledger). */
@Data
@Builder
public class TransactionLogEntry {
  private LocalDateTime timestamp;
  private String transactionId;
  private String accountId;
  private String operationType; // DEBIT, CREDIT, REVERSAL
  private BigDecimal amount;
  private BigDecimal previousBalance;
  private BigDecimal newBalance;
  private String checksum;

  /** Genera una cadena formateada para persistencia en texto plano. */
  public String toLogLine() {
    return String.format(
        "%s | %s | %s | %s | %s | %s | %s | %s",
        timestamp,
        transactionId,
        accountId,
        operationType,
        amount,
        previousBalance,
        newBalance,
        checksum);
  }
}
