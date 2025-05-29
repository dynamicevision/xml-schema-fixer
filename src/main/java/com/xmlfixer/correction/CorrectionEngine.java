package com.xmlfixer.correction;

import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.correction.model.CorrectionGroup;
import com.xmlfixer.correction.model.CorrectionPlan;
import com.xmlfixer.correction.model.CorrectionResult;
import com.xmlfixer.correction.strategies.*;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ValidationResult;
import com.xmlfixer.validation.model.ValidationError;
import com.xmlfixer.validation.model.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced correction engine that applies intelligent correction strategies
 * to fix XML files based on schema validation errors
 */
@Singleton
public class CorrectionEngine {

    private static final Logger logger = LoggerFactory.getLogger(CorrectionEngine.class);

    private final DomManipulator domManipulator;
    private final Map<ErrorType, CorrectionStrategy> correctionStrategies;
    private final CorrectionPlanner correctionPlanner;

    @Inject
    public CorrectionEngine(DomManipulator domManipulator) {
        this.domManipulator = domManipulator;
        this.correctionPlanner = new CorrectionPlanner();
        this.correctionStrategies = initializeCorrectionStrategies();
        logger.info("CorrectionEngine initialized with {} strategies", correctionStrategies.size());
    }

    /**
     * Corrects an XML file based on validation results using intelligent strategies
     */
    public CorrectionResult correct(File xmlFile, File schemaFile, File outputFile,
                                    ValidationResult validationResult) {
        return correct(xmlFile, schemaFile, outputFile, validationResult, null);
    }

