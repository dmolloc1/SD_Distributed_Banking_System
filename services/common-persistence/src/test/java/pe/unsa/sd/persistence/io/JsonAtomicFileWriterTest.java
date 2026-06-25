package pe.unsa.sd.persistence.io;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.unsa.sd.persistence.exception.AtomicWriteException;

class JsonAtomicFileWriterTest {

  private JsonAtomicFileWriter writer;
  private ObjectMapper objectMapper;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    writer = new JsonAtomicFileWriter(objectMapper);
  }

  @Test
  @DisplayName("Debe persistir datos correctamente y limpiar archivos temporales")
  void testSuccessfulAtomicWrite() throws IOException {
    Path targetFile = tempDir.resolve("accounts.json");
    TestData data = new TestData("ACC-1", 500.0);

    writer.writeAtomic(targetFile, data);

    // Verificaciones
    assertTrue(Files.exists(targetFile), "El archivo destino debe existir");
    assertFalse(
        Files.exists(tempDir.resolve("accounts.json.tmp")),
        "El archivo temporal debe haber sido eliminado");

    // Validar contenido
    TestData persisted = objectMapper.readValue(targetFile.toFile(), TestData.class);
    assertEquals(data.getAccountId(), persisted.getAccountId());
    assertEquals(data.getBalance(), persisted.getBalance());
  }

  @Test
  @DisplayName("Debe crear directorios inexistentes automáticamente")
  void testCreateDirectoriesAutomatically() {
    Path nestedPath = tempDir.resolve("deep/path/to/bank/data.json");
    TestData data = new TestData("ACC-X", 100.0);

    assertDoesNotThrow(() -> writer.writeAtomic(nestedPath, data));
    assertTrue(Files.exists(nestedPath));
  }

  @Test
  @DisplayName("Debe proteger el archivo original si ocurre un error de serialización")
  void testProtectionOnFailure() throws IOException {
    Path targetFile = tempDir.resolve("original.json");
    String originalContent = "{\"status\": \"safe\"}";
    Files.writeString(targetFile, originalContent);

    // Intentamos escribir un objeto que causará error de serialización
    // En este caso, simulamos enviando un objeto que Jackson no podrá manejar o forzando un error
    Object invalidData =
        new Object() {
          public String getError() {
            throw new RuntimeException("Serialization Fail");
          }
        };

    assertThrows(
        AtomicWriteException.class,
        () -> {
          writer.writeAtomic(targetFile, invalidData);
        });

    // El archivo original DEBE permanecer intacto
    assertEquals(
        originalContent,
        Files.readString(targetFile),
        "El archivo original no debe haber sido modificado tras un fallo de escritura");

    // El temporal debe haber sido limpiado
    assertFalse(
        Files.exists(tempDir.resolve("original.json.tmp")),
        "El archivo temporal debe limpiarse tras un fallo");
  }

  @Test
  @DisplayName("Debe sobrescribir el archivo de forma atómica si ya existe")
  void testOverwriteExistingFile() throws IOException {
    Path targetFile = tempDir.resolve("stable.json");
    writer.writeAtomic(targetFile, new TestData("OLD", 10.0));

    TestData newData = new TestData("NEW", 20.0);
    writer.writeAtomic(targetFile, newData);

    TestData persisted = objectMapper.readValue(targetFile.toFile(), TestData.class);
    assertEquals("NEW", persisted.getAccountId());
  }

  // DTO de prueba interno
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestData {
    private String accountId;
    private double balance;
  }
}
