package com.xmlfixer.reporting.model;

import com.xmlfixer.correction.model.CorrectionResult;
import com.xmlfixer.validation.model.ValidationResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a processing report containing validation and correction results
 */
public class Report {
    
    private String title;
    private LocalDateTime generatedAt;
    private ValidationResult validationResult;
    private CorrectionResult correctionResult;
    private List<ReportSection> sections;
    private ReportStatistics statistics;
    
    public Report() {
        this.generatedAt = LocalDateTime.now();
        this.sections = new ArrayList<>();
        this.statistics = new ReportStatistics();
    }
    
    public Report(String title) {
        this();
        this.title = title;
    }
    
    // Basic properties
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    // Results
    public ValidationResult getValidationResult() { return validationResult; }
    public void setValidationResult(ValidationResult validationResult) { 
        this.validationResult = validationResult; 
    }
    
    public CorrectionResult getCorrectionResult() { return correctionResult; }
    public void setCorrectionResult(CorrectionResult correctionResult) { 
        this.correctionResult = correctionResult; 
    }
    
    // Sections
    public List<ReportSection> getSections() { return sections; }
    public void setSections(List<ReportSection> sections) { this.sections = sections; }
    
    public void addSection(ReportSection section) {
        if (this.sections == null) {
            this.sections = new ArrayList<>();
        }
        this.sections.add(section);
    }
    
    // Statistics
    public ReportStatistics getStatistics() { return statistics; }
    public void setStatistics(ReportStatistics statistics) { this.statistics = statistics; }
    
    // Utility methods
    public boolean hasValidationResults() {
        return validationResult != null;
    }
    
    public boolean hasCorrectionResults() {
        return correctionResult != null;
    }
    
    @Override
    public String toString() {
        return String.format("Report{title='%s', generatedAt=%s, hasValidation=%s, hasCorrection=%s}", 
            title, generatedAt, hasValidationResults(), hasCorrectionResults());
    }
    
    /**
     * Inner class for report statistics
     */
    public static class ReportStatistics {
        private int totalFiles;
        private int validFiles;
        private int invalidFiles;
        private int correctedFiles;
        private int totalErrors;
        private int totalWarnings;
        private int fixedErrors;
        private long processingTimeMs;
        
        // Getters and setters
        public int getTotalFiles() { return totalFiles; }
        public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
        
        public int getValidFiles() { return validFiles; }
        public void setValidFiles(int validFiles) { this.validFiles = validFiles; }
        
        public int getInvalidFiles() { return invalidFiles; }
        public void setInvalidFiles(int invalidFiles) { this.invalidFiles = invalidFiles; }
        
        public int getCorrectedFiles() { return correctedFiles; }
        public void setCorrectedFiles(int correctedFiles) { this.correctedFiles = correctedFiles; }
        
        public int getTotalErrors() { return totalErrors; }
        public void setTotalErrors(int totalErrors) { this.totalErrors = totalErrors; }
        
        public int getTotalWarnings() { return totalWarnings; }
        public void setTotalWarnings(int totalWarnings) { this.totalWarnings = totalWarnings; }
        
        public int getFixedErrors() { return fixedErrors; }
        public void setFixedErrors(int fixedErrors) { this.fixedErrors = fixedErrors; }
        
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }
}

