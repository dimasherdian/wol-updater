package com.wol.updater.application;

import java.nio.file.Path;
import com.wol.updater.domain.DownloadPackage;

public interface UpdateInstaller {
    boolean install(Path downloadedArchive, DownloadPackage pkg, Path targetInstallationPath, UpdateObserver observer);
    void executePostUpdateActions(DownloadPackage pkg, Path targetInstallationPath);
}
