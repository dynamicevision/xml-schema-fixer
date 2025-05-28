package com.xmlfixer.app.gui.controllers;

import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.correction.model.CorrectionResult;
import com.xmlfixer.validation.model.ValidationResult;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Main GUI controller handling user interactions
 */
public class MainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    private ApplicationOrchestrator orchestrator;
    private Properties properties;
    
    // FXML Controls
    @FXML private TextField xmlFileTextField;
    @FXML private TextField schemaFileTextField;
    @FXML private TextField outputFileTextField;
    
    @FXML private Button browseXmlButton;
    @FXML private Button browseSchemaButton;
    @FXML private Button browseOutputButton;
    
    @FXML private Button validateButton;
    @FXML private Button fixButton;
    @FXML private Button batchProcessButton;
    
    @FXML private TextArea logTextArea;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    
    @FXML private CheckBox createBackupCheckBox;
    @FXML private CheckBox verboseLoggingCheckBox;
    @FXML private CheckBox generateReportCheckBox;
    
    @FXML private TabPane mainTabPane;
    @FXML private Tab validationTab;
    @FXML private Tab correctionTab;
    @FXML private Tab batchTab;
    @FXML private Tab settingsTab;
    
    public void setOrchestrator(ApplicationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
    
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing main controller");
        
        // Initialize UI components
        setupFileChoosers();
        setupProgressIndicators();
        setupLogging();
        
        // Set default values
        createBackupCheckBox.setSelected(true);
        generateReportCheckBox.setSelected(true);
        
        updateStatus("Ready");
        
        logger.info("Main controller initialized");
    }
    
    private void setupFileChoosers() {
        // File chooser configurations will be set up here
        browseXmlButton.setOnAction(this::browseXmlFile);
        browseSchemaButton.setOnAction(this::browseSchemaFile);
        browseOutputButton.setOnAction(this::browseOutputFile);
    }
    
    private void setupProgressIndicators() {
        progressBar.setVisible(false);
        progressBar.setProgress(0);
    }
    
    private void setupLogging() {
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
    }
    
    @FXML
    private void browseXmlFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XML File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            xmlFileTextField.setText(selectedFile.getAbsolutePath());
            logMessage("Selected XML file: " + selectedFile.getName());
        }
    }
    
    @FXML
    private void browseSchemaFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XSD Schema File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XSD Files", "*.xsd")
        );
        
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            schemaFileTextField.setText(selectedFile.getAbsolutePath());
            logMessage("Selected schema file: " + selectedFile.getName());
        }
    }
    
    @FXML
    private void browseOutputFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Output File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("XML Files", "*.xml")
        );
        
        File selectedFile = fileChooser.showSaveDialog(getStage());
        if (selectedFile != null) {
            outputFileTextField.setText(selectedFile.getAbsolutePath());
            logMessage("Selected output file: " + selectedFile.getName());
        }
    }
    
    @FXML
    private void validateXml(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }
        
        File xmlFile = new File(xmlFileTextField.getText());
        File schemaFile = new File(schemaFileTextField.getText());
        
        // Create background task for validation
        Task<ValidationResult> validationTask = new Task<ValidationResult>() {
            @Override
            protected ValidationResult call() throws Exception {
                updateMessage("Validating XML file...");
                return orchestrator.validateXml(xmlFile, schemaFile);
            }
            
            @Override
            protected void succeeded() {
                ValidationResult result = getValue();
                Platform.runLater(() -> {
                    handleValidationResult(result);
                    hideProgress();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    handleError("Validation failed", exception);
                    hideProgress();
                });
            }
        };
        
        // Bind progress and status
        showProgress();
        progressBar.progressProperty().bind(validationTask.progressProperty());
        statusLabel.textProperty().bind(validationTask.messageProperty());
        
        // Execute task
        Thread validationThread = new Thread(validationTask);
        validationThread.setDaemon(true);
        validationThread.start();
        
        logMessage("Started XML validation...");
    }
    
    @FXML
    private void fixXml(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }
        
        File xmlFile = new File(xmlFileTextField.getText());
        File schemaFile = new File(schemaFileTextField.getText());
        File outputFile = getOutputFile();
        
        // Create background task for correction
        Task<CorrectionResult> correctionTask = new Task<CorrectionResult>() {
            @Override
            protected CorrectionResult call() throws Exception {
                updateMessage("Fixing XML file...");
                return orchestrator.fixXml(xmlFile, schemaFile, outputFile);
            }
            
            @Override
            protected void succeeded() {
                CorrectionResult result = getValue();
                Platform.runLater(() -> {
                    handleCorrectionResult(result);
                    hideProgress();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    handleError("Correction failed", exception);
                    hideProgress();
                });
            }
        };
        
        // Bind progress and status
        showProgress();
        progressBar.progressProperty().bind(correctionTask.progressProperty());
        statusLabel.textProperty().bind(correctionTask.messageProperty());
        
        // Execute task
        Thread correctionThread = new Thread(correctionTask);
        correctionThread.setDaemon(true);
        correctionThread.start();
        
        logMessage("Started XML correction...");
    }
    
    @FXML
    private void batchProcess(ActionEvent event) {
        // TODO: Implement batch processing UI
        logMessage("Batch processing feature will be implemented");
        showInfo("Feature Coming Soon", "Batch processing will be available in the next version.");
    }
    
    private boolean validateInputs() {
        if (xmlFileTextField.getText().trim().isEmpty()) {
            showError("Input Error", "Please select an XML file.");
            return false;
        }
        
        if (schemaFileTextField.getText().trim().isEmpty()) {
            showError("Input Error", "Please select a schema file.");
            return false;
        }
        
        File xmlFile = new File(xmlFileTextField.getText());
        File schemaFile = new File(schemaFileTextField.getText());
        
        if (!xmlFile.exists()) {
            showError("File Error", "XML file does not exist: " + xmlFile.getAbsolutePath());
            return false;
        }
        
        if (!schemaFile.exists()) {
            showError("File Error", "Schema file does not exist: " + schemaFile.getAbsolutePath());
            return false;
        }
        
        return true;
    }
    
    private File getOutputFile() {
        if (!outputFileTextField.getText().trim().isEmpty()) {
            return new File(outputFileTextField.getText());
        }
        
        // Generate default output file name
        File xmlFile = new File(xmlFileTextField.getText());
        String baseName = xmlFile.getName();
        String extension = "";
        int lastDot = baseName.lastIndexOf('.');
        
        if (lastDot > 0) {
            extension = baseName.substring(lastDot);
            baseName = baseName.substring(0, lastDot);
        }
        
        return new File(xmlFile.getParent(), baseName + ".fixed" + extension);
    }
    
    private void handleValidationResult(ValidationResult result) {
        if (result.isValid()) {
            logMessage("✓ XML file is valid!");
            showInfo("Validation Success", "The XML file is valid according to the schema.");
        } else {
            logMessage("✗ XML file has validation errors: " + result.getErrors().size());
            showWarning("Validation Failed", 
                "The XML file has " + result.getErrors().size() + " validation errors.");
        }
        
        updateStatus("Validation completed");
    }
    
    private void handleCorrectionResult(CorrectionResult result) {
        if (result.isSuccess()) {
            if (result.isNoChangesRequired()) {
                logMessage("✓ XML file was already valid - no changes needed");
                showInfo("Correction Complete", "The XML file was already valid.");
            } else {
                logMessage("✓ XML file corrected successfully with " + 
                    result.getActionsApplied().size() + " fixes");
                showInfo("Correction Success", 
                    "XML file corrected successfully. " + result.getActionsApplied().size() + 
                    " corrections were applied.");
            }
        } else {
            logMessage("✗ Correction failed: " + result.getErrorMessage());
            showError("Correction Failed", 
                "Failed to correct the XML file: " + result.getErrorMessage());
        }
        
        updateStatus("Correction completed");
    }
    
    private void handleError(String title, Throwable exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Unknown error";
        logMessage("Error: " + message);
        showError(title, message);
        updateStatus("Error occurred");
        logger.error(title, exception);
    }
    
    @FXML
    private void clearLog(ActionEvent event) {
        logTextArea.clear();
        logMessage("Log cleared");
    }
    
    private void showProgress() {
        progressBar.setVisible(true);
        validateButton.setDisable(true);
        fixButton.setDisable(true);
        batchProcessButton.setDisable(true);
    }
    
    private void hideProgress() {
        progressBar.setVisible(false);
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        
        validateButton.setDisable(false);
        fixButton.setDisable(false);
        batchProcessButton.setDisable(false);
    }
    
    private void logMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            logTextArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }
    
    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private Stage getStage() {
        return (Stage) xmlFileTextField.getScene().getWindow();
    }
}

