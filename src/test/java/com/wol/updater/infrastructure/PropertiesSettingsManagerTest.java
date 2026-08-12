package com.wol.updater.infrastructure;

import com.wol.updater.domain.UserSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesSettingsManagerTest {

    @Test
    void shouldLoadDefaultSettingsIfFileDoesNotExist(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("missing.properties");
        PropertiesSettingsManager manager = new PropertiesSettingsManager(settingsFile);

        UserSettings settings = manager.loadSettings();

        assertTrue(settings.checkOnStartup());
        assertFalse(settings.autoDownload());
        assertTrue(settings.confirmBeforeInstall());
        assertTrue(settings.backupBeforeUpdate());
        assertTrue(settings.verifyFiles());
        assertTrue(settings.enableLogging());
        assertTrue(settings.installationPath().isEmpty());
    }

    @Test
    void shouldSaveAndLoadSettings(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("settings.properties");
        PropertiesSettingsManager manager = new PropertiesSettingsManager(settingsFile);

        UserSettings toSave = new UserSettings(
            Optional.of(Path.of("C:\\wol")),
            Optional.empty(),
            false, true, false, false, false, false, "en"
        );

        manager.saveSettings(toSave);

        assertTrue(settingsFile.toFile().exists());

        UserSettings loaded = manager.loadSettings();
        
        assertEquals(Optional.of(Path.of("C:\\wol")), loaded.installationPath());
        assertEquals(Optional.empty(), loaded.temporaryDownloadPath());
        assertFalse(loaded.checkOnStartup());
        assertTrue(loaded.autoDownload());
        assertFalse(loaded.confirmBeforeInstall());
        assertFalse(loaded.backupBeforeUpdate());
        assertFalse(loaded.verifyFiles());
        assertFalse(loaded.enableLogging());
        assertEquals("en", loaded.language());
    }
}
