package pe.unsa.sd.persistence.locking;

import java.io.Closeable;
import pe.unsa.sd.persistence.exception.LockAcquisitionException;

/**
 * Define el contrato para la gestión de bloqueos de archivos en el sistema distribuido. Implementa
 * Closeable para asegurar la liberación en bloques try-with-resources.
 */
public interface LockManager extends Closeable {

  /**
   * Intenta adquirir un bloqueo exclusivo.
   *
   * @return Una instancia de Closeable que libera el lock al cerrarse.
   */
  AutoCloseable acquireExclusiveLock() throws LockAcquisitionException;

  @Override
  void close();
}
