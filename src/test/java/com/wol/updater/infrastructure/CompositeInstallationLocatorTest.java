package com.wol.updater.infrastructure;

import com.wol.updater.application.InstallationLocator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompositeInstallationLocatorTest {

    @Test
    void shouldReturnFirstFoundPath() {
        InstallationLocator locator1 = () -> Optional.empty();
        InstallationLocator locator2 = () -> Optional.of(Path.of("C:\\wol-mock"));
        InstallationLocator locator3 = () -> Optional.of(Path.of("C:\\wol-ignore"));

        CompositeInstallationLocator composite = new CompositeInstallationLocator(List.of(locator1, locator2, locator3));

        Optional<Path> result = composite.locate();

        assertTrue(result.isPresent());
        assertEquals(Path.of("C:\\wol-mock"), result.get());
    }

    @Test
    void shouldReturnEmptyIfNoneFound() {
        InstallationLocator locator1 = () -> Optional.empty();
        InstallationLocator locator2 = () -> Optional.empty();

        CompositeInstallationLocator composite = new CompositeInstallationLocator(List.of(locator1, locator2));

        Optional<Path> result = composite.locate();

        assertTrue(result.isEmpty());
    }
}
