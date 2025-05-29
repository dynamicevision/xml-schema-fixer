package com.xmlfixer.app.core;

import com.xmlfixer.common.exceptions.XmlFixerException;
import com.xmlfixer.correction.CorrectionEngine;
import com.xmlfixer.correction.model.CorrectionResult;
import com.xmlfixer.parsing.XmlParser;
import com.xmlfixer.reporting.ReportGenerator;
import com.xmlfixer.reporting.model.Report;
import com.xmlfixer.schema.SchemaAnalyzer;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.XmlValidator;
import com.xmlfixer.validation.model.ValidationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced orchestrator that coordinates all application components with intelligent correction capabilities.
 * This class provides the high-level API for both GUI and CLI applications.
 */
@Singleton
public class ApplicationOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationOrchestrator.class);

    private final SchemaAnalyzer schemaAnalyzer;
    private final XmlValidator xmlValidator;
    private final CorrectionEngine correctionEngine;
    private final XmlParser xmlParser;
    private final ReportGenerator reportGenerator;
    private final Properties properties;

    @Inject
    public ApplicationOrchestrator(
            SchemaAnalyzer schemaAnalyzer,
            XmlValidator xmlValidator,
            CorrectionEngine correctionEngine,
            XmlParser xmlParser,
            ReportGenerator reportGenerator,
            Properties properties) {

        this.schemaAnalyzer = schemaAnalyzer;
        this.xmlValidator = xmlValidator;
        this.correctionEngine = correctionEngine;
        this.xmlParser = xmlParser;
        this.reportGenerator = reportGenerator;
        this.properties = properties;

        logger.info("ApplicationOrchestrator initialized with enhanced correction capabilities");
    }

    /**
     * Validates an XML file against a schema with comprehensive analysis
     */
    public CompletableFuture<ValidationResult> validateXmlAsync(File xmlFile, File schemaFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting validation of {} against {}",
                        xmlFile.getName(), schemaFile.getName());

                // Perform validation using the enhanced validator
                ValidationResult result = xmlValidator.validate(xmlFile, schemaFile);

                logger.info("Validation completed with {} errors, {} warnings",
                        result.getErrorCount(), result.getWarningCount());
                return result;

            } catch (Exception e) {
                logger.error("Validation failed", e);
                throw new XmlFixerException("Validation failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Validates XML synchronously
     */
    public ValidationResult validateXml(File xmlFile, File schemaFile) {
        try {
            return validateXmlAsync(xmlFile, schemaFile).get();
        } catch (Exception e) {
            throw new XmlFixerException("Validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fixes an XML file using intelligent correction strategies
     */
    public CompletableFuture<CorrectionResult> fixXmlAsync(
            File xmlFile, File schemaFile, File outputFile) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting intelligent correction of {} using schema {}",
                        xmlFile.getName(), schemaFile.getName());

                // Step 1: Analyze schema structure
                SchemaElement rootSchema = schemaAnalyzer.analyzeSchema(schemaFile);
                logger.debug("Schema analysis completed for: {}", schemaFile.getName());

                // Step 2: Validate XML to identify errors
                ValidationResult validationResult = xmlValidator.validate(xmlFile, schemaFile);
                logger.debug("Validation found {} errors to correct", validationResult.getErrorCount());

                // Step 3: Apply intelligent corrections
                CorrectionResult correctionResult = correctionEngine.correct(
                        xmlFile, schemaFile, outputFile, validationResult, rootSchema);

                // Step 4: Post-correction validation
                if (correctionResult.isSuccess() && !correctionResult.isNoChangesRequired()) {
                    ValidationResult postCorrectionValidation = xmlValidator.validate(outputFile, schemaFile);
                    correctionResult.setAfterValidation(postCorrectionValidation);

                    logger.info("Post-correction validation: {} errors remaining",
                            postCorrectionValidation.getErrorCount());
                }

                logger.info("Correction completed for: {} ({}ms). Applied: {}, Failed: {}",
                        xmlFile.getName(), correctionResult.getCorrectionTimeMs(),
                        correctionResult.getAppliedActionCount(), correctionResult.getFailedActionCount());

                return correctionResult;

            } catch (Exception e) {
                logger.error("Correction failed", e);
                throw new XmlFixerException("Correction failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Fixes XML synchronously
     */
    public CorrectionResult fixXml(File xmlFile, File schemaFile, File outputFile) {
        try {
            return fixXmlAsync(xmlFile, schemaFile, outputFile).get();
        } catch (Exception e) {
            throw new XmlFixerException("Correction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Performs a complete validation and correction cycle with detailed reporting
     */
    public CompletableFuture<ProcessingResult> processXmlComplete(
            File xmlFile, File schemaFile, File outputFile, ProcessingOptions options) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting complete XML processing cycle for: {}", xmlFile.getName());

                ProcessingResult result = new ProcessingResult();
                result.setInputFile(xmlFile);
                result.setSchemaFile(schemaFile);
                result.setOutputFile(outputFile);

                long startTime = System.currentTimeMillis();

                // Step 1: Schema Analysis
                if (options.isAnalyzeSchema()) {
                    logger.debug("Analyzing schema structure");
                    SchemaElement rootSchema = schemaAnalyzer.analyzeSchema(schemaFile);
                    result.setSchemaAnalysis(rootSchema);
                }

                // Step 2: Initial Validation
                logger.debug("Performing initial validation");
                ValidationResult initialValidation = xmlValidator.validate(xmlFile, schemaFile);
                result.setInitialValidation(initialValidation);

                // Step 3: Correction (if needed and requested)
                if (!initialValidation.isValid() && options.isApplyCorrections()) {
                    logger.debug("Applying corrections");
                    CorrectionResult correctionResult = correctionEngine.correct(
                            xmlFile, schemaFile, outputFile, initialValidation, result.getSchemaAnalysis());
                    result.setCorrectionResult(correctionResult);

                    // Step 4: Post-correction validation
                    if (correctionResult.isSuccess() && !correctionResult.isNoChangesRequired()) {
                        ValidationResult finalValidation = xmlValidator.validate(outputFile, schemaFile);
                        result.setFinalValidation(finalValidation);
                    }
                }

                // Step 5: Generate comprehensive report
                if (options.isGenerateReport()) {
                    logger.debug("Generating comprehensive report");
                    Report report = reportGenerator.generateReport(
                            result.getInitialValidation(), result.getCorrectionResult());
                    result.setReport(report);
                }

                long endTime = System.currentTimeMillis();
                result.setTotalProcessingTimeMs(endTime - startTime);

                logger.info("Complete processing finished for: {} in {}ms",
                        xmlFile.getName(), result.getTotalProcessingTimeMs());

                return result;

            } catch (Exception e) {
                logger.error("Complete processing failed", e);
                throw new XmlFixerException("Processing failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Generates a comprehensive report for validation and correction results
     */
    public Report generateReport(ValidationResult validationResult, CorrectionResult correctionResult) {
        logger.info("Generating comprehensive report");

        Report report = reportGenerator.generateReport(validationResult, correctionResult);

        // Add processing statistics
        if (correctionResult != null) {
            CorrectionEngine.CorrectionStatistics stats = correctionEngine.getCorrectionStatistics(correctionResult);
            // Add statistics to report (would need to extend Report model)
            logger.debug("Correction statistics: {}", stats);
        }

        return report;
    }

    /**
     * Analyzes a schema file and returns its structure with detailed information
     */
    public SchemaElement analyzeSchema(File schemaFile) {
        logger.info("Analyzing schema: {}", schemaFile.getName());
        return schemaAnalyzer.analyzeSchema(schemaFile);
    }

    /**
     * Gets detailed information about supported correction capabilities
     */
    public CorrectionCapabilities getCorrectionCapabilities() {
        CorrectionCapabilities capabilities = new CorrectionCapabilities();
        capabilities.setSupportedErrorTypes(correctionEngine.getSupportedErrorTypes());
        capabilities.setStrategiesAvailable(correctionEngine.getSupportedErrorTypes().size());

        // Add capability descriptions
        capabilities.addCapability("Missing Elements", "Automatically adds required elements with default values");
        capabilities.addCapability("Element Ordering", "Reorders elements to match schema sequence requirements");
        capabilities.addCapability("Cardinality Fixes", "Adds missing or removes excess element occurrences");
        capabilities.addCapability("Data Type Correction", "Converts and validates data types according to schema");
        capabilities.addCapability("Attribute Correction", "Adds missing attributes and fixes invalid values");
        capabilities.addCapability("Content Validation", "Fixes empty required content and content model issues");

        return capabilities;
    }

    /**
     * Validates if a correction can be applied to specific XML
     */
    public boolean canApplyCorrection(File xmlFile, File schemaFile, ValidationResult validationResult) {
        try {
            if (validationResult == null || validationResult.isValid()) {
                return false; // No corrections needed
            }

            // Check if we have strategies for the error types found
            return validationResult.getErrors().stream()
                    .anyMatch(error -> correctionEngine.getSupportedErrorTypes().contains(error.getErrorType()));

        } catch (Exception e) {
            logger.warn("Error checking correction applicability", e);
            return false;
        }
    }

    /**
     * Batch processes multiple XML files with progress tracking
     */
    public CompletableFuture<BatchProcessingResult> processBatchAsync(
            File[] xmlFiles, File schemaFile, File outputDirectory) {

        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting batch processing of {} files", xmlFiles.length);

            BatchProcessingResult result = new BatchProcessingResult();
            result.setTotalFiles(xmlFiles.length);
            result.setStartTime(System.currentTimeMillis());

            int processedCount = 0;
            int successfulCount = 0;
            int failedCount = 0;

            // Process each file
            for (File xmlFile : xmlFiles) {
                try {
                    logger.debug("Processing batch file: {}", xmlFile.getName());

                    // Generate output file name
                    File outputFile = new File(outputDirectory,
                            xmlFile.getName().replaceAll("\\.[^.]+$", ".fixed.xml"));

                    // Process the file
                    CorrectionResult correctionResult = fixXml(xmlFile, schemaFile, outputFile);

                    if (correctionResult.isSuccess()) {
                        successfulCount++;
                        logger.debug("Successfully processed: {}", xmlFile.getName());
                    } else {
                        failedCount++;
                        logger.warn("Failed to process: {} - {}",
                                xmlFile.getName(), correctionResult.getErrorMessage());
                    }

                    processedCount++;

                } catch (Exception e) {
                    failedCount++;
                    processedCount++;
                    logger.error("Error processing batch file: {}", xmlFile.getName(), e);
                }
            }

            result.setProcessedFiles(processedCount);
            result.setSuccessfulFiles(successfulCount);
            result.setFailedFiles(failedCount);
            result.setEndTime(System.currentTimeMillis());

            logger.info("Batch processing completed: {}/{} successful",
                    successfulCount, xmlFiles.length);

            return result;
        });
    }

    /**
     * Gets processing statistics and performance metrics
     */
    public ProcessingStatistics getProcessingStatistics() {
        ProcessingStatistics stats = new ProcessingStatistics();

        // Would typically collect these from internal metrics
        stats.setTotalValidationsPerformed(0);
        stats.setTotalCorrectionsApplied(0);
        stats.setAverageValidationTimeMs(0);
        stats.setAverageCorrectionTimeMs(0);

        return stats;
    }

    // Inner classes for result objects

    /**
     * Comprehensive processing result containing all phases
     */
    public static class ProcessingResult {
        private File inputFile;
        private File schemaFile;
        private File outputFile;
        private SchemaElement schemaAnalysis;
        private ValidationResult initialValidation;
        private CorrectionResult correctionResult;
        private ValidationResult finalValidation;
        private Report report;
        private long totalProcessingTimeMs;

        // Getters and setters
        public File getInputFile() { return inputFile; }
        public void setInputFile(File inputFile) { this.inputFile = inputFile; }

        public File getSchemaFile() { return schemaFile; }
        public void setSchemaFile(File schemaFile) { this.schemaFile = schemaFile; }

        public File getOutputFile() { return outputFile; }
        public void setOutputFile(File outputFile) { this.outputFile = outputFile; }

        public SchemaElement getSchemaAnalysis() { return schemaAnalysis; }
        public void setSchemaAnalysis(SchemaElement schemaAnalysis) { this.schemaAnalysis = schemaAnalysis; }

        public ValidationResult getInitialValidation() { return initialValidation; }
        public void setInitialValidation(ValidationResult initialValidation) {
            this.initialValidation = initialValidation;
        }

        public CorrectionResult getCorrectionResult() { return correctionResult; }
        public void setCorrectionResult(CorrectionResult correctionResult) {
            this.correctionResult = correctionResult;
        }

        public ValidationResult getFinalValidation() { return finalValidation; }
        public void setFinalValidation(ValidationResult finalValidation) {
            this.finalValidation = finalValidation;
        }

        public Report getReport() { return report; }
        public void setReport(Report report) { this.report = report; }

        public long getTotalProcessingTimeMs() { return totalProcessingTimeMs; }
        public void setTotalProcessingTimeMs(long totalProcessingTimeMs) {
            this.totalProcessingTimeMs = totalProcessingTimeMs;
        }

        public boolean wasSuccessful() {
            return finalValidation != null ? finalValidation.isValid() :
                    (initialValidation != null && initialValidation.isValid());
        }

        public int getErrorReduction() {
            if (initialValidation == null || finalValidation == null) {
                return 0;
            }
            return initialValidation.getErrorCount() - finalValidation.getErrorCount();
        }
    }

    /**
     * Processing options for controlling behavior
     */
    public static class ProcessingOptions {
        private boolean analyzeSchema = true;
        private boolean applyCorrections = true;
        private boolean generateReport = true;
        private boolean createBackup = true;
        private boolean validateAfterCorrection = true;

        // Getters and setters
        public boolean isAnalyzeSchema() { return analyzeSchema; }
        public void setAnalyzeSchema(boolean analyzeSchema) { this.analyzeSchema = analyzeSchema; }

        public boolean isApplyCorrections() { return applyCorrections; }
        public void setApplyCorrections(boolean applyCorrections) { this.applyCorrections = applyCorrections; }

        public boolean isGenerateReport() { return generateReport; }
        public void setGenerateReport(boolean generateReport) { this.generateReport = generateReport; }

        public boolean isCreateBackup() { return createBackup; }
        public void setCreateBackup(boolean createBackup) { this.createBackup = createBackup; }

        public boolean isValidateAfterCorrection() { return validateAfterCorrection; }
        public void setValidateAfterCorrection(boolean validateAfterCorrection) {
            this.validateAfterCorrection = validateAfterCorrection;
        }
    }

    /**
     * Information about correction capabilities
     */
    public static class CorrectionCapabilities {
        private java.util.Set<com.xmlfixer.validation.model.ErrorType> supportedErrorTypes;
        private int strategiesAvailable;
        private java.util.Map<String, String> capabilities = new HashMap<>();

        public java.util.Set<com.xmlfixer.validation.model.ErrorType> getSupportedErrorTypes() {
            return supportedErrorTypes;
        }
        public void setSupportedErrorTypes(java.util.Set<com.xmlfixer.validation.model.ErrorType> supportedErrorTypes) {
            this.supportedErrorTypes = supportedErrorTypes;
        }

        public int getStrategiesAvailable() { return strategiesAvailable; }
        public void setStrategiesAvailable(int strategiesAvailable) {
            this.strategiesAvailable = strategiesAvailable;
        }

        public java.util.Map<String, String> getCapabilities() { return capabilities; }
        public void addCapability(String name, String description) {
            capabilities.put(name, description);
        }
    }

    /**
     * Processing performance statistics
     */
    public static class ProcessingStatistics {
        private long totalValidationsPerformed;
        private long totalCorrectionsApplied;
        private long averageValidationTimeMs;
        private long averageCorrectionTimeMs;

        // Getters and setters
        public long getTotalValidationsPerformed() { return totalValidationsPerformed; }
        public void setTotalValidationsPerformed(long totalValidationsPerformed) {
            this.totalValidationsPerformed = totalValidationsPerformed;
        }

        public long getTotalCorrectionsApplied() { return totalCorrectionsApplied; }
        public void setTotalCorrectionsApplied(long totalCorrectionsApplied) {
            this.totalCorrectionsApplied = totalCorrectionsApplied;
        }

        public long getAverageValidationTimeMs() { return averageValidationTimeMs; }
        public void setAverageValidationTimeMs(long averageValidationTimeMs) {
            this.averageValidationTimeMs = averageValidationTimeMs;
        }

        public long getAverageCorrectionTimeMs() { return averageCorrectionTimeMs; }
        public void setAverageCorrectionTimeMs(long averageCorrectionTimeMs) {
            this.averageCorrectionTimeMs = averageCorrectionTimeMs;
        }
    }

    // Keep the existing BatchProcessingResult inner class for compatibility
    /**
     * Inner class to represent batch processing results
     */
    public static class BatchProcessingResult {
        private int totalFiles;
        private int processedFiles;
        private int successfulFiles;
        private int failedFiles;
        private long startTime;
        private long endTime;

        // Getters and setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }

        public int getProcessedFiles() { return processedFiles; }
        public void setProcessedFiles(int processedFiles) { this.processedFiles = processedFiles; }

        public int getSuccessfulFiles() { return successfulFiles; }
        public void setSuccessfulFiles(int successfulFiles) { this.successfulFiles = successfulFiles; }

        public int getFailedFiles() { return failedFiles; }
        public void setFailedFiles(int failedNumbers) { this.failedFiles = failedNumbers; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        public long getTotalTimeMs() { return endTime - startTime; }

        public double getSuccessRate() {
            return totalFiles > 0 ? (double) successfulFiles / totalFiles * 100.0 : 0.0;
        }
    }
}
