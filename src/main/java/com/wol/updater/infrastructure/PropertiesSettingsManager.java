package com.wol.updater.infrastructure;

import com.wol.updater.application.SettingsManager;
import com.wol.updater.domain.UserSettings;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class PropertiesSettingsManager implements SettingsManager {

    private final Path settingsFile;

    public PropertiesSettingsManager() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        Path configDir = Path.of(appData, "WoLUpdater");
        configDir.toFile().mkdirs();
        this.settingsFile = configDir.resolve("settings.properties");
    }

    public PropertiesSettingsManager(Path settingsFile) {
        this.settingsFile = settingsFile;
        this.settingsFile.getParent().toFile().mkdirs();
    }

    @Override
    public UserSettings loadSettings() {
        if (!settingsFile.toFile().exists()) {
            return UserSettings.defaultSettings();
        }

        try (FileInputStream in = new FileInputStream(settingsFile.toFile())) {
            Properties props = new Properties();
            props.load(in);

            String pathStr = props.getProperty("installationPath");
            Optional<Path> path = (pathStr != null && !pathStr.isEmpty()) ? Optional.of(Path.of(pathStr)) : Optional.empty();

            String tempPathStr = props.getProperty("temporaryDownloadPath");
            Optional<Path> tempPath = (tempPathStr != null && !tempPathStr.isEmpty()) ? Optional.of(Path.of(tempPathStr)) : Optional.empty();

            return new UserSettings(
                path,
                tempPath,
                Boolean.parseBoolean(props.getProperty("checkOnStartup", "true")),
                Boolean.parseBoolean(props.getProperty("autoDownload", "false")),
                Boolean.parseBoolean(props.getProperty("confirmBeforeInstall", "true")),
                Boolean.parseBoolean(props.getProperty("backupBeforeUpdate", "true")),
                Boolean.parseBoolean(props.getProperty("verifyFiles", "true")),
                Boolean.parseBoolean(props.getProperty("enableLogging", "true")),
                props.getProperty("language", "en")
            );
        } catch (Exception e) {
            return UserSettings.defaultSettings();
        }
    }

    @Override
    public void saveSettings(UserSettings settings) {
        Properties props = new Properties();
        settings.installationPath().ifPresent(p -> props.setProperty("installationPath", p.toAbsolutePath().toString()));
        settings.temporaryDownloadPath().ifPresent(p -> props.setProperty("temporaryDownloadPath", p.toAbsolutePath().toString()));
        props.setProperty("checkOnStartup", String.valueOf(settings.checkOnStartup()));
        props.setProperty("autoDownload", String.valueOf(settings.autoDownload()));
        props.setProperty("confirmBeforeInstall", String.valueOf(settings.confirmBeforeInstall()));
        props.setProperty("backupBeforeUpdate", String.valueOf(settings.backupBeforeUpdate()));
        props.setProperty("verifyFiles", String.valueOf(settings.verifyFiles()));
        props.setProperty("enableLogging", String.valueOf(settings.enableLogging()));
        props.setProperty("language", settings.language());

        try (FileOutputStream out = new FileOutputStream(settingsFile.toFile())) {
            props.store(out, "Wars of Liberty Updater Settings");
        } catch (Exception e) {
            // Log warning or ignore silently as per architecture
        }
    }
}
