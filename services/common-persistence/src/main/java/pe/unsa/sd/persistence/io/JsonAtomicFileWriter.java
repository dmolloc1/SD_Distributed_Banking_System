package pe.unsa.sd.persistence.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.unsa.sd.persistence.exception.AtomicWriteException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAtomicFileWriter implements AtomicFileWriter {

  private final ObjectMapper objectMapper;
  private static final String TEMP_EXTENSION = ".tmp";

  @Override
  public void writeAtomic(Path targetPath, Object data) throws AtomicWriteException {
    // Aseguramos que el directorio padre existe antes de cualquier operación
    try {
      if (targetPath.getParent() != null) {
        Files.createDirectories(targetPath.getParent());
      }
    } catch (IOException e) {
      throw new AtomicWriteException("No se pudieron crear los directorios para: " + targetPath, e);
    }

    Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + TEMP_EXTENSION);

    try {
      // Escribir en el archivo temporal (Shadow File)
      writeToTempFile(tempPath, data);

      // Renombrado Atómico (Punto de Commit)
      Files.move(
          tempPath,
          targetPath,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);

      log.debug("Escritura atómica completada: {}", targetPath);

    } catch (IOException e) {
      cleanUp(tempPath);
      throw new AtomicWriteException("Fallo en la escritura atómica sobre: " + targetPath, e);
    }
  }

  private void writeToTempFile(Path tempPath, Object data) throws IOException {
    // Configuramos Jackson para NO cerrar el stream automáticamente
    objectMapper.getFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

    try (FileOutputStream fos = new FileOutputStream(tempPath.toFile());
        FileChannel channel = fos.getChannel()) {

      // Serialización Jackson
      objectMapper.writeValue(fos, data);

      // Ahora el canal sigue ABIERTO y podemos forzar el flush físico
      channel.force(true);

      log.trace("Flush físico completado para {}", tempPath.getFileName());
    }
    // El try-with-resources cerrará el fos y el channel al salir de este bloque
  }

  private void cleanUp(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.warn("Limpieza fallida de archivo temporal: {}", path);
    }
  }
}
