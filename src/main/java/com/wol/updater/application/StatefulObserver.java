package com.wol.updater.application;

import com.wol.updater.domain.*;

public interface StatefulObserver {
    void onStateChanged(UpdaterState newState, String message);
    void onInstallationStatusChanged(InstallationStatus status);
    void onUpdatePlanReady(UpdatePlan plan);
    void onProgressUpdated(int currentPackage, int totalPackages, long bytesDownloaded, long totalBytes);
    void onError(String message, Exception e);
}
