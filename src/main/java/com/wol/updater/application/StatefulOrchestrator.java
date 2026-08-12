package com.wol.updater.application;

import com.wol.updater.domain.*;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;

public class StatefulOrchestrator {

    private final InstallationLocator installationLocator;
    private final InstallationValidator installationValidator;
    private final VersionDetector versionDetector;
    private final UpdateSource updateSource;
    private final FileDownloader fileDownloader;
    private final UpdateInstaller updateInstaller;
    private final SettingsManager settingsManager;
    private final HistoryManager historyManager;
    private final StatefulObserver observer;

    private UpdaterState currentState = UpdaterState.NO_INSTALLATION;
    private UserSettings currentSettings;
    private InstallationStatus currentInstallation = InstallationStatus.noInstallation();
    private UpdatePlan currentPlan;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private int currentPackageIndex = 0;
    private boolean isAborting = false;

    public StatefulOrchestrator(
        InstallationLocator installationLocator,
        InstallationValidator installationValidator,
        VersionDetector versionDetector,
        UpdateSource updateSource,
        FileDownloader fileDownloader,
        UpdateInstaller updateInstaller,
        SettingsManager settingsManager,
        HistoryManager historyManager,
        StatefulObserver observer
    ) {
        this.installationLocator = installationLocator;
        this.installationValidator = installationValidator;
        this.versionDetector = versionDetector;
        this.updateSource = updateSource;
        this.fileDownloader = fileDownloader;
        this.updateInstaller = updateInstaller;
        this.settingsManager = settingsManager;
        this.historyManager = historyManager;
        this.observer = observer;
    }

    public void initialize() {
        currentSettings = settingsManager.loadSettings();
        
        Optional<Path> pathToCheck = currentSettings.installationPath();
        if (pathToCheck.isEmpty()) {
            pathToCheck = installationLocator.locate();
        }

        if (pathToCheck.isPresent()) {
            validateAndSetInstallation(pathToCheck.get());
        } else {
            changeState(UpdaterState.NO_INSTALLATION, "Wars of Liberty installation not found.");
        }
    }

    public void setInstallationPath(Path newPath) {
        updateSettings(new UserSettings(
            Optional.of(newPath),
            currentSettings.temporaryDownloadPath(),
            currentSettings.checkOnStartup(),
            currentSettings.autoDownload(),
            currentSettings.confirmBeforeInstall(),
            currentSettings.backupBeforeUpdate(),
            currentSettings.verifyFiles(),
            currentSettings.enableLogging(),
            currentSettings.language()
        ));
    }

    public void updateSettings(UserSettings newSettings) {
        boolean pathChanged = !newSettings.installationPath().equals(this.currentSettings.installationPath());
        this.currentSettings = newSettings;
        settingsManager.saveSettings(currentSettings);
        
        if (pathChanged && currentSettings.installationPath().isPresent()) {
            validateAndSetInstallation(currentSettings.installationPath().get());
        }
    }

    private void validateAndSetInstallation(Path path) {
        currentInstallation = installationValidator.validate(path);
        observer.onInstallationStatusChanged(currentInstallation);
        
        if (currentInstallation.state() == UpdaterState.CHECKING) {
            changeState(UpdaterState.CHECKING, "Installation found. Checking version...");
            CompletableFuture.runAsync(this::checkForUpdates);
        } else {
            changeState(currentInstallation.state(), "Invalid or corrupted installation.");
        }
    }

    public void checkForUpdates() {
        if (currentInstallation.installationPath().isEmpty() || currentInstallation.state() != UpdaterState.CHECKING) {
            return;
        }

        Path installPath = currentInstallation.installationPath().get();
        Optional<VersionSignature> sigOpt = versionDetector.detect(installPath);
        
        if (sigOpt.isEmpty()) {
            changeState(UpdaterState.CORRUPTED_INSTALLATION, "Could not compute version signatures.");
            return;
        }

        Optional<UpdatePlan> planOpt = updateSource.getUpdatePlan(sigOpt.get());
        if (planOpt.isEmpty()) {
            changeState(UpdaterState.CORRUPTED_INSTALLATION, "Unknown local version or unable to reach update server.");
            return;
        }

        currentPlan = planOpt.get();
        currentPackageIndex = 0;
        observer.onUpdatePlanReady(currentPlan);

        if (currentPlan.isUpToDate()) {
            changeState(UpdaterState.UP_TO_DATE, "Wars of Liberty is up to date (" + currentPlan.currentVersion() + ").");
        } else {
            changeState(UpdaterState.UPDATE_AVAILABLE, "Update available: " + currentPlan.targetVersion());
            if (currentSettings.autoDownload()) {
                Path tempDir = currentSettings.temporaryDownloadPath().orElse(installPath.resolve("temp_update"));
                startUpdate(tempDir);
            }
        }
    }

    public void startUpdate() {
        if (currentInstallation.installationPath().isEmpty()) return;
        Path tempDir = currentSettings.temporaryDownloadPath().orElse(currentInstallation.installationPath().get().resolve("temp_update"));
        startUpdate(tempDir);
    }

