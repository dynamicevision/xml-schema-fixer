package com.xmlfixer.correction;

import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.correction.model.CorrectionPlan;
import com.xmlfixer.correction.model.CorrectionResult;
import com.xmlfixer.correction.strategies.CorrectionStrategy;
import com.xmlfixer.correction.strategies.MissingElementStrategy;
import com.xmlfixer.correction.strategies.OrderingStrategy;
import com.xmlfixer.correction.strategies.CardinalityStrategy;
import com.xmlfixer.correction.strategies.DataTypeStrategy;
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
 * Main correction engine that orchestrates XML correction using multiple strategies
 */
@Singleton
public class CorrectionEngine {

    private static final Logger logger = LoggerFactory.getLogger(CorrectionEngine.class);

    private final DomManipulator domManipulator;
    private final List<CorrectionStrategy> strategies;

    @Inject
    public CorrectionEngine(DomManipulator domManipulator,
                            MissingElementStrategy missingElementStrategy,
                            OrderingStrategy orderingStrategy,
                            CardinalityStrategy cardinalityStrategy,
                            DataTypeStrategy dataTypeStrategy) {
        this.domManipulator = domManipulator;

        // Initialize strategies in priority order
        this.strategies = Arrays.asList(
                missingElementStrategy,    // Highest priority - structural fixes
                orderingStrategy,          // High priority - element ordering
                cardinalityStrategy,       // Medium priority - occurrence fixes
                dataTypeStrategy          // Lower priority - data quality fixes
        );

        logger.info("CorrectionEngine initialized with {} strategies", strategies.size());
    }

