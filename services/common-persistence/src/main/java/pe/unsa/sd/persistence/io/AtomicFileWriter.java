package pe.unsa.sd.persistence.io;

import java.nio.file.Path;
import pe.unsa.sd.persistence.exception.AtomicWriteException;

/**
 * Define el contrato para operaciones de escritura que deben ser atómicas a nivel de File System.
 */
public interface AtomicFileWriter {
  /**
   * Escribe un objeto en la ruta especificada de forma atómica.
   *
   * @param targetPath Ruta del archivo final.
   * @param data Objeto a serializar y persistir.
   */
  void writeAtomic(Path targetPath, Object data) throws AtomicWriteException;
}
