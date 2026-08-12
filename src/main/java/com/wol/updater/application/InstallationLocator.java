package com.wol.updater.application;

import java.nio.file.Path;
import java.util.Optional;

public interface InstallationLocator {
    Optional<Path> locate();
}