    public void startUpdate(Path temporaryDirectory) {
        if ((currentState != UpdaterState.UPDATE_AVAILABLE && currentState != UpdaterState.PAUSED) || currentPlan == null || currentInstallation.installationPath().isEmpty()) {
            return;
        }

        cancelled.set(false);
        isAborting = false;
        CompletableFuture.runAsync(() -> {
            try {
                executeUpdateLoop(temporaryDirectory, currentInstallation.installationPath().get());
            } catch (Exception e) {
                changeState(UpdaterState.ERROR, "Update encountered an error.");
                observer.onError("Fatal update error", e);
            }
        });
    }

    private void executeUpdateLoop(Path tempDir, Path installPath) {
        int total = currentPlan.packagesToInstall().size();
        final java.util.concurrent.atomic.AtomicInteger current = new java.util.concurrent.atomic.AtomicInteger(currentPackageIndex + 1);

        // Create a bridge observer for the legacy downloader/installer
        UpdateObserver legacyBridge = new UpdateObserver() {
            @Override public void onUpdateStarted() {}
            @Override public void onInstallationNotFound() {}
            @Override public void onVersionDetected(String versionString) {}
            @Override public void onUnknownVersionDetected() {}
            @Override public void onUpToDate() {}
            @Override public void onUpdatePlanCreated(int totalUpdates) {}
            
            @Override public void onPackageDownloadStarted(int idx, int tot) {
                changeState(UpdaterState.DOWNLOADING, "Downloading package " + idx + " of " + tot);
            }
            @Override public void onPackageInstallStarted(int idx, int tot) {
                changeState(UpdaterState.INSTALLING, "Installing package " + idx + " of " + tot);
            }
            @Override public void onPackageInstallSuccess(int idx, int tot) {}
            @Override public void onProgressUpdate(long bytesDownloaded, long totalBytes) {
                observer.onProgressUpdated(current.get(), total, bytesDownloaded, totalBytes);
            }
            @Override public void onProgressReset() {
                observer.onProgressUpdated(current.get(), total, 0, 0);
            }
            @Override public void onError(String errorMessage, Exception e) {
                // Handled below by return values
            }
            @Override public boolean requestUserConfirmation(String title, String message) {
                return true; // Auto-confirm in this architecture, or delegate to UI
            }
            @Override public boolean isCancelled() {
                return cancelled.get();
            }
        };

        for (; currentPackageIndex < currentPlan.packagesToInstall().size(); currentPackageIndex++) {
            DownloadPackage pkg = currentPlan.packagesToInstall().get(currentPackageIndex);
            if (cancelled.get()) {
                handleCancellation(tempDir);
                return;
            }

            legacyBridge.onPackageDownloadStarted(current.get(), total);
            Optional<Path> archive = fileDownloader.download(pkg, tempDir, legacyBridge);
            
            if (archive.isEmpty()) {
                if (cancelled.get()) {
                    handleCancellation(tempDir);
                    return;
                }
                changeState(UpdaterState.ERROR, "Failed to download package " + current.get());
                return;
            }

            if (cancelled.get()) {
                handleCancellation(tempDir);
                return;
            }

            legacyBridge.onPackageInstallStarted(current.get(), total);
            boolean success = updateInstaller.install(archive.get(), pkg, installPath, legacyBridge);
            if (!success) {
                changeState(UpdaterState.ERROR, "Failed to install package " + current.get());
                return;
            }

            updateInstaller.executePostUpdateActions(pkg, installPath);
            current.incrementAndGet();
        }
        
        // Cleanup temporary directory
        try {
            com.wol.updater.infrastructure.FileUtils.deleteDirectory(tempDir);
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        if (historyManager != null && currentPlan != null) {
            historyManager.addRecord(new UpdateRecord(
                java.time.LocalDateTime.now(),
                currentPlan.targetVersion(),
                "SUCCESS",
                "Updated to version " + currentPlan.targetVersion()
            ));
        }

        changeState(UpdaterState.COMPLETED, "Update finished successfully.");
    }

    private void handleCancellation(Path tempDir) {
        if (isAborting) {
            currentPackageIndex = 0;
            try { com.wol.updater.infrastructure.FileUtils.deleteDirectory(tempDir); } catch (Exception ignored) {}
            changeState(UpdaterState.UPDATE_AVAILABLE, "Update aborted. Ready to update.");
        } else {
            changeState(UpdaterState.PAUSED, "Update paused.");
        }
    }

    public void pauseUpdate() {
        if (currentState == UpdaterState.DOWNLOADING || currentState == UpdaterState.INSTALLING) {
            isAborting = false;
            cancelled.set(true);
        }
    }

    public void abortUpdate() {
        if (currentState == UpdaterState.DOWNLOADING || currentState == UpdaterState.INSTALLING || currentState == UpdaterState.PAUSED) {
            isAborting = true;
            cancelled.set(true);
            
            if (currentState == UpdaterState.PAUSED) {
                // Manually trigger handleCancellation since thread is dead
                Path tempDir = currentSettings.temporaryDownloadPath().orElse(currentInstallation.installationPath().get().resolve("temp_update"));
                handleCancellation(tempDir);
            }
        }
    }

    private void changeState(UpdaterState newState, String message) {
        this.currentState = newState;
        observer.onStateChanged(newState, message);
    }
    
    public UserSettings getCurrentSettings() {
        return currentSettings;
    }
    
    public InstallationStatus getCurrentInstallation() {
        return currentInstallation;
    }
    
    public UpdatePlan getCurrentPlan() {
        return currentPlan;
    }

    public UpdaterState getCurrentState() {
        return currentState;
    }
}
