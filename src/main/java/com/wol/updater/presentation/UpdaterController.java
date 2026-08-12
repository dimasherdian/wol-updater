package com.wol.updater.presentation;

import com.wol.updater.application.StatefulObserver;
import com.wol.updater.application.StatefulOrchestrator;
import com.wol.updater.domain.InstallationStatus;
import com.wol.updater.domain.UpdatePlan;
import com.wol.updater.domain.UpdaterState;
import com.wol.updater.domain.UserSettings;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.geometry.Insets;
import javafx.scene.Parent;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import java.util.ResourceBundle;
import java.net.URL;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;

public class UpdaterController implements StatefulObserver, Initializable {

    // Views
    @FXML private VBox homeView;
    @FXML private VBox updatesView;
    @FXML private VBox settingsView;
    @FXML private VBox userDataView;
    @FXML private VBox aboutView;
    
    // Navigation Buttons
    @FXML private Button navHome;
    @FXML private Button navUpdates;
    @FXML private Button navSettings;
    @FXML private Button navUserData;
    @FXML private Button navAbout;

    // Home View Elements
    @FXML private Label statusTitle;
    @FXML private Label statusSubtitle;
    @FXML private Label installPathText;
    @FXML private Label installedVersionText;
    @FXML private Label latestVersionText;
    @FXML private Button browseButton;
    @FXML private Button actionButton;

    // Updates View Elements
    @FXML private Label updateProgressTitle;
    @FXML private VBox progressContainer;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressText;
    @FXML private Label progressDetails;
    @FXML private Button pauseButton;
    @FXML private Button resumeButton;
    @FXML private Button abortButton;

    // Settings View Elements
    @FXML private javafx.scene.control.TextField installPathField;
    @FXML private javafx.scene.control.TextField downloadPathField;
    @FXML private CheckBox chkCheckOnStartup;
    @FXML private CheckBox chkAutoDownload;
    @FXML private CheckBox chkConfirmBeforeInstall;
    @FXML private CheckBox chkBackup;
    @FXML private CheckBox chkVerifyFiles;
    @FXML private CheckBox chkLogging;
    @FXML private ComboBox<String> languageComboBox;

    private ResourceBundle resources;

    // User Data View Elements
    @FXML private javafx.scene.control.TextField userDataPathField;

    // Notification Panel
    @FXML private javafx.scene.layout.AnchorPane notificationOverlay;
    @FXML private VBox notificationList;
    @FXML private javafx.scene.shape.Circle notificationBadge;

    // About View Elements
    @FXML private ImageView aboutIconView;

    // Window and UI Controls
    @FXML private VBox sidebar;
    @FXML private javafx.scene.shape.SVGPath themeIconPath;
    @FXML private javafx.scene.text.Text brandText;
    
    // Page Titles (for hide/show logic)
    @FXML private javafx.scene.text.Text pageTitleHome;
    @FXML private javafx.scene.text.Text pageTitleSettings;
    @FXML private javafx.scene.text.Text pageTitleUserData;
    @FXML private javafx.scene.text.Text pageTitleAbout;
    
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean sidebarExpanded = false;

    private StatefulOrchestrator orchestrator;
    private com.wol.updater.application.HistoryManager historyManager;

    public void setHistoryManager(com.wol.updater.application.HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;
        if (languageComboBox != null) {
            languageComboBox.getItems().addAll(resources.getString("language.en"), resources.getString("language.id"));
        }
    }

    public void setOrchestrator(StatefulOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        loadSettingsToUI(orchestrator.getCurrentSettings());
        
        // Initialize About Icon
        if (aboutIconView != null) {
            try {
                aboutIconView.setImage(new Image(getClass().getResourceAsStream("/wol.ico")));
            } catch (Exception e) {
                // Ignore if icon not found
            }
        }
        
        populateDummyNotifications();
    }
    
    private void populateDummyNotifications() {
        if (notificationList != null) {
            notificationList.getChildren().clear();
            
            VBox emptyState = new VBox(5);
            emptyState.getStyleClass().add("notification-item");
            Label msgLabel = new Label(resources.getString("notify.empty"));
            msgLabel.getStyleClass().add("notification-item-title");
            emptyState.getChildren().add(msgLabel);
            
            notificationList.getChildren().add(emptyState);
        }
    }
    
    // End of Notification Panel Logic

    // --- Navigation Handlers ---

    @FXML private void showHome() { switchView(homeView, navHome); }
    @FXML private void showUpdates() { switchView(updatesView, navUpdates); }
    @FXML private void showSettings() { switchView(settingsView, navSettings); }
    @FXML private void showUserData() { switchView(userDataView, navUserData); }
    @FXML private void showAbout() { switchView(aboutView, navAbout); }

