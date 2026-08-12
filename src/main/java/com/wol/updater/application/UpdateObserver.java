package com.wol.updater.application;

public interface UpdateObserver {

    void onUpdateStarted();
    
    void onInstallationNotFound();

    void onVersionDetected(String versionString);
    
    void onUnknownVersionDetected();
    
    void onUpToDate();
    
    void onUpdatePlanCreated(int totalUpdates);

    void onPackageDownloadStarted(int currentIndex, int totalPackages);

    void onPackageInstallStarted(int currentIndex, int totalPackages);

    void onPackageInstallSuccess(int currentIndex, int totalPackages);

    void onProgressUpdate(long bytesDownloaded, long totalBytes);

    void onProgressReset();

    void onError(String errorMessage, Exception e);

    boolean requestUserConfirmation(String title, String message);
    
    boolean isCancelled();
}
