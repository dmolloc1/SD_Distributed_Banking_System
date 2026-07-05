package pe.unsa.sd.persistence.exception;

/**
 * Lanzada cuando ocurre un fallo crítico durante el proceso de escritura en sombra (Shadow Paging).
 */
public class AtomicWriteException extends StorageException {
  public AtomicWriteException(String message, Throwable cause) {
    super(message, cause);
  }
}
