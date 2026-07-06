package pe.unsa.sd.persistence.journal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.unsa.sd.persistence.exception.StorageException;

@Slf4j
@Service
public class FileJournalingService implements JournalingService {

  @Override
  public void appendEntry(Path logPath, TransactionLogEntry entry) {
    try {
      if (logPath.getParent() != null) {
        Files.createDirectories(logPath.getParent());
      }

      // Blockchain chaining: leer el hash de la ultima entrada del ledger
      String previousHash = getLastChecksum(logPath);
      entry.setPreviousHash(previousHash);

      // Calcular checksum SHA-256 incluyendo el previousHash
      String checksum = calculateChecksum(entry);
      entry.setChecksum(checksum);

      try (BufferedWriter writer =
          Files.newBufferedWriter(
              logPath,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.APPEND)) {

        writer.write(entry.toLogLine());
        writer.newLine();
        log.debug("Entrada de auditoria registrada: TX={} hash={}", entry.getTransactionId(), checksum);
      }
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new StorageException("Error al escribir en el log de transacciones", e);
    }
  }

  // Lee la ultima linea del ledger y extrae su checksum para encadenamiento blockchain
  private String getLastChecksum(Path logPath) throws IOException {
    if (!Files.exists(logPath) || Files.size(logPath) == 0) {
      return null; // Genesis block - no previous hash
    }
    try (var lines = Files.lines(logPath)) {
      String lastLine = lines.reduce((first, second) -> second).orElse(null);
      if (lastLine == null || lastLine.isBlank()) return null;
      String[] parts = lastLine.split(" \\| ");
      return parts.length >= 8 ? parts[7] : null; // previousHash/checksum esta en la columna 8
    }
  }

  @Override
  public List<TransactionLogEntry> readHistory(Path logPath) {
    if (!Files.exists(logPath)) return List.of();

    try {
      return Files.lines(logPath).map(this::parseLogLine).collect(Collectors.toList());
    } catch (IOException e) {
      throw new StorageException("Error al leer el historial de transacciones", e);
    }
  }

  // SHA-256 sobre: txId + accountId + operationType + amount + prevBalance + newBalance + previousHash
  private String calculateChecksum(TransactionLogEntry e) throws NoSuchAlgorithmException {
    String rawData = String.format("%s%s%s%s%s%s%s",
        e.getTransactionId(),
        e.getAccountId(),
        e.getOperationType(),
        e.getAmount(),
        e.getPreviousBalance(),
        e.getNewBalance(),
        e.getPreviousHash() == null ? "" : e.getPreviousHash());

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

    return HexFormat.of().formatHex(hash);
  }

  private TransactionLogEntry parseLogLine(String line) {
    String[] parts = line.split(" \\| ");
    String previousHash = "GENESIS".equals(parts[7]) ? null : parts[7];
    return TransactionLogEntry.builder()
        .timestamp(java.time.LocalDateTime.parse(parts[0]))
        .transactionId(parts[1])
        .accountId(parts[2])
        .operationType(parts[3])
        .amount(new java.math.BigDecimal(parts[4]))
        .previousBalance(new java.math.BigDecimal(parts[5]))
        .newBalance(new java.math.BigDecimal(parts[6]))
        .previousHash(previousHash)
        .checksum(parts[8])
        .build();
  }
}
