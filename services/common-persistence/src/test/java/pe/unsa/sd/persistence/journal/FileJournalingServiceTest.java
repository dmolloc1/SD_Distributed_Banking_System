package pe.unsa.sd.persistence.journal;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileJournalingServiceTest {

  private FileJournalingService journalingService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    journalingService = new FileJournalingService();
  }

  @Test
  @DisplayName("Debe registrar y recuperar transacciones manteniendo la integridad de datos")
  void testAppendAndReadHistory() {
    Path logPath = tempDir.resolve("journal/transactions.log");
    TransactionLogEntry entry = createEntry("TX-100", "DEBIT", 100.0, 1000.0, 900.0);

    journalingService.appendEntry(logPath, entry);
    List<TransactionLogEntry> history = journalingService.readHistory(logPath);

    assertEquals(1, history.size());
    TransactionLogEntry persisted = history.get(0);

    assertEquals("TX-100", persisted.getTransactionId());
    assertEquals("DEBIT", persisted.getOperationType());
    assertEquals(0, new BigDecimal("100.0").compareTo(persisted.getAmount()));
    assertNotNull(persisted.getChecksum(), "El checksum debe haber sido generado");
  }

  @Test
  @DisplayName("Debe añadir registros al final del archivo sin borrar previos (Append-Only)")
  void testAppendOnlyBehavior() throws Exception {
    Path logPath = tempDir.resolve("bank.log");

    journalingService.appendEntry(logPath, createEntry("TX-1", "CREDIT", 50.0, 0.0, 50.0));
    journalingService.appendEntry(logPath, createEntry("TX-2", "DEBIT", 20.0, 50.0, 30.0));

    List<String> lines = Files.readAllLines(logPath);

    assertEquals(2, lines.size(), "Debe haber exactamente dos líneas en el archivo");
    assertTrue(lines.get(0).contains("TX-1"));
    assertTrue(lines.get(1).contains("TX-2"));
  }

  @Test
  @DisplayName("Debe generar un checksum SHA-256 consistente")
  void testChecksumConsistency() throws Exception {
    Path logPath = tempDir.resolve("checksum.log");
    TransactionLogEntry entry = createEntry("TX-SEC", "DEBIT", 10.0, 100.0, 90.0);

    journalingService.appendEntry(logPath, entry);

    String logLine = Files.readAllLines(logPath).get(0);
    String[] parts = logLine.split(" \\| ");
    String checksum = parts[parts.length - 1];

    assertEquals(
        64, checksum.length(), "El checksum SHA-256 debe tener 64 caracteres hexadecimales");
  }

  @Test
  @DisplayName("Debe retornar lista vacía si el archivo no existe")
  void testReadNonExistentFile() {
    Path fakePath = tempDir.resolve("non-existent.log");
    List<TransactionLogEntry> history = journalingService.readHistory(fakePath);

    assertNotNull(history);
    assertTrue(history.isEmpty());
  }

  private TransactionLogEntry createEntry(
      String txId, String type, double amount, double prev, double next) {
    return TransactionLogEntry.builder()
        .timestamp(LocalDateTime.now())
        .transactionId(txId)
        .accountId("ACC-TEST")
        .operationType(type)
        .amount(BigDecimal.valueOf(amount))
        .previousBalance(BigDecimal.valueOf(prev))
        .newBalance(BigDecimal.valueOf(next))
        .build();
  }
}
