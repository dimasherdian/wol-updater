package com.wol.updater.infrastructure;

import com.wol.updater.application.InstallationValidator;
import com.wol.updater.domain.InstallationStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StrictInstallationValidator implements InstallationValidator {

    @Override
    public InstallationStatus validate(Path potentialPath) {
        if (potentialPath == null || !Files.exists(potentialPath) || !Files.isDirectory(potentialPath)) {
            return InstallationStatus.noInstallation();
        }

        List<String> missingFiles = new ArrayList<>();
        
        Path protoy = potentialPath.resolve("data").resolve("protoy.xml");
        Path stringtabley = potentialPath.resolve("data").resolve("stringtabley.xml");
        Path techtreey = potentialPath.resolve("data").resolve("techtreey.xml");
        Path zuluShield = potentialPath.resolve("art").resolve("zulushield");

        if (!Files.exists(protoy)) missingFiles.add("data\\protoy.xml");
        if (!Files.exists(stringtabley)) missingFiles.add("data\\stringtabley.xml");
        if (!Files.exists(techtreey)) missingFiles.add("data\\techtreey.xml");
        if (!Files.exists(zuluShield)) missingFiles.add("art\\zulushield");

        if (!missingFiles.isEmpty()) {
            return InstallationStatus.invalid(potentialPath, missingFiles);
        }

        return InstallationStatus.valid(potentialPath);
    }
}
