package com.xmlfixer.correction.model;

import com.xmlfixer.validation.model.ValidationResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of XML correction operations
 */
public class CorrectionResult {

    private File inputFile;
    private File outputFile;
    private boolean success;
    private boolean noChangesRequired;
    private String errorMessage;
    private List<CorrectionAction> actionsApplied;
    private List<CorrectionAction> failedActions;
    private long correctionTimeMs;
    private ValidationResult beforeValidation;
    private ValidationResult afterValidation;

    public CorrectionResult() {
        this.actionsApplied = new ArrayList<>();
        this.failedActions = new ArrayList<>();
        this.success = false;
        this.noChangesRequired = false;
    }

    // File properties
    public File getInputFile() { return inputFile; }
    public void setInputFile(File inputFile) { this.inputFile = inputFile; }

    public File getOutputFile() { return outputFile; }
    public void setOutputFile(File outputFile) { this.outputFile = outputFile; }

    // Result status
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isNoChangesRequired() { return noChangesRequired; }
    public void setNoChangesRequired(boolean noChangesRequired) {
        this.noChangesRequired = noChangesRequired;
        if (noChangesRequired) {
            this.success = true;
        }
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    // Actions performed
    public List<CorrectionAction> getActionsApplied() { return actionsApplied; }
    public void setActionsApplied(List<CorrectionAction> actionsApplied) {
        this.actionsApplied = actionsApplied;
    }

    public void addAppliedAction(CorrectionAction action) {
        if (this.actionsApplied == null) {
            this.actionsApplied = new ArrayList<>();
        }
        this.actionsApplied.add(action);
    }

    public List<CorrectionAction> getFailedActions() { return failedActions; }
    public void setFailedActions(List<CorrectionAction> failedActions) {
        this.failedActions = failedActions;
    }

    public void addFailedAction(CorrectionAction action) {
        if (this.failedActions == null) {
            this.failedActions = new ArrayList<>();
        }
        this.failedActions.add(action);
    }

    // Performance metrics
    public long getCorrectionTimeMs() { return correctionTimeMs; }
    public void setCorrectionTimeMs(long correctionTimeMs) { this.correctionTimeMs = correctionTimeMs; }

    // Validation results
    public ValidationResult getBeforeValidation() { return beforeValidation; }
    public void setBeforeValidation(ValidationResult beforeValidation) {
        this.beforeValidation = beforeValidation;
    }

    public ValidationResult getAfterValidation() { return afterValidation; }
    public void setAfterValidation(ValidationResult afterValidation) {
        this.afterValidation = afterValidation;
    }

    // Utility methods
    public int getAppliedActionCount() {
        return actionsApplied != null ? actionsApplied.size() : 0;
    }

    public int getFailedActionCount() {
        return failedActions != null ? failedActions.size() : 0;
    }

    public boolean hasValidationImprovement() {
        if (beforeValidation == null || afterValidation == null) {
            return false;
        }
        return beforeValidation.getErrorCount() > afterValidation.getErrorCount();
    }

    public int getErrorReduction() {
        if (beforeValidation == null || afterValidation == null) {
            return 0;
        }
        return beforeValidation.getErrorCount() - afterValidation.getErrorCount();
    }

    @Override
    public String toString() {
        return String.format("CorrectionResult{success=%s, actionsApplied=%d, noChanges=%s, file='%s'}",
            success, getAppliedActionCount(), noChangesRequired,
            inputFile != null ? inputFile.getName() : "null");
    }
}

