package com.wol.updater.application;

import com.wol.updater.domain.InstallationStatus;
import java.nio.file.Path;

public interface InstallationValidator {
    InstallationStatus validate(Path potentialPath);
}
