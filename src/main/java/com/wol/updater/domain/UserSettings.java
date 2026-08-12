package com.wol.updater.domain;

import java.nio.file.Path;
import java.util.Optional;

public record UserSettings(
    Optional<Path> installationPath,
    Optional<Path> temporaryDownloadPath,
    boolean checkOnStartup,
    boolean autoDownload,
    boolean confirmBeforeInstall,
    boolean backupBeforeUpdate,
    boolean verifyFiles,
    boolean enableLogging,
    String language
) {
    public static UserSettings defaultSettings() {
        return new UserSettings(
            Optional.empty(),
            Optional.empty(),
            true,   // checkOnStartup
            false,  // autoDownload
            true,   // confirmBeforeInstall
            true,   // backupBeforeUpdate
            true,   // verifyFiles
            true,   // enableLogging
            "en"    // language
        );
    }
}
