package pe.unsa.sd.persistence.locking;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.unsa.sd.persistence.exception.LockAcquisitionException;

class NioFileLockManagerTest {

  private NioFileLockManager lockManager;

  @TempDir Path tempDir; // Directorio temporal proveído por JUnit 5

  private String lockFilePath;

  @BeforeEach
  void setUp() {
    lockFilePath = tempDir.resolve(".accounts.json.lock").toString();
    lockManager = new NioFileLockManager(lockFilePath);
  }

  @Test
  @DisplayName("Debe permitir que hilos secuenciales adquieran y liberen el bloqueo")
  void testSequentialLocking() throws Exception {
    try (AutoCloseable lock1 = lockManager.acquireExclusiveLock()) {
      assertNotNull(lock1, "El primer bloqueo debería ser exitoso");
    }

    try (AutoCloseable lock2 = lockManager.acquireExclusiveLock()) {
      assertNotNull(lock2, "El segundo bloqueo secuencial debería ser exitoso");
    }
  }

  @Test
  @DisplayName("Debe garantizar exclusión mutua ante múltiples hilos concurrentes")
  void testConcurrentLocking() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch endGate = new CountDownLatch(threadCount);

    AtomicInteger successfulLocks = new AtomicInteger(0);
    AtomicInteger failedLocks = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              startGate.await(); // Espera a que todos estén listos
              try (AutoCloseable lock = lockManager.acquireExclusiveLock()) {
                successfulLocks.incrementAndGet();
                // Simulamos una pequeña operación de escritura
                Thread.sleep(50);
              }
            } catch (LockAcquisitionException e) {
              failedLocks.incrementAndGet();
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              endGate.countDown();
            }
          });
    }

    long startTime = System.currentTimeMillis();
    startGate.countDown(); // Libera todos los hilos al mismo tiempo
    endGate.await(10, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();

    executor.shutdown();

    // En 2 segundos de timeout acumulado y con 10 hilos de 50ms,
    // todos deberían haber logrado entrar eventualmente.
    assertEquals(
        threadCount,
        successfulLocks.get(),
        "Todos los hilos deberían haber adquirido el lock eventualmente dentro del timeout");
    assertEquals(0, failedLocks.get(), "No debería haber fallos por timeout en estas condiciones");

    assertTrue(
        (endTime - startTime) >= (threadCount * 50),
        "El tiempo total debe demostrar ejecución secuencial (aprox 500ms)");
  }

  @Test
  @DisplayName(
      "Debe fallar con LockAcquisitionException si un hilo retiene el bloqueo más del timeout")
  void testLockTimeout() throws Exception {
    CountDownLatch lockAcquiredSignal = new CountDownLatch(1);
    CountDownLatch testFinishedSignal = new CountDownLatch(1);

    // Hilo A: Adquiere el lock y lo retiene por mucho tiempo
    Thread threadA =
        new Thread(
            () -> {
              try (AutoCloseable lock = lockManager.acquireExclusiveLock()) {
                lockAcquiredSignal.countDown();
                Thread.sleep(3000); // Excede los 2000ms de timeout del manager
              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                testFinishedSignal.countDown();
              }
            });

    threadA.start();
    lockAcquiredSignal.await(); // Esperamos a que Hilo A tenga el lock

    // Hilo B: Intenta adquirirlo y debería fallar
    assertThrows(
        LockAcquisitionException.class,
        () -> {
          lockManager.acquireExclusiveLock();
        },
        "Debería lanzar excepción debido a que Hilo A no libera el archivo a tiempo");

    testFinishedSignal.await();
  }
}
