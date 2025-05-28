package com.xmlfixer.validation.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of XML validation against a schema
 */
public class ValidationResult {
    
    private File xmlFile;
    private File schemaFile;
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationError> warnings;
    private long validationTimeMs;
    
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.valid = true;
    }
    
    // File properties
    public File getXmlFile() { return xmlFile; }
    public void setXmlFile(File xmlFile) { this.xmlFile = xmlFile; }
    
    public File getSchemaFile() { return schemaFile; }
    public void setSchemaFile(File schemaFile) { this.schemaFile = schemaFile; }
    
    // Validation results
    public boolean isValid() { return valid && (errors == null || errors.isEmpty()); }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { 
        this.errors = errors; 
        if (errors != null && !errors.isEmpty()) {
            this.valid = false;
        }
    }
    
    public void addError(ValidationError error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.valid = false;
    }
    
    public List<ValidationError> getWarnings() { return warnings; }
    public void setWarnings(List<ValidationError> warnings) { this.warnings = warnings; }
    
    public void addWarning(ValidationError warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
    
    // Performance metrics
    public long getValidationTimeMs() { return validationTimeMs; }
    public void setValidationTimeMs(long validationTimeMs) { this.validationTimeMs = validationTimeMs; }
    
    // Utility methods
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
    
    public int getWarningCount() {
        return warnings != null ? warnings.size() : 0;
    }
    
    public boolean hasIssues() {
        return !isValid() || getWarningCount() > 0;
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{valid=%s, errors=%d, warnings=%d, file='%s'}", 
            isValid(), getErrorCount(), getWarningCount(), 
            xmlFile != null ? xmlFile.getName() : "null");
    }
}

