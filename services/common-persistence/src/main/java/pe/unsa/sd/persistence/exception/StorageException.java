package pe.unsa.sd.persistence.exception;

/** Excepción base para errores en la capa de persistencia distribuida. */
public class StorageException extends RuntimeException {
  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
