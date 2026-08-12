package com.wol.updater.domain;

import java.util.List;

public record UpdatePlan(
    String targetVersion,
    String currentVersion,
    List<DownloadPackage> packagesToInstall
) {
    public boolean isUpToDate() {
        return packagesToInstall.isEmpty();
    }
}
