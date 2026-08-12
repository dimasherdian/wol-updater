package com.wol.updater.infrastructure;

import com.wol.updater.application.InstallationLocator;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class CompositeInstallationLocator implements InstallationLocator {
    private final List<InstallationLocator> locators;

    public CompositeInstallationLocator() {
        this(List.of(
            new LocalDirectoryLocator(),
            new WindowsRegistryLocator()
        ));
    }

    public CompositeInstallationLocator(List<InstallationLocator> locators) {
        this.locators = locators;
    }

    @Override
    public Optional<Path> locate() {
        for (InstallationLocator locator : locators) {
            Optional<Path> path = locator.locate();
            if (path.isPresent()) {
                return path;
            }
        }
        return Optional.empty();
    }
}