    /**
     * Corrects an XML file based on validation results and schema constraints
     */
    public CorrectionResult correct(File xmlFile, File schemaFile, File outputFile,
                                    ValidationResult validationResult, SchemaElement schema) {
        logger.info("Starting correction of {} using schema {}",
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

            // Load XML document for manipulation
            Document document = domManipulator.loadDocument(xmlFile);

            // Create correction plan
            CorrectionPlan plan = createCorrectionPlan(validationResult, schema);

            // Execute correction plan
            executeCorrectionPlan(document, plan, result);

            // Save corrected document
            domManipulator.saveDocument(document, outputFile);

            // Calculate processing time
            long endTime = System.currentTimeMillis();
            result.setCorrectionTimeMs(endTime - startTime);
            result.setSuccess(true);

            logger.info("Correction completed for: {} ({}ms) - {} actions applied",
                    xmlFile.getName(), result.getCorrectionTimeMs(), result.getAppliedActionCount());

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
     * Creates a comprehensive correction plan based on validation errors
     */
    private CorrectionPlan createCorrectionPlan(ValidationResult validationResult, SchemaElement schema) {
        logger.debug("Creating correction plan for {} errors", validationResult.getErrorCount());

        CorrectionPlan plan = new CorrectionPlan();

        if (validationResult.getErrors() == null || validationResult.getErrors().isEmpty()) {
            return plan;
        }

        // Group errors by type for strategic processing
        Map<ErrorType, List<ValidationError>> errorsByType = validationResult.getErrors().stream()
                .collect(Collectors.groupingBy(ValidationError::getErrorType));

        // Generate correction actions for each error type using appropriate strategies
        for (CorrectionStrategy strategy : strategies) {
            List<CorrectionAction> actions = strategy.generateCorrections(errorsByType, schema);
            plan.addActions(actions);
        }

        // Prioritize and optimize the correction plan
        optimizeCorrectionPlan(plan);

        logger.debug("Created correction plan with {} actions", plan.getActionCount());
        return plan;
    }

    /**
     * Optimizes the correction plan by prioritizing actions and removing conflicts
     */
    private void optimizeCorrectionPlan(CorrectionPlan plan) {
        logger.debug("Optimizing correction plan");

        // Sort actions by priority (critical structural fixes first)
        plan.sortActionsByPriority();

        // Remove conflicting actions (e.g., don't try to move an element that's being added)
        plan.removeConflictingActions();

        // Group related actions for batch processing
        plan.groupRelatedActions();

        logger.debug("Optimized correction plan: {} actions remaining", plan.getActionCount());
    }

    /**
     * Executes the correction plan by applying actions to the DOM
     */
    private void executeCorrectionPlan(Document document, CorrectionPlan plan, CorrectionResult result) {
        logger.debug("Executing correction plan with {} actions", plan.getActionCount());

        int successCount = 0;
        int failureCount = 0;

        for (CorrectionAction action : plan.getActions()) {
            try {
                logger.debug("Applying correction action: {}", action.getDescription());

                // Apply the correction action
                boolean success = applyCorrectionAction(document, action);

                if (success) {
                    action.setApplied(true);
                    result.addAppliedAction(action);
                    successCount++;
                    logger.debug("Successfully applied action: {}", action.getDescription());
                } else {
                    action.setFailureReason("Action execution failed");
                    result.addFailedAction(action);
                    failureCount++;
                    logger.warn("Failed to apply action: {}", action.getDescription());
                }

            } catch (Exception e) {
                action.setFailureReason("Exception during action execution: " + e.getMessage());
                result.addFailedAction(action);
                failureCount++;
                logger.error("Exception while applying action: {}", action.getDescription(), e);
            }
        }

        logger.info("Correction plan execution completed: {} successful, {} failed",
                successCount, failureCount);
    }

    /**
     * Applies a single correction action to the DOM document
     */
    private boolean applyCorrectionAction(Document document, CorrectionAction action) {
        switch (action.getActionType()) {
            case ADD_ELEMENT:
                return domManipulator.addElement(document, action);
            case REMOVE_ELEMENT:
                return domManipulator.removeElement(document, action);
            case MOVE_ELEMENT:
                return domManipulator.moveElement(document, action);
            case MODIFY_ELEMENT:
                return domManipulator.modifyElement(document, action);
            case ADD_ATTRIBUTE:
                return domManipulator.addAttribute(document, action);
            case REMOVE_ATTRIBUTE:
                return domManipulator.removeAttribute(document, action);
            case MODIFY_ATTRIBUTE:
                return domManipulator.modifyAttribute(document, action);
            case CHANGE_TEXT_CONTENT:
                return domManipulator.changeTextContent(document, action);
            case REORDER_ELEMENTS:
                return domManipulator.reorderElements(document, action);
            case FIX_NAMESPACE:
                return domManipulator.fixNamespace(document, action);
            default:
                logger.warn("Unknown correction action type: {}", action.getActionType());
                return false;
        }
    }

    /**
     * Validates the correction result by re-running validation
     */
    public ValidationResult validateCorrectionResult(File correctedFile, File schemaFile) {
        // This will be implemented when we integrate with the validation engine
        // For now, return a placeholder
        logger.debug("Validation of correction result not yet implemented");
        return null;
    }

    /**
     * Gets correction statistics and metrics
     */
    public CorrectionStatistics getCorrectionStatistics(CorrectionResult result) {
        CorrectionStatistics stats = new CorrectionStatistics();
        stats.setTotalActions(result.getAppliedActionCount() + result.getFailedActionCount());
        stats.setSuccessfulActions(result.getAppliedActionCount());
        stats.setFailedActions(result.getFailedActionCount());
        stats.setProcessingTimeMs(result.getCorrectionTimeMs());

        // Calculate success rate
        if (stats.getTotalActions() > 0) {
            stats.setSuccessRate((double) stats.getSuccessfulActions() / stats.getTotalActions() * 100);
        }

        return stats;
    }

    /**
     * Inner class for correction statistics
     */
    public static class CorrectionStatistics {
        private int totalActions;
        private int successfulActions;
        private int failedActions;
        private long processingTimeMs;
        private double successRate;

        // Getters and setters
        public int getTotalActions() { return totalActions; }
        public void setTotalActions(int totalActions) { this.totalActions = totalActions; }

        public int getSuccessfulActions() { return successfulActions; }
        public void setSuccessfulActions(int successfulActions) { this.successfulActions = successfulActions; }

        public int getFailedActions() { return failedActions; }
        public void setFailedActions(int failedActions) { this.failedActions = failedActions; }

        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        @Override
        public String toString() {
            return String.format("CorrectionStatistics{total=%d, successful=%d, failed=%d, successRate=%.1f%%, time=%dms}",
                    totalActions, successfulActions, failedActions, successRate, processingTimeMs);
        }
    }
}
