package pe.unsa.sd.persistence.locking;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.unsa.sd.persistence.exception.LockAcquisitionException;
import pe.unsa.sd.persistence.exception.StorageException;

@Slf4j
@Component
public class NioFileLockManager implements LockManager {

  private final Path lockFilePath;
  private final int timeoutMs;

  // Lock interno para sincronizar hilos de la misma JVM y evitar OverlappingFileLockException
  private final ReentrantLock internalLock = new ReentrantLock();

  public NioFileLockManager(
      @Value("${bank.data.accounts-lock-file:data/.accounts.json.lock}") String lockFile,
      @Value("${bank.lock.timeout-ms:2000}") int timeoutMs) {
    this.lockFilePath = Path.of(lockFile);
    this.timeoutMs = timeoutMs;
  }

  @Override
  public AutoCloseable acquireExclusiveLock() throws LockAcquisitionException {
    try {
      // Intentar bloqueo interno (JVM-level)
      if (!internalLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
        throw new LockAcquisitionException(lockFilePath.toString() + " (Internal Timeout)");
      }

      // Intentar bloqueo de archivo (OS-level)
      return acquireFileLock();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StorageException("Interrupción mientras se esperaba el bloqueo", e);
    } catch (Exception e) {
      // Si algo falla después de obtener el internalLock, debemos liberarlo
      if (internalLock.isHeldByCurrentThread()) {
        internalLock.unlock();
      }
      throw e;
    }
  }

  private AutoCloseable acquireFileLock() throws LockAcquisitionException {
    try {
      RandomAccessFile raf = new RandomAccessFile(lockFilePath.toFile(), "rw");
      FileChannel channel = raf.getChannel();

      // Usamos lock() síncrono ya que internalLock ya serializó el acceso en esta JVM
      FileLock fileLock = channel.lock(0, Long.MAX_VALUE, false);

      if (fileLock != null) {
        log.debug("Bloqueo de archivo adquirido: {}", lockFilePath.getFileName());
        // Retornamos un recurso que libera AMBOS bloqueos (NIO e Interno)
        return () -> release(fileLock, channel, raf);
      }
    } catch (IOException e) {
      log.error("Error al acceder al archivo de bloqueo: {}", e.getMessage());
    }

    throw new LockAcquisitionException(lockFilePath.toString());
  }

  private void release(FileLock fileLock, FileChannel channel, RandomAccessFile raf) {
    try {
      if (fileLock != null && fileLock.isValid()) {
        fileLock.release();
      }
    } catch (IOException e) {
      log.error("Error liberando FileLock", e);
    } finally {
      // Cerrar recursos de archivos
      closeQuietly(channel);
      closeQuietly(raf);
      // Liberar el lock interno SIEMPRE al final
      if (internalLock.isHeldByCurrentThread()) {
        internalLock.unlock();
        log.debug("Bloqueo interno y de archivo liberados.");
      }
    }
  }

  private void closeQuietly(AutoCloseable resource) {
    try {
      if (resource != null) resource.close();
    } catch (Exception e) {
      // Ignorar
    }
  }

  @Override
  public void close() {
    // No se requiere estado global que cerrar
  }
}
