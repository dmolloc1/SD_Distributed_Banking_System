package pe.unsa.sd.persistence.journal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

// Encadenamiento tipo blockchain: cada entrada guarda el hash SHA-256 de la anterior,
// formando una cadena inmutable que permite detectar manipulaciones.
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
  private String previousHash; // Hash SHA-256 de la entrada anterior (blockchain chaining)
  private String checksum;     // Hash SHA-256 de esta entrada (incluye previousHash)

  public String toLogLine() {
    return String.format(
        "%s | %s | %s | %s | %s | %s | %s | %s | %s",
        timestamp,
        transactionId,
        accountId,
        operationType,
        amount,
        previousBalance,
        newBalance,
        previousHash == null ? "GENESIS" : previousHash,
        checksum);
  }
}
