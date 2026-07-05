package pe.unsa.sd.persistence.journal;

import java.nio.file.Path;
import java.util.List;

public interface JournalingService {

  /** Registra una nueva operación al final del log de forma inmutable. */
  void appendEntry(Path logPath, TransactionLogEntry entry);

  /** Recupera el historial de transacciones desde el archivo de log. */
  List<TransactionLogEntry> readHistory(Path logPath);
}
