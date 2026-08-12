package com.wol.updater.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record InstallationStatus(
    UpdaterState state,
    Optional<Path> installationPath,
    List<String> missingOrInvalidFiles
) {
    public static InstallationStatus noInstallation() {
        return new InstallationStatus(UpdaterState.NO_INSTALLATION, Optional.empty(), List.of());
    }
    
    public static InstallationStatus valid(Path path) {
        return new InstallationStatus(UpdaterState.CHECKING, Optional.of(path), List.of());
    }
    
    public static InstallationStatus invalid(Path path, List<String> issues) {
        return new InstallationStatus(UpdaterState.INVALID_INSTALLATION, Optional.of(path), issues);
    }
}
