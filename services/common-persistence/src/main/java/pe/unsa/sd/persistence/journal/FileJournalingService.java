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
      // Asegurar directorios
      if (logPath.getParent() != null) {
        Files.createDirectories(logPath.getParent());
      }

      // Calcular Checksum antes de escribir
      String checksum = calculateChecksum(entry);
      entry.setChecksum(checksum);

      // Escritura Append-Only
      try (BufferedWriter writer =
          Files.newBufferedWriter(
              logPath,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.APPEND)) {

        writer.write(entry.toLogLine());
        writer.newLine();
        log.debug("Entrada de auditoría registrada: TX={}", entry.getTransactionId());
      }
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new StorageException("Error al escribir en el log de transacciones", e);
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

  private String calculateChecksum(TransactionLogEntry e) throws NoSuchAlgorithmException {
    String rawData =
        String.format(
            "%s%s%s%s%s%s",
            e.getTransactionId(),
            e.getAccountId(),
            e.getOperationType(),
            e.getAmount(),
            e.getPreviousBalance(),
            e.getNewBalance());

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

    return HexFormat.of().formatHex(hash);
  }

  private TransactionLogEntry parseLogLine(String line) {
    String[] parts = line.split(" \\| ");
    return TransactionLogEntry.builder()
        .timestamp(java.time.LocalDateTime.parse(parts[0]))
        .transactionId(parts[1])
        .accountId(parts[2])
        .operationType(parts[3])
        .amount(new java.math.BigDecimal(parts[4]))
        .previousBalance(new java.math.BigDecimal(parts[5]))
        .newBalance(new java.math.BigDecimal(parts[6]))
        .checksum(parts[7])
        .build();
  }
}
