package pe.unsa.sd.persistence.recovery;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class SnapshotSchedulerTest {

    private SnapshotScheduler snapshotScheduler;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        snapshotScheduler = new SnapshotScheduler();
        ReflectionTestUtils.setField(snapshotScheduler, "baseDir", tempDir.toString());
        
        // Crear algunos datos ficticios
        Path ledgerDir = tempDir.resolve("ledger");
        Files.createDirectories(ledgerDir);
        Files.writeString(ledgerDir.resolve("account_1.json"), "{}");
        
        Path identityDir = tempDir.resolve("identity");
        Files.createDirectories(identityDir);
        Files.writeString(identityDir.resolve("user_1.json"), "{}");
    }

    @Test
    void testCreateSnapshot() throws IOException {
        // Act
        snapshotScheduler.createSnapshot();

        // Assert
        Path snapshotDir = tempDir.resolve("recovery/snapshots");
        assertTrue(Files.exists(snapshotDir), "El directorio de snapshots debió crearse");
        
        // Debería existir un archivo .zip
        var zipFiles = Files.list(snapshotDir).filter(p -> p.toString().endsWith(".zip")).toList();
        assertEquals(1, zipFiles.size(), "Debería haberse generado exactamente 1 archivo .zip");
        
        Path zipFile = zipFiles.get(0);
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            assertNotNull(zip.getEntry("ledger/account_1.json"), "El zip debe contener el archivo del ledger");
            assertNotNull(zip.getEntry("identity/user_1.json"), "El zip debe contener el archivo de identity");
        }
    }
}
