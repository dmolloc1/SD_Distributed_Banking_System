package pe.unsa.sd.persistence.recovery;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.unsa.sd.persistence.exception.StorageException;

@Slf4j
@Service
public class SnapshotScheduler {

    @Value("${bank.data.base-dir:data}")
    private String baseDir;

    @Scheduled(cron = "0 0 * * * ?") // Cada hora
    public void createSnapshot() {
        log.info("Iniciando creación de Snapshot (Hot Backup)...");
        Path ledgerDir = Path.of(baseDir, "ledger");
        Path identityDir = Path.of(baseDir, "identity");
        Path snapshotDir = Path.of(baseDir, "recovery", "snapshots");

        try {
            if (!Files.exists(snapshotDir)) {
                Files.createDirectories(snapshotDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path zipFilePath = snapshotDir.resolve("snapshot_" + timestamp + ".zip");

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
                compressDirectory(ledgerDir, zos, "ledger");
                compressDirectory(identityDir, zos, "identity");
            }

            log.info("Snapshot creado exitosamente en: {}", zipFilePath);

        } catch (IOException e) {
            log.error("Error al crear snapshot de recuperación", e);
            throw new StorageException("Error al crear snapshot de recuperación", e);
        }
    }

    private void compressDirectory(Path sourceDir, ZipOutputStream zos, String zipPrefix) throws IOException {
        if (!Files.exists(sourceDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    ZipEntry zipEntry = new ZipEntry(zipPrefix + "/" + path.getFileName().toString());
                    zos.putNextEntry(zipEntry);
                    Files.copy(path, zos);
                    zos.closeEntry();
                }
            }
        }
    }
}