    private void switchView(VBox view, Button navButton) {
        homeView.setVisible(false);
        updatesView.setVisible(false);
        settingsView.setVisible(false);
        userDataView.setVisible(false);
        aboutView.setVisible(false);

        navHome.getStyleClass().remove("active-nav");
        navUpdates.getStyleClass().remove("active-nav");
        navSettings.getStyleClass().remove("active-nav");
        navUserData.getStyleClass().remove("active-nav");
        navAbout.getStyleClass().remove("active-nav");

        view.setVisible(true);
        navButton.getStyleClass().add("active-nav");
    }

    // --- Action Handlers ---
    
    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        if (sidebar != null) {
            double currentWidth = sidebar.getPrefWidth();
            boolean isExpanded = currentWidth > 100;
            
            if (isExpanded) {
                sidebar.setPrefWidth(60);
                brandText.setVisible(false);
                brandText.setManaged(false);
                sidebar.getStyleClass().remove("sidebar");
                
                navHome.setVisible(false); navHome.setManaged(false);
                navUpdates.setVisible(false); navUpdates.setManaged(false);
                navSettings.setVisible(false); navSettings.setManaged(false);
                navUserData.setVisible(false); navUserData.setManaged(false);
                navAbout.setVisible(false); navAbout.setManaged(false);
                
                if (pageTitleHome != null) { pageTitleHome.setVisible(true); pageTitleHome.setManaged(true); }
                if (pageTitleSettings != null) { pageTitleSettings.setVisible(true); pageTitleSettings.setManaged(true); }
                if (pageTitleUserData != null) { pageTitleUserData.setVisible(true); pageTitleUserData.setManaged(true); }
                if (pageTitleAbout != null) { pageTitleAbout.setVisible(true); pageTitleAbout.setManaged(true); }
            } else {
                sidebar.setPrefWidth(220);
                brandText.setVisible(true);
                brandText.setManaged(true);
                if (!sidebar.getStyleClass().contains("sidebar")) {
                    sidebar.getStyleClass().add("sidebar");
                }
                
                navHome.setVisible(true); navHome.setManaged(true);
                boolean isUpdatingState = orchestrator != null && 
                    (orchestrator.getCurrentState() == com.wol.updater.domain.UpdaterState.DOWNLOADING || 
                     orchestrator.getCurrentState() == com.wol.updater.domain.UpdaterState.INSTALLING);
                navUpdates.setVisible(isUpdatingState); navUpdates.setManaged(isUpdatingState);
                navSettings.setVisible(true); navSettings.setManaged(true);
                navUserData.setVisible(true); navUserData.setManaged(true);
                navAbout.setVisible(true); navAbout.setManaged(true);
                
                if (pageTitleHome != null) { pageTitleHome.setVisible(false); pageTitleHome.setManaged(false); }
                if (pageTitleSettings != null) { pageTitleSettings.setVisible(false); pageTitleSettings.setManaged(false); }
                if (pageTitleUserData != null) { pageTitleUserData.setVisible(false); pageTitleUserData.setManaged(false); }
                if (pageTitleAbout != null) { pageTitleAbout.setVisible(false); pageTitleAbout.setManaged(false); }
            }
        }
    }
    
    @FXML
    private void handleToggleTheme(ActionEvent event) {
        if (navHome != null && navHome.getScene() != null) {
            Parent root = navHome.getScene().getRoot();
            if (root.getStyleClass().contains("light-mode")) {
                root.getStyleClass().remove("light-mode");
                root.getStyleClass().add("dark-mode");
            } else {
                root.getStyleClass().remove("dark-mode");
                root.getStyleClass().add("light-mode");
            }
        }
    }
    

    @FXML
    private void handleBrowse(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Wars of Liberty Installation Folder");
        File dir = dc.showDialog(browseButton.getScene().getWindow());
        if (dir != null) {
            orchestrator.setInstallationPath(dir.toPath());
        }
    }

    @FXML
    private void handleChangeInstallPath(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Wars of Liberty Installation Folder");
        File dir = dc.showDialog(navSettings.getScene().getWindow());
        if (dir != null) {
            UserSettings current = orchestrator.getCurrentSettings();
            orchestrator.updateSettings(new UserSettings(
                Optional.of(dir.toPath()),
                current.temporaryDownloadPath(),
                current.checkOnStartup(),
                current.autoDownload(),
                current.confirmBeforeInstall(),
                current.backupBeforeUpdate(),
                current.verifyFiles(),
                current.enableLogging(),
                current.language()
            ));
            loadSettingsToUI(orchestrator.getCurrentSettings());
        }
    }

    @FXML
    private void handleChangeDownloadPath(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Temporary Download Folder");
        File dir = dc.showDialog(navSettings.getScene().getWindow());
        if (dir != null) {
            UserSettings current = orchestrator.getCurrentSettings();
            orchestrator.updateSettings(new UserSettings(
                current.installationPath(),
                Optional.of(dir.toPath()),
                current.checkOnStartup(),
                current.autoDownload(),
                current.confirmBeforeInstall(),
                current.backupBeforeUpdate(),
                current.verifyFiles(),
                current.enableLogging(),
                current.language()
            ));
            loadSettingsToUI(orchestrator.getCurrentSettings());
        }
    }

    @FXML
    private void handlePause(ActionEvent event) {
        orchestrator.pauseUpdate();
    }

    @FXML
    private void handleResume(ActionEvent event) {
        orchestrator.startUpdate();
    }

    @FXML
    private void handleAbort(ActionEvent event) {
        orchestrator.abortUpdate();
    }

    @FXML
    private void handleAction(ActionEvent event) {
        UpdaterState state = orchestrator.getCurrentState();
        if (state == UpdaterState.UPDATE_AVAILABLE || state == UpdaterState.PAUSED) {
            orchestrator.startUpdate();
        } else if (state == UpdaterState.UP_TO_DATE || state == UpdaterState.COMPLETED) {
            launchGame();
        }
    }

    private void launchGame() {
        Optional<Path> installPath = orchestrator.getCurrentSettings().installationPath();
        if (installPath.isPresent()) {
            File exe = installPath.get().resolve("age3y.exe").toFile();
            if (exe.exists()) {
                try {
                    // Detach the process using cmd /c start so the updater can exit cleanly
                    new ProcessBuilder("cmd", "/c", "start", "\"\"", exe.getAbsolutePath())
                        .directory(exe.getParentFile())
                        .start();
                } catch (Exception e) {
                    showInfoDialog("Launch Error", "Failed to launch game: " + e.getMessage());
                }
            } else {
                showInfoDialog("File Not Found", "Could not find age3y.exe in the installation folder.");
            }
        }
        // Force exit the updater completely so it doesn't stay stuck in the background
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleSaveSettings(ActionEvent event) {
        UserSettings current = orchestrator.getCurrentSettings();
        UserSettings newSettings = new UserSettings(
            current.installationPath(),
            current.temporaryDownloadPath(),
            chkCheckOnStartup.isSelected(),
            chkAutoDownload.isSelected(),
            chkConfirmBeforeInstall.isSelected(),
            chkBackup.isSelected(),
            chkVerifyFiles.isSelected(),
            chkLogging.isSelected(),
            resources.getString("language.id").equals(languageComboBox.getValue()) ? "id" : "en"
        );
        orchestrator.updateSettings(newSettings);
        if (!current.language().equals(newSettings.language())) {
            this.resources = java.util.ResourceBundle.getBundle("i18n.Messages", new java.util.Locale(newSettings.language()));
            
            if (navHome != null) navHome.setText(resources.getString("tab.home"));
            if (navUpdates != null) navUpdates.setText(resources.getString("tab.update"));
            if (navUserData != null) navUserData.setText(resources.getString("tab.user_data"));
            if (navSettings != null) navSettings.setText(resources.getString("tab.settings"));
            if (navAbout != null) navAbout.setText(resources.getString("tab.about"));
            
            if (pageTitleHome != null) pageTitleHome.setText(resources.getString("tab.home"));
            if (pageTitleSettings != null) pageTitleSettings.setText(resources.getString("tab.settings"));
            if (pageTitleUserData != null) pageTitleUserData.setText(resources.getString("tab.user_data"));
            if (pageTitleAbout != null) pageTitleAbout.setText(resources.getString("tab.about"));
            
            populateDummyNotifications(); // Updates empty notification text
            
            showInfoDialog(resources.getString("notify.title"), resources.getString("notify.language_changed"));
        }
    }

    private void loadSettingsToUI(UserSettings settings) {
        if (settings == null) return;
        chkCheckOnStartup.setSelected(settings.checkOnStartup());
        chkAutoDownload.setSelected(settings.autoDownload());
        chkConfirmBeforeInstall.setSelected(settings.confirmBeforeInstall());
        chkBackup.setSelected(settings.backupBeforeUpdate());
        chkVerifyFiles.setSelected(settings.verifyFiles());
        chkLogging.setSelected(settings.enableLogging());
        if (languageComboBox != null) {
            // Must run after items are populated and skin is ready
            Platform.runLater(() -> {
                int index = "id".equals(settings.language()) ? 1 : 0;
                languageComboBox.getSelectionModel().select(index);
                languageComboBox.setValue(languageComboBox.getItems().get(index));
            });
        }
        
        if (installPathField != null) {
            String configuredPath = settings.installationPath().map(Path::toString).orElse("");
            if (!configuredPath.isEmpty()) {
                installPathField.setText(configuredPath);
                installPathText.setText("Path: " + configuredPath);
            }
        }
        if (downloadPathField != null) downloadPathField.setText(settings.temporaryDownloadPath().map(Path::toString).orElse("Default (inside install folder)"));
    }

    // --- Info and User Data Handlers ---

    private void showInfoDialog(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        if (actionButton != null && actionButton.getScene() != null) {
            alert.initOwner(actionButton.getScene().getWindow());
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Prevent text truncation in Alert dialogs
        alert.getDialogPane().setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        javafx.scene.Node contentNode = alert.getDialogPane().lookup(".content.label");
        if (contentNode instanceof javafx.scene.control.Label) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) contentNode;
            label.setWrapText(true);
            label.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        }
        
        // Apply styling class for Golden Ratio
        alert.getDialogPane().getStyleClass().add("root-pane");
        if (contentNode != null) {
            contentNode.getStyleClass().add("gr-body");
        }
        alert.showAndWait();
    }

    @FXML
    private void handleViewHistory(ActionEvent event) {
        if (historyManager == null) return;
        
        java.util.List<com.wol.updater.domain.UpdateRecord> records = historyManager.getHistory();
        
        Alert alert = new Alert(AlertType.INFORMATION);
        if (actionButton != null && actionButton.getScene() != null) {
            alert.initOwner(actionButton.getScene().getWindow());
        }
        alert.setTitle(resources.getString("history.title"));
        alert.setHeaderText("Update History");
        alert.setGraphic(null); // Remove the native 'i' icon
        
        VBox container = new VBox(15);
        container.setPadding(new Insets(10));
        
        // Base Version / Current Version from orchestrator
        if (orchestrator != null && orchestrator.getCurrentPlan() != null) {
            String currentVer = orchestrator.getCurrentPlan().currentVersion();
            Label baseLbl = new Label("Currently Detected Version: " + currentVer + " (Includes manual patches)");
            baseLbl.setStyle("-fx-font-family: 'Poppins', sans-serif; -fx-font-weight: bold; -fx-text-fill: -fx-theme-cta;");
            container.getChildren().add(baseLbl);
        }
        
        if (records.isEmpty()) {
            Label emptyLbl = new Label(resources.getString("history.empty"));
            emptyLbl.setWrapText(true);
            container.getChildren().add(emptyLbl);
        } else {
            StringBuilder sb = new StringBuilder();
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (com.wol.updater.domain.UpdateRecord r : records) {
                sb.append("[").append(r.timestamp().format(dtf)).append("] ")
                  .append("Updated to Version ").append(r.targetVersion())
                  .append(" - ").append(r.status()).append("\n");
            }
            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.getStyleClass().add("history-text-area");
            javafx.scene.layout.VBox.setVgrow(textArea, javafx.scene.layout.Priority.ALWAYS);
            container.getChildren().add(textArea);
        }
        
        alert.getDialogPane().setContent(container);
        
        // Remove native OS borders if possible, style the DialogPane
        alert.getDialogPane().getStyleClass().addAll("root-pane", "history-dialog-pane");
        
        // Remove header text styling to let CSS take over or clear it
        javafx.scene.Node headerPanel = alert.getDialogPane().lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle("-fx-background-color: transparent;");
        }
        
        alert.getDialogPane().setPrefWidth(550);
        alert.getDialogPane().setPrefHeight(400);
        
        // Ensure scene inherits the root styles (Light/Dark mode)
        if (actionButton != null && actionButton.getScene() != null) {
            alert.getDialogPane().getStylesheets().addAll(actionButton.getScene().getStylesheets());
            boolean isDark = actionButton.getScene().getRoot().getStyleClass().contains("dark-mode");
            alert.getDialogPane().getStyleClass().add(isDark ? "dark-mode" : "light-mode");
        }
        
        alert.showAndWait();
    }

    @FXML
    private void showInstallPathInfo(javafx.scene.input.MouseEvent event) {
        showInfoDialog(resources.getString("info.install_path.title"), resources.getString("info.install_path.desc"));
    }

    @FXML
    private void showTempPathInfo(javafx.scene.input.MouseEvent event) {
        showInfoDialog(resources.getString("info.temp_path.title"), resources.getString("info.temp_path.desc"));
    }

    @FXML
    private void handleNotificationClick(javafx.scene.input.MouseEvent event) {
        if (notificationOverlay != null) {
            boolean isVisible = notificationOverlay.isVisible();
            notificationOverlay.setVisible(!isVisible);
            notificationOverlay.setManaged(!isVisible);
        }
    }

    @FXML
    private void handleClearNotifications(javafx.scene.input.MouseEvent event) {
        if (notificationList != null) {
            notificationList.getChildren().clear();
            VBox emptyState = new VBox(5);
            emptyState.getStyleClass().add("notification-item");
            Label msgLabel = new Label(resources.getString("notify.empty"));
            msgLabel.getStyleClass().add("notification-item-title");
            emptyState.getChildren().add(msgLabel);
            notificationList.getChildren().add(emptyState);
        }
        if (notificationBadge != null) {
            notificationBadge.setVisible(false);
        }
        if (notificationOverlay != null) {
            notificationOverlay.setVisible(false);
            notificationOverlay.setManaged(false);
        }
    }

    @FXML
    private void handleOpenUserData(ActionEvent event) {
        File dir = null;
        if (userDataPathField != null && userDataPathField.getText() != null && !userDataPathField.getText().trim().isEmpty()) {
            dir = new File(userDataPathField.getText());
        } else {
            dir = new File(System.getProperty("user.home"), "Documents/My Games/Wars of Liberty");
        }
        
        if (dir.exists()) {
            try {
                java.awt.Desktop.getDesktop().open(dir);
            } catch (Exception e) {
                showInfoDialog("Error", "Gagal membuka folder.");
            }
        } else {
            showInfoDialog("Not Found", "Folder User Data tidak ditemukan di path tersebut.");
        }
    }

    @FXML
    private void handleDetectUserData() {
        String userHome = System.getProperty("user.home");
        
        File[] possibleDirs = {
            new File(userHome, "Documents/My Games/Wars of Liberty"),
            new File(userHome, "OneDrive/Dokumen/My Games/Wars of Liberty"),
            new File(userHome, "OneDrive/Documents/My Games/Wars of Liberty"),
            new File(userHome, "Dokumen/My Games/Wars of Liberty")
        };
        
        File foundDir = null;
        for (File d : possibleDirs) {
            if (d.exists() && d.isDirectory()) {
                foundDir = d;
                break;
            }
        }
        
        if (foundDir != null) {
            if (userDataPathField != null) {
                userDataPathField.setText(foundDir.getAbsolutePath());
            }
            showInfoDialog("Detected", "Folder Wars of Liberty berhasil ditemukan di:\n" + foundDir.getAbsolutePath());
        } else {
            showInfoDialog("Not Found", "Folder Wars of Liberty tidak ditemukan secara otomatis di My Games.");
        }
    }

    @FXML
    private void handleBrowseUserData(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select User Data Folder");
        if (userDataPathField != null && !userDataPathField.getText().isEmpty()) {
            File current = new File(userDataPathField.getText());
            if (current.exists()) {
                dc.setInitialDirectory(current);
            }
        }
        File dir = dc.showDialog(navUserData.getScene().getWindow());
        if (dir != null && userDataPathField != null) {
            userDataPathField.setText(dir.getAbsolutePath());
        }
    }

    @FXML
    private void handleBackupUserData(ActionEvent event) {
        String currentPath = userDataPathField.getText();
        if (currentPath == null || currentPath.isEmpty()) {
            showInfoDialog(resources.getString("validation.error"), "Please detect or select your User Data folder first.");
            return;
        }
        File sourceDir = new File(currentPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            showInfoDialog(resources.getString("validation.error"), "The selected User Data folder does not exist.");
            return;
        }

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle(resources.getString("userdata.backup.title"));
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("ZIP Archive", "*.zip"));
        fc.setInitialFileName("WoL_UserData_Backup_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip");
        File destFile = fc.showSaveDialog(navUserData.getScene().getWindow());
        if (destFile == null) return;

        statusTitle.setText("Creating Backup...");
        statusSubtitle.setText("Zipping " + sourceDir.getName() + "...");
        
        javafx.concurrent.Task<Void> backupTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(destFile))) {
                    java.nio.file.Path sourcePath = sourceDir.toPath();
                    java.nio.file.Files.walk(sourcePath)
                        .filter(path -> !java.nio.file.Files.isDirectory(path))
                        .forEach(path -> {
                            try {
                                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(sourcePath.relativize(path).toString().replace("\\", "/"));
                                zos.putNextEntry(zipEntry);
                                java.nio.file.Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                }
                return null;
            }
        };

        backupTask.setOnSucceeded(e -> {
            statusTitle.setText(resources.getString("app.title"));
            statusSubtitle.setText("Backup completed successfully.");
            showInfoDialog(resources.getString("notify.title"), "Backup saved successfully to:\n" + destFile.getAbsolutePath());
        });

        backupTask.setOnFailed(e -> {
            statusTitle.setText(resources.getString("app.title"));
            statusSubtitle.setText("Backup failed.");
            showInfoDialog(resources.getString("validation.error"), "Failed to create backup: " + backupTask.getException().getMessage());
        });

        new Thread(backupTask).start();
    }

    @FXML
    private void handleRestoreUserData(ActionEvent event) {
        String currentPath = userDataPathField.getText();
        if (currentPath == null || currentPath.isEmpty()) {
            showInfoDialog(resources.getString("validation.error"), "Please detect or select your User Data folder first.");
            return;
        }
        File targetDir = new File(currentPath);

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle(resources.getString("userdata.restore.title"));
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("ZIP Archive", "*.zip"));
        File zipFile = fc.showOpenDialog(navUserData.getScene().getWindow());
        if (zipFile == null) return;

        statusTitle.setText("Restoring Backup...");
        statusSubtitle.setText("Extracting files...");

        javafx.concurrent.Task<Void> restoreTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile))) {
                    java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
                    while (zipEntry != null) {
                        File newFile = new File(targetDir, zipEntry.getName());
                        String destDirPath = targetDir.getCanonicalPath();
                        String destFilePath = newFile.getCanonicalPath();
                        if (!destFilePath.startsWith(destDirPath + java.io.File.separator)) {
                            throw new java.io.IOException("Entry is outside of the target dir: " + zipEntry.getName());
                        }
                        if (zipEntry.isDirectory()) {
                            if (!newFile.isDirectory() && !newFile.mkdirs()) {
                                throw new java.io.IOException("Failed to create directory " + newFile);
                            }
                        } else {
                            File parent = newFile.getParentFile();
                            if (!parent.isDirectory() && !parent.mkdirs()) {
                                throw new java.io.IOException("Failed to create directory " + parent);
                            }
                            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
                                zis.transferTo(fos);
                            }
                        }
                        zipEntry = zis.getNextEntry();
                    }
                }
                return null;
            }
        };

        restoreTask.setOnSucceeded(e -> {
            statusTitle.setText(resources.getString("app.title"));
            statusSubtitle.setText("Restore completed successfully.");
            showInfoDialog(resources.getString("notify.title"), "Backup restored successfully to:\n" + targetDir.getAbsolutePath());
        });

        restoreTask.setOnFailed(e -> {
            statusTitle.setText(resources.getString("app.title"));
            statusSubtitle.setText("Restore failed.");
            showInfoDialog(resources.getString("validation.error"), "Failed to restore backup: " + restoreTask.getException().getMessage());
        });

        new Thread(restoreTask).start();
    }

    // --- StatefulObserver Implementation ---

    @Override
    public void onStateChanged(UpdaterState newState, String message) {
        Platform.runLater(() -> {
            statusSubtitle.setText(message);
            updateUIForState(newState);
        });
    }

    @Override
    public void onInstallationStatusChanged(InstallationStatus status) {
        Platform.runLater(() -> {
            String path = status.installationPath().map(Path::toString).orElse("Not configured");
            installPathText.setText("Path: " + path);
            
            if (installPathField != null) {
                boolean isAutoDetected = orchestrator.getCurrentSettings().installationPath().isEmpty();
                installPathField.setText(path + (isAutoDetected && status.installationPath().isPresent() ? " (Auto-detected)" : ""));
            }
        });
    }

    @Override
    public void onUpdatePlanReady(UpdatePlan plan) {
        Platform.runLater(() -> {
            installedVersionText.setText("Installed Version: " + plan.currentVersion());
            latestVersionText.setText("Latest Version: " + plan.targetVersion());
            installedVersionText.setVisible(true);
            installedVersionText.setManaged(true);
            latestVersionText.setVisible(true);
            latestVersionText.setManaged(true);
        });
    }

    @FXML
    private void handleClearTemp(ActionEvent event) {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "wol_updater_temp");
        try {
            com.wol.updater.infrastructure.FileUtils.deleteDirectory(tempDir);
            statusSubtitle.setText("Temporary files cleared successfully.");
        } catch (Exception e) {
            statusSubtitle.setText("Failed to clear temporary files.");
        }
    }

    @Override
    public void onProgressUpdated(int currentPackage, int totalPackages, long bytesDownloaded, long totalBytes) {
        Platform.runLater(() -> {
            UpdaterState state = orchestrator != null ? orchestrator.getCurrentState() : UpdaterState.DOWNLOADING;
            String action = state == UpdaterState.INSTALLING ? "Extracting" : "Downloading";
            if (totalBytes > 0) {
                double pct = (double) bytesDownloaded / totalBytes;
                progressBar.setProgress(pct);
                progressText.setText(String.format("%s Package %d of %d (%.0f%%)", action, currentPackage, totalPackages, pct * 100));
                progressDetails.setText(String.format("%.2f / %.2f MB", bytesDownloaded / 1048576.0, totalBytes / 1048576.0));
            } else {
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                progressText.setText(String.format("%s Package %d of %d", action, currentPackage, totalPackages));
                progressDetails.setText("");
            }
        });
    }

    @Override
    public void onError(String message, Exception e) {
        Platform.runLater(() -> {
            statusTitle.setText("ERROR");
            statusSubtitle.setText(message + (e != null ? ": " + e.getMessage() : ""));
        });
    }

    private void updateUIForState(UpdaterState state) {
        browseButton.setVisible(false);
        browseButton.setManaged(false);
        if (pauseButton != null) {
            pauseButton.setVisible(false);
            pauseButton.setManaged(false);
            resumeButton.setVisible(false);
            resumeButton.setManaged(false);
            abortButton.setVisible(false);
            abortButton.setManaged(false);
        }
        actionButton.setVisible(false);
        actionButton.setManaged(false);

        boolean isUpdating = (state == UpdaterState.DOWNLOADING || state == UpdaterState.INSTALLING || state == UpdaterState.PAUSED);
        navUpdates.setVisible(isUpdating);
        navUpdates.setManaged(isUpdating);

        if (state == UpdaterState.UP_TO_DATE || state == UpdaterState.NO_INSTALLATION || state == UpdaterState.INVALID_INSTALLATION || state == UpdaterState.CORRUPTED_INSTALLATION || state == UpdaterState.INCOMPLETE_INSTALLATION) {
            if (progressContainer != null) {
                progressContainer.setVisible(false);
                progressContainer.setManaged(false);
            }
            if (updateProgressTitle != null) {
                if (state == UpdaterState.UP_TO_DATE) {
                     updateProgressTitle.setText(resources.getString("progress.title.uptodate"));
                } else {
                     updateProgressTitle.setText(resources.getString("progress.title.no_update"));
                }
            }
        } else {
            if (progressContainer != null) {
                progressContainer.setVisible(true);
                progressContainer.setManaged(true);
            }
            if (updateProgressTitle != null) {
                updateProgressTitle.setText("Update Progress");
            }
        }

        switch (state) {
            case NO_INSTALLATION:
            case INVALID_INSTALLATION:
            case INCOMPLETE_INSTALLATION:
                statusTitle.setText(resources.getString("status.error"));
                browseButton.setVisible(true);
                browseButton.setManaged(true);
                showHome();
                break;
            case CHECKING:
            case VERIFYING:
                statusTitle.setText(resources.getString("status.checking"));
                break;
            case CORRUPTED_INSTALLATION:
                statusTitle.setText(resources.getString("status.error"));
                browseButton.setVisible(true);
                browseButton.setManaged(true);
                showHome();
                break;
            case UP_TO_DATE:
                statusTitle.setText(resources.getString("status.uptodate"));
                actionButton.setText(resources.getString("btn.launch"));
                actionButton.setVisible(true);
                actionButton.setManaged(true);
                showHome();
                break;
            case UPDATE_AVAILABLE:
                statusTitle.setText(resources.getString("status.update_available"));
                actionButton.setText(resources.getString("btn.update"));
                actionButton.setVisible(true);
                actionButton.setManaged(true);
                showHome();
                break;
            case DOWNLOADING:
            case INSTALLING:
                statusTitle.setText(state == UpdaterState.DOWNLOADING ? resources.getString("status.downloading") : resources.getString("status.installing"));
                if (updateProgressTitle != null) {
                    updateProgressTitle.setText(state == UpdaterState.DOWNLOADING ? resources.getString("progress.title.downloading") : resources.getString("progress.title.installing"));
                }
                if (pauseButton != null) {
                    pauseButton.setVisible(true);
                    pauseButton.setManaged(true);
                    abortButton.setVisible(true);
                    abortButton.setManaged(true);
                }
                showUpdates();
                break;
            case PAUSED:
                statusTitle.setText("UPDATE PAUSED");
                if (updateProgressTitle != null) {
                    updateProgressTitle.setText("Update Paused");
                }
                if (pauseButton != null) {
                    resumeButton.setVisible(true);
                    resumeButton.setManaged(true);
                    abortButton.setVisible(true);
                    abortButton.setManaged(true);
                }
                showUpdates();
                break;
            case COMPLETED:
                statusTitle.setText(resources.getString("status.uptodate"));
                actionButton.setText(resources.getString("btn.launch"));
                actionButton.setVisible(true);
                actionButton.setManaged(true);
                showHome();
                break;
            case CANCELLED:
                statusTitle.setText("UPDATE CANCELLED");
                showHome();
                break;
            case ERROR:
                statusTitle.setText(resources.getString("status.error"));
                showHome();
                break;
        }
    }
    
    @FXML
    private void handleCloseApp() {
        javafx.application.Platform.exit();
    }
    
    @FXML
    private void handleMinimizeApp() {
        if (actionButton != null && actionButton.getScene() != null && actionButton.getScene().getWindow() != null) {
            ((javafx.stage.Stage) actionButton.getScene().getWindow()).setIconified(true);
        }
    }
    
    @FXML
    private void handleMaximizeApp() {
        if (actionButton != null && actionButton.getScene() != null && actionButton.getScene().getWindow() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) actionButton.getScene().getWindow();
            stage.setFullScreen(!stage.isFullScreen());
        }
    }
    
    @FXML
    private void handleToggleTheme() {
        if (actionButton != null && actionButton.getScene() != null) {
            javafx.scene.Parent root = actionButton.getScene().getRoot();
            if (root.getStyleClass().contains("light-mode")) {
                root.getStyleClass().remove("light-mode");
                root.getStyleClass().add("dark-mode");
                if (themeIconPath != null) {
                    // Sun Icon for Dark Mode (click to go to light mode)
                    themeIconPath.setContent("M12 3v2m0 14v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M21 12h-2M5 12H3m16.07-7.07l-1.41 1.41M6.34 17.66l-1.41 1.41M12 7a5 5 0 1 1 0 10 5 5 0 0 1 0-10z");
                }
            } else {
                root.getStyleClass().remove("dark-mode");
                root.getStyleClass().add("light-mode");
                if (themeIconPath != null) {
                    // Moon Icon for Light Mode (click to go to dark mode)
                    themeIconPath.setContent("M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z");
                }
            }
        }
    }
    
    @FXML
    private void handleTitleBarPressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }
    
    @FXML
    private void handleTitleBarDragged(javafx.scene.input.MouseEvent event) {
        if (actionButton != null && actionButton.getScene() != null && actionButton.getScene().getWindow() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) actionButton.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        }
    }
    
    @FXML
    private void handleToggleSidebar() {
        if (sidebar == null) return;
        
        sidebarExpanded = !sidebarExpanded;
        
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        double targetWidth = sidebarExpanded ? 220 : 60;
        double targetOpacity = sidebarExpanded ? 1 : 0;
        
        javafx.animation.KeyValue kvWidth = new javafx.animation.KeyValue(sidebar.prefWidthProperty(), targetWidth, javafx.animation.Interpolator.EASE_BOTH);
        javafx.animation.KeyValue kvTextOpacity = new javafx.animation.KeyValue(brandText.opacityProperty(), targetOpacity, javafx.animation.Interpolator.EASE_BOTH);
        
        timeline.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), kvWidth, kvTextOpacity));
        timeline.play();
        
        // Hide/Show page titles in the content views
        if (pageTitleHome != null) { pageTitleHome.setVisible(!sidebarExpanded); pageTitleHome.setManaged(!sidebarExpanded); }
        if (pageTitleSettings != null) { pageTitleSettings.setVisible(!sidebarExpanded); pageTitleSettings.setManaged(!sidebarExpanded); }
        if (pageTitleUserData != null) { pageTitleUserData.setVisible(!sidebarExpanded); pageTitleUserData.setManaged(!sidebarExpanded); }
        if (pageTitleAbout != null) { pageTitleAbout.setVisible(!sidebarExpanded); pageTitleAbout.setManaged(!sidebarExpanded); }
    }
}
