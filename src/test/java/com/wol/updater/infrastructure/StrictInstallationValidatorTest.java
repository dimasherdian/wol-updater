package com.wol.updater.infrastructure;

import com.wol.updater.domain.InstallationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StrictInstallationValidatorTest {

    private final StrictInstallationValidator validator = new StrictInstallationValidator();

    @Test
    void shouldReturnNoInstallationIfPathNull() {
        InstallationStatus status = validator.validate(null);
        assertEquals(com.wol.updater.domain.UpdaterState.NO_INSTALLATION, status.state());
        assertFalse(status.installationPath().isPresent());
        assertTrue(status.missingOrInvalidFiles().isEmpty());
    }

    @Test
    void shouldReturnNoInstallationIfPathDoesNotExist(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("missing_dir");
        InstallationStatus status = validator.validate(nonExistent);
        assertEquals(com.wol.updater.domain.UpdaterState.NO_INSTALLATION, status.state());
        assertFalse(status.installationPath().isPresent());
    }

    @Test
    void shouldReturnInvalidIfFilesAreMissing(@TempDir Path tempDir) throws Exception {
        // Create directory but no files
        Files.createDirectories(tempDir.resolve("data"));
        Files.createDirectories(tempDir.resolve("art"));

        InstallationStatus status = validator.validate(tempDir);

        assertEquals(com.wol.updater.domain.UpdaterState.INVALID_INSTALLATION, status.state());
        assertTrue(status.installationPath().isPresent());
        assertEquals(4, status.missingOrInvalidFiles().size());
        assertTrue(status.missingOrInvalidFiles().contains("data\\protoy.xml"));
        assertTrue(status.missingOrInvalidFiles().contains("data\\stringtabley.xml"));
        assertTrue(status.missingOrInvalidFiles().contains("data\\techtreey.xml"));
        assertTrue(status.missingOrInvalidFiles().contains("art\\zulushield"));
    }

    @Test
    void shouldReturnValidIfAllFilesExist(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("data"));
        Files.createDirectories(tempDir.resolve("art"));

        Files.createFile(tempDir.resolve("data").resolve("protoy.xml"));
        Files.createFile(tempDir.resolve("data").resolve("stringtabley.xml"));
        Files.createFile(tempDir.resolve("data").resolve("techtreey.xml"));
        Files.createFile(tempDir.resolve("art").resolve("zulushield"));

        InstallationStatus status = validator.validate(tempDir);

        assertEquals(com.wol.updater.domain.UpdaterState.CHECKING, status.state());
        assertTrue(status.installationPath().isPresent());
        assertTrue(status.missingOrInvalidFiles().isEmpty());
    }
}
