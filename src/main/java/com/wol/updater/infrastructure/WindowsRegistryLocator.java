package com.wol.updater.infrastructure;

import com.wol.updater.application.InstallationLocator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class WindowsRegistryLocator implements InstallationLocator {

    private static final String[][] REGISTRY_LOCATIONS = {
        {"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{EB448764-CABB-4766-8055-495AEA292020}_is1", "Inno Setup: App Path"},
        {"HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{EB448764-CABB-4766-8055-495AEA292020}_is1", "Inno Setup: App Path"},
        {"HKLM\\SOFTWARE\\Wars of Liberty\\Updater", "Path"}
    };

    @Override
    public Optional<Path> locate() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return Optional.empty();
        }

        for (String[] location : REGISTRY_LOCATIONS) {
            String key = location[0];
            String valueName = location[1];
            Optional<String> pathStr = queryRegistry(key, valueName);
            
            if (pathStr.isPresent()) {
                Path path = Paths.get(pathStr.get());
                if (LocalDirectoryLocator.isValidInstallation(path)) {
                    return Optional.of(path);
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> queryRegistry(String key, String valueName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", key, "/v", valueName);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(valueName)) {
                        String[] parts = line.split("REG_SZ");
                        if (parts.length > 1) {
                            return Optional.of(parts[1].trim());
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore exceptions during registry query (key might not exist or not Windows)
        }
        return Optional.empty();
    }
}
