package com.wol.updater.presentation;

import com.wol.updater.application.*;
import com.wol.updater.infrastructure.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.ResourceBundle;

public class JavaFXLauncherApp extends Application {

    private StatefulOrchestrator orchestrator;

    @Override
    public void init() throws Exception {
        // Wire up dependencies manually as per Clean Architecture without DI framework
        InstallationLocator locator = new CompositeInstallationLocator();
        InstallationValidator validator = new StrictInstallationValidator();
        VersionDetector detector = new HashingVersionDetector();
        UpdateSource source = new XmlUpdateSource();
        FileDownloader downloader = new HttpDownloader();
        UpdateInstaller installer = new CommonsCompressInstaller();
        SettingsManager settingsManager = new PropertiesSettingsManager();
        com.wol.updater.domain.UserSettings settings = settingsManager.loadSettings();
        
        java.nio.file.Path dataDir = java.nio.file.Paths.get(System.getProperty("user.home"), ".wol-updater");
        HistoryManager historyManager = new FileHistoryManager(dataDir);

        Locale locale = "id".equals(settings.language()) ? new Locale("id", "ID") : Locale.US;
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.Messages", locale);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/wol/updater/presentation/updater.fxml"), bundle);
        Parent root = loader.load();
        
        UpdaterController controller = loader.getController();
        controller.setHistoryManager(historyManager);
        
        orchestrator = new StatefulOrchestrator(
            locator, validator, detector, source, downloader, installer, settingsManager, historyManager, controller
        );
        
        controller.setOrchestrator(orchestrator);
        
        // Store root in a thread-local or static for start(), or just a simple hack for demo
        rootPane = root;
    }
    
    private Parent rootPane;



    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Wars of Liberty Updater");
        primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/wol.png")));
        
        // Remove native window borders
        primaryStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        
        Scene scene = new Scene(rootPane, 900, 600);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        // Window dragging is handled in UpdaterController for the title bar

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Start the orchestrator logic
        orchestrator.initialize();
    }
}