    /**
     * Corrects XML file with schema element context for better correction accuracy
     */
    public CorrectionResult correct(File xmlFile, File schemaFile, File outputFile,
                                    ValidationResult validationResult, SchemaElement rootSchema) {
        logger.info("Starting intelligent correction of XML file: {} using schema: {}",
                xmlFile.getName(), schemaFile.getName());

        long startTime = System.currentTimeMillis();
        CorrectionResult result = new CorrectionResult();
        result.setInputFile(xmlFile);
        result.setOutputFile(outputFile);
        result.setBeforeValidation(validationResult);

        try {
            // Check if correction is needed
            if (validationResult != null && validationResult.isValid()) {
                result.setNoChangesRequired(true);
                result.setSuccess(true);
                logger.info("XML file is already valid, no corrections needed");
                return result;
            }

            // Load and parse the XML document
            Document document = domManipulator.loadDocument(xmlFile);
            if (document == null) {
                throw new RuntimeException("Failed to load XML document");
            }

            // Phase 1: Analysis and Planning
            logger.debug("Phase 1: Analyzing errors and planning corrections");
            List<ValidationError> errors = validationResult != null ?
                    validationResult.getErrors() : new ArrayList<>();

            CorrectionPlan correctionPlan = correctionPlanner.createCorrectionPlan(
                    errors, document, rootSchema);

            logger.debug("Created correction plan with {} correction groups",
                    correctionPlan.getCorrectionGroups().size());

            // Phase 2: Apply Corrections
            logger.debug("Phase 2: Applying corrections systematically");
            List<CorrectionAction> appliedActions = new ArrayList<>();
            List<CorrectionAction> failedActions = new ArrayList<>();

            for (CorrectionGroup group : correctionPlan.getCorrectionGroups()) {
                logger.debug("Processing correction group: {} with {} actions",
                        group.getGroupType(), group.getActions().size());

                for (CorrectionAction action : group.getActions()) {
                    try {
                        boolean success = applyCorrectionAction(action, document, rootSchema);
                        if (success) {
                            action.setApplied(true);
                            appliedActions.add(action);
                            logger.debug("Successfully applied: {}", action.getDescription());
                        } else {
                            action.setFailureReason("Strategy execution failed");
                            failedActions.add(action);
                            logger.warn("Failed to apply: {}", action.getDescription());
                        }
                    } catch (Exception e) {
                        action.setFailureReason("Exception: " + e.getMessage());
                        failedActions.add(action);
                        logger.error("Error applying correction: {}", action.getDescription(), e);
                    }
                }
            }

            // Phase 3: Finalization
            logger.debug("Phase 3: Finalizing corrections and saving document");

            // Save the corrected document
            boolean saved = domManipulator.saveDocument(document, outputFile);
            if (!saved) {
                throw new RuntimeException("Failed to save corrected document");
            }

            // Set results
            result.setActionsApplied(appliedActions);
            result.setFailedActions(failedActions);
            result.setSuccess(true);

            long endTime = System.currentTimeMillis();
            result.setCorrectionTimeMs(endTime - startTime);

            logger.info("Correction completed for: {} ({}ms). Applied: {}, Failed: {}",
                    xmlFile.getName(), result.getCorrectionTimeMs(),
                    appliedActions.size(), failedActions.size());

            return result;

        } catch (Exception e) {
            logger.error("Correction failed for: {}", xmlFile.getName(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setCorrectionTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * Initializes correction strategies for different error types
     */
    private Map<ErrorType, CorrectionStrategy> initializeCorrectionStrategies() {
        Map<ErrorType, CorrectionStrategy> strategies = new HashMap<>();

        // Missing element strategies
        MissingElementStrategy missingElementStrategy = new MissingElementStrategy(domManipulator);
        strategies.put(ErrorType.MISSING_REQUIRED_ELEMENT, missingElementStrategy);

        // Ordering strategies
        OrderingStrategy orderingStrategy = new OrderingStrategy(domManipulator);
        strategies.put(ErrorType.INVALID_ELEMENT_ORDER, orderingStrategy);

        // Cardinality strategies
        CardinalityStrategy cardinalityStrategy = new CardinalityStrategy(domManipulator);
        strategies.put(ErrorType.TOO_FEW_OCCURRENCES, cardinalityStrategy);
        strategies.put(ErrorType.TOO_MANY_OCCURRENCES, cardinalityStrategy);

        // Data type strategies
        DataTypeStrategy dataTypeStrategy = new DataTypeStrategy(domManipulator);
        strategies.put(ErrorType.INVALID_DATA_TYPE, dataTypeStrategy);
        strategies.put(ErrorType.INVALID_FORMAT, dataTypeStrategy);
        strategies.put(ErrorType.PATTERN_MISMATCH, dataTypeStrategy);
        strategies.put(ErrorType.INVALID_VALUE_RANGE, dataTypeStrategy);

        // Attribute strategies
        AttributeStrategy attributeStrategy = new AttributeStrategy(domManipulator);
        strategies.put(ErrorType.MISSING_REQUIRED_ATTRIBUTE, attributeStrategy);
        strategies.put(ErrorType.INVALID_ATTRIBUTE_VALUE, attributeStrategy);

        // Content strategies
        ContentStrategy contentStrategy = new ContentStrategy(domManipulator);
        strategies.put(ErrorType.EMPTY_REQUIRED_CONTENT, contentStrategy);
        strategies.put(ErrorType.INVALID_CONTENT_MODEL, contentStrategy);

        return strategies;
    }

    /**
     * Applies a single correction action using the appropriate strategy
     */
    private boolean applyCorrectionAction(CorrectionAction action, Document document,
                                          SchemaElement rootSchema) {
        ErrorType relatedErrorType = action.getRelatedErrorType();
        if (relatedErrorType == null) {
            logger.warn("Cannot apply correction action without related error type: {}",
                    action.getDescription());
            return false;
        }

        CorrectionStrategy strategy = correctionStrategies.get(relatedErrorType);
        if (strategy == null) {
            logger.warn("No correction strategy available for error type: {}", relatedErrorType);
            return false;
        }

        try {
            return strategy.canCorrect(action, document, rootSchema) &&
                    strategy.applyCorrection(action, document, rootSchema);
        } catch (Exception e) {
            logger.error("Strategy execution failed for action: {}", action.getDescription(), e);
            return false;
        }
    }

    /**
     * Gets available correction strategies
     */
    public Set<ErrorType> getSupportedErrorTypes() {
        return new HashSet<>(correctionStrategies.keySet());
    }

    /**
     * Validates if a correction action can be applied
     */
    public boolean canApplyCorrection(CorrectionAction action, Document document,
                                      SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();
        CorrectionStrategy strategy = correctionStrategies.get(errorType);

        return strategy != null && strategy.canCorrect(action, document, rootSchema);
    }

    /**
     * Gets correction statistics for analysis
     */
    public CorrectionStatistics getCorrectionStatistics(CorrectionResult result) {
        CorrectionStatistics stats = new CorrectionStatistics();

        if (result.getActionsApplied() != null) {
            Map<CorrectionAction.ActionType, Long> actionTypeCounts = result.getActionsApplied()
                    .stream()
                    .collect(Collectors.groupingBy(
                            CorrectionAction::getActionType,
                            Collectors.counting()));
            stats.setActionTypeDistribution(actionTypeCounts);
            stats.setTotalCorrections(result.getActionsApplied().size());
        }

        if (result.getFailedActions() != null) {
            stats.setFailedCorrections(result.getFailedActions().size());
        }

        stats.setCorrectionTimeMs(result.getCorrectionTimeMs());
        stats.setSuccessRate(calculateSuccessRate(result));

        return stats;
    }

    private double calculateSuccessRate(CorrectionResult result) {
        int total = result.getAppliedActionCount() + result.getFailedActionCount();
        if (total == 0) return 100.0;

        return (double) result.getAppliedActionCount() / total * 100.0;
    }

    /**
     * Inner class for correction statistics
     */
    public static class CorrectionStatistics {
        private int totalCorrections;
        private int failedCorrections;
        private long correctionTimeMs;
        private double successRate;
        private Map<CorrectionAction.ActionType, Long> actionTypeDistribution;

        // Getters and setters
        public int getTotalCorrections() { return totalCorrections; }
        public void setTotalCorrections(int totalCorrections) { this.totalCorrections = totalCorrections; }

        public int getFailedCorrections() { return failedCorrections; }
        public void setFailedCorrections(int failedCorrections) { this.failedCorrections = failedCorrections; }

        public long getCorrectionTimeMs() { return correctionTimeMs; }
        public void setCorrectionTimeMs(long correctionTimeMs) { this.correctionTimeMs = correctionTimeMs; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public Map<CorrectionAction.ActionType, Long> getActionTypeDistribution() {
            return actionTypeDistribution;
        }
        public void setActionTypeDistribution(Map<CorrectionAction.ActionType, Long> actionTypeDistribution) {
            this.actionTypeDistribution = actionTypeDistribution;
        }

        @Override
        public String toString() {
            return String.format("CorrectionStatistics{total=%d, failed=%d, successRate=%.1f%%, time=%dms}",
                    totalCorrections, failedCorrections, successRate, correctionTimeMs);
        }
    }
}
