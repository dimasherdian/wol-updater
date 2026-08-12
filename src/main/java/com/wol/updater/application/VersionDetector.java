package com.wol.updater.application;

import com.wol.updater.domain.VersionSignature;
import java.nio.file.Path;
import java.util.Optional;

public interface VersionDetector {
    Optional<VersionSignature> detect(Path installationPath);
}
