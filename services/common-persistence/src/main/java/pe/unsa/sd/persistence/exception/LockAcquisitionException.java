package pe.unsa.sd.persistence.exception;

/** Lanzada cuando no se puede adquirir un bloqueo sobre un recurso tras agotar el timeout. */
public class LockAcquisitionException extends StorageException {
  public LockAcquisitionException(String resource) {
    super(
        "No se pudo adquirir el bloqueo sobre el recurso: " + resource + " tras varios intentos.");
  }
}
