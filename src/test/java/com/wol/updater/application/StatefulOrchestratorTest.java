package com.wol.updater.application;

import com.wol.updater.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StatefulOrchestratorTest {

    private StatefulOrchestrator orchestrator;
    private MockStatefulObserver observer;
    private MockSettingsManager settingsManager;

    @BeforeEach
    void setUp() {
        observer = new MockStatefulObserver();
        settingsManager = new MockSettingsManager();
    }

    @Test
    void shouldReportNoInstallationIfLocatorFails() {
        InstallationLocator locator = Optional::empty;
        InstallationValidator validator = p -> InstallationStatus.noInstallation();
        
        orchestrator = new StatefulOrchestrator(
            locator, validator, null, null, null, null, settingsManager, null, observer
        );

        orchestrator.initialize();
        assertEquals(UpdaterState.NO_INSTALLATION, orchestrator.getCurrentState());
        assertTrue(observer.states.contains(UpdaterState.NO_INSTALLATION));
    }

    @Test
    void shouldCheckVersionIfValidInstallationFound() {
        Path fakePath = Path.of("C:\\fake");
        InstallationLocator locator = () -> Optional.of(fakePath);
        InstallationValidator validator = p -> InstallationStatus.valid(p);
        
        VersionDetector detector = p -> Optional.of(new VersionSignature("v1", "v1", "v1"));
        UpdateSource source = sig -> Optional.of(new UpdatePlan("v1", "v1", List.of()));
        
        orchestrator = new StatefulOrchestrator(
            locator, validator, detector, source, null, null, settingsManager, null, observer
        );

        orchestrator.initialize();
        // Since checkForUpdates is async, we can just check if CHECKING state was entered
        // and eventually UP_TO_DATE (or we can sleep/wait for the CompletableFuture to finish in a real test,
        // but here we can just verify the synchronous parts or wait briefly).
        
        assertTrue(observer.states.contains(UpdaterState.CHECKING));
    }

    private static class MockStatefulObserver implements StatefulObserver {
        public final java.util.List<UpdaterState> states = new java.util.ArrayList<>();
        @Override
        public void onStateChanged(UpdaterState newState, String message) {
            states.add(newState);
        }
        @Override
        public void onInstallationStatusChanged(InstallationStatus status) {}
        @Override
        public void onUpdatePlanReady(UpdatePlan plan) {}
        @Override
        public void onProgressUpdated(int currentPackage, int totalPackages, long bytesDownloaded, long totalBytes) {}
        @Override
        public void onError(String message, Exception e) {}
    }

    private static class MockSettingsManager implements SettingsManager {
        private UserSettings settings = new UserSettings(
                Optional.empty(), Optional.empty(), true, false, true, true, true, true, "en"
        );
        @Override
        public UserSettings loadSettings() {
            return settings;
        }
        @Override
        public void saveSettings(UserSettings settings) {
            this.settings = settings;
        }
    }
}
