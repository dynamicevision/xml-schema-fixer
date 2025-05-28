package com.xmlfixer.app.gui;

import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.app.core.config.ApplicationComponent;
import com.xmlfixer.app.core.config.DaggerApplicationComponent;
import com.xmlfixer.app.gui.controllers.MainController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Properties;

/**
 * Main JavaFX GUI application entry point
 */
public class GuiApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(GuiApplication.class);
    
    @Inject
    ApplicationOrchestrator orchestrator;
    
    @Inject
    Properties properties;
    
    private ApplicationComponent component;
    
    public static void main(String[] args) {
        logger.info("Starting GUI application");
        launch(args);
    }
    
    @Override
    public void init() throws Exception {
        super.init();
        
        // Initialize DI container
        component = DaggerApplicationComponent.factory().create();
        component.inject(this);
        
        logger.info("GUI application initialized with dependency injection");
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load main FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            
            // Create and inject controller
            MainController mainController = new MainController();
            mainController.setOrchestrator(orchestrator);
            mainController.setProperties(properties);
            loader.setController(mainController);
            
            Parent root = loader.load();
            
            // Configure primary stage
            primaryStage.setTitle(getApplicationTitle());
            primaryStage.setScene(new Scene(root, getWindowWidth(), getWindowHeight()));
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Set application icon if available
            // primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app-icon.png")));
            
            // Show the stage
            primaryStage.show();
            
            logger.info("GUI application started successfully");
            
        } catch (IOException e) {
            logger.error("Failed to load main FXML", e);
            throw new RuntimeException("Failed to start GUI application", e);
        }
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        logger.info("GUI application stopped");
    }
    
    private String getApplicationTitle() {
        if (properties != null) {
            String name = properties.getProperty("app.name", "XML Schema Fixer");
            String version = properties.getProperty("app.version", "1.0.0");
            return name + " v" + version;
        }
        return "XML Schema Fixer";
    }
    
    private double getWindowWidth() {
        if (properties != null) {
            return Double.parseDouble(properties.getProperty("gui.window.width", "1200"));
        }
        return 1200;
    }
    
    private double getWindowHeight() {
        if (properties != null) {
            return Double.parseDouble(properties.getProperty("gui.window.height", "800"));
        }
        return 800;
    }
}

