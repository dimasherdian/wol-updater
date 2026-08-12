package com.wol.updater.infrastructure;

import com.wol.updater.application.InstallationLocator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class LocalDirectoryLocator implements InstallationLocator {
    @Override
    public Optional<Path> locate() {
        Path currentDir = Paths.get(".").toAbsolutePath().normalize();
        if (isValidInstallation(currentDir)) {
            return Optional.of(currentDir);
        }
        return Optional.empty();
    }

    static boolean isValidInstallation(Path path) {
        return Files.isRegularFile(path.resolve("data/protoy.xml")) &&
               Files.isRegularFile(path.resolve("data/stringtabley.xml")) &&
               Files.isRegularFile(path.resolve("data/techtreey.xml")) &&
               Files.isDirectory(path.resolve("art/zulushield"));
    }
}
