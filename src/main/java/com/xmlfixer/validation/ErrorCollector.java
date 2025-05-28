package com.xmlfixer.validation;

import com.xmlfixer.validation.model.ErrorType;
import com.xmlfixer.validation.model.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for collecting, organizing, and analyzing validation errors
 */
@Singleton
public class ErrorCollector {

    private static final Logger logger = LoggerFactory.getLogger(ErrorCollector.class);

    // Error storage by category
    private final Map<ErrorType, List<ValidationError>> errorsByType;
    private final Map<String, List<ValidationError>> errorsByElement;
    private final Map<String, List<ValidationError>> errorsByPath;
    private final List<ValidationError> allErrors;
    private final List<ValidationError> allWarnings;

    // Statistics
    private int totalErrorCount;
    private int totalWarningCount;
    private Map<ErrorType, Integer> errorTypeCount;

    @Inject
    public ErrorCollector() {
        this.errorsByType = new ConcurrentHashMap<>();
        this.errorsByElement = new ConcurrentHashMap<>();
        this.errorsByPath = new ConcurrentHashMap<>();
        this.allErrors = Collections.synchronizedList(new ArrayList<>());
        this.allWarnings = Collections.synchronizedList(new ArrayList<>());
        this.errorTypeCount = new ConcurrentHashMap<>();

        logger.info("ErrorCollector initialized");
    }

    /**
     * Reports a list of validation errors
     */
    public void reportErrors(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        for (ValidationError error : errors) {
            collectError(error);
        }

        logger.debug("Collected {} errors", errors.size());
    }

    /**
     * Reports a list of validation warnings
     */
    public void reportWarnings(List<ValidationError> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }

        for (ValidationError warning : warnings) {
            warning.setSeverity(ValidationError.Severity.WARNING);
            collectWarning(warning);
        }

        logger.debug("Collected {} warnings", warnings.size());
    }

    /**
     * Collects a single validation error
     */
    public void collectError(ValidationError error) {
        if (error == null) {
            return;
        }

        // Add to all errors list
        allErrors.add(error);
        totalErrorCount++;

        // Categorize by error type
        errorsByType.computeIfAbsent(error.getErrorType(), k -> new ArrayList<>()).add(error);

        // Categorize by element name
        if (error.getElementName() != null) {
            errorsByElement.computeIfAbsent(error.getElementName(), k -> new ArrayList<>()).add(error);
        }

        // Categorize by path
        if (error.getxPath() != null) {
            errorsByPath.computeIfAbsent(error.getxPath(), k -> new ArrayList<>()).add(error);
        }

        // Update error type count
        errorTypeCount.merge(error.getErrorType(), 1, Integer::sum);

        logger.trace("Collected error: {} at {}", error.getMessage(), error.getLocationString());
    }

    /**
     * Collects a single validation warning
     */
    public void collectWarning(ValidationError warning) {
        if (warning == null) {
            return;
        }

        warning.setSeverity(ValidationError.Severity.WARNING);
        allWarnings.add(warning);
        totalWarningCount++;

        logger.trace("Collected warning: {} at {}", warning.getMessage(), warning.getLocationString());
    }

    /**
     * Gets all collected errors
     */
    public List<ValidationError> getAllErrors() {
        return new ArrayList<>(allErrors);
    }

    /**
     * Gets all collected warnings
     */
    public List<ValidationError> getAllWarnings() {
        return new ArrayList<>(allWarnings);
    }

    /**
     * Gets errors by type
     */
    public List<ValidationError> getErrorsByType(ErrorType errorType) {
        return errorsByType.getOrDefault(errorType, Collections.emptyList());
    }

    /**
     * Gets errors by element name
     */
    public List<ValidationError> getErrorsByElement(String elementName) {
        return errorsByElement.getOrDefault(elementName, Collections.emptyList());
    }

    /**
     * Gets errors by XPath
     */
    public List<ValidationError> getErrorsByPath(String xPath) {
        return errorsByPath.getOrDefault(xPath, Collections.emptyList());
    }

    /**
     * Gets error summary statistics
     */
    public ErrorSummary getErrorSummary() {
        ErrorSummary summary = new ErrorSummary();
        summary.setTotalErrors(totalErrorCount);
        summary.setTotalWarnings(totalWarningCount);
        summary.setErrorTypeDistribution(new HashMap<>(errorTypeCount));
        summary.setMostCommonErrorType(findMostCommonErrorType());
        summary.setElementsWithErrors(errorsByElement.keySet());
        summary.setPathsWithErrors(errorsByPath.keySet());

        return summary;
    }

    /**
     * Groups errors by line number for easier fixing
     */
    public Map<Integer, List<ValidationError>> getErrorsByLine() {
        return allErrors.stream()
                .filter(error -> error.getLineNumber() > 0)
                .collect(Collectors.groupingBy(ValidationError::getLineNumber));
    }

    /**
     * Gets critical errors (those that must be fixed)
     */
    public List<ValidationError> getCriticalErrors() {
        Set<ErrorType> criticalTypes = EnumSet.of(
                ErrorType.MISSING_REQUIRED_ELEMENT,
                ErrorType.MISSING_REQUIRED_ATTRIBUTE,
                ErrorType.MALFORMED_XML,
                ErrorType.SCHEMA_VIOLATION
        );

        return allErrors.stream()
                .filter(error -> criticalTypes.contains(error.getErrorType()))
                .collect(Collectors.toList());
    }

    /**
     * Gets structural errors (element order, cardinality)
     */
    public List<ValidationError> getStructuralErrors() {
        Set<ErrorType> structuralTypes = EnumSet.of(
                ErrorType.INVALID_ELEMENT_ORDER,
                ErrorType.TOO_FEW_OCCURRENCES,
                ErrorType.TOO_MANY_OCCURRENCES,
                ErrorType.UNEXPECTED_ELEMENT
        );

        return allErrors.stream()
                .filter(error -> structuralTypes.contains(error.getErrorType()))
                .collect(Collectors.toList());
    }

    /**
     * Gets data quality errors (type, format, value issues)
     */
    public List<ValidationError> getDataQualityErrors() {
        Set<ErrorType> dataTypes = EnumSet.of(
                ErrorType.INVALID_DATA_TYPE,
                ErrorType.INVALID_FORMAT,
                ErrorType.INVALID_VALUE_RANGE,
                ErrorType.PATTERN_MISMATCH,
                ErrorType.EMPTY_REQUIRED_CONTENT
        );

        return allErrors.stream()
                .filter(error -> dataTypes.contains(error.getErrorType()))
                .collect(Collectors.toList());
    }

    /**
     * Generates a detailed error report
     */
    public String generateDetailedReport() {
        StringBuilder report = new StringBuilder();

        report.append("=== VALIDATION ERROR REPORT ===\n\n");

        // Summary
        report.append("SUMMARY:\n");
        report.append(String.format("Total Errors: %d\n", totalErrorCount));
        report.append(String.format("Total Warnings: %d\n", totalWarningCount));
        report.append(String.format("Error Types: %d\n", errorTypeCount.size()));
        report.append(String.format("Affected Elements: %d\n", errorsByElement.size()));
        report.append("\n");

        // Error type distribution
        report.append("ERROR TYPE DISTRIBUTION:\n");
        errorTypeCount.entrySet().stream()
                .sorted(Map.Entry.<ErrorType, Integer>comparingByValue().reversed())
                .forEach(entry -> report.append(String.format("  %s: %d\n",
                        entry.getKey().getDescription(), entry.getValue())));
        report.append("\n");

        // Critical errors
        List<ValidationError> criticalErrors = getCriticalErrors();
        if (!criticalErrors.isEmpty()) {
            report.append("CRITICAL ERRORS (Must Fix):\n");
            appendErrorList(report, criticalErrors);
            report.append("\n");
        }

        // Structural errors
        List<ValidationError> structuralErrors = getStructuralErrors();
        if (!structuralErrors.isEmpty()) {
            report.append("STRUCTURAL ERRORS:\n");
            appendErrorList(report, structuralErrors);
            report.append("\n");
        }

        // Data quality errors
        List<ValidationError> dataErrors = getDataQualityErrors();
        if (!dataErrors.isEmpty()) {
            report.append("DATA QUALITY ERRORS:\n");
            appendErrorList(report, dataErrors);
            report.append("\n");
        }

        // Warnings
        if (!allWarnings.isEmpty()) {
            report.append("WARNINGS:\n");
            appendErrorList(report, allWarnings);
            report.append("\n");
        }

        // Errors by location
        report.append("ERRORS BY LOCATION:\n");
        Map<Integer, List<ValidationError>> errorsByLine = getErrorsByLine();
        errorsByLine.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    report.append(String.format("  Line %d: %d error(s)\n",
                            entry.getKey(), entry.getValue().size()));
                });

        return report.toString();
    }

    /**
     * Clears all collected errors and warnings
     */
    public void clear() {
        errorsByType.clear();
        errorsByElement.clear();
        errorsByPath.clear();
        allErrors.clear();
        allWarnings.clear();
        errorTypeCount.clear();
        totalErrorCount = 0;
        totalWarningCount = 0;

        logger.debug("Error collector cleared");
    }

    /**
     * Finds the most common error type
     */
    private ErrorType findMostCommonErrorType() {
        return errorTypeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Appends a list of errors to the report
     */
    private void appendErrorList(StringBuilder report, List<ValidationError> errors) {
        errors.stream()
                .sorted(Comparator.comparing(ValidationError::getLineNumber))
                .forEach(error -> {
                    report.append(String.format("  - %s\n", error.getFullMessage()));
                    if (error.getExpectedValue() != null) {
                        report.append(String.format("    Expected: %s\n", error.getExpectedValue()));
                    }
                    if (error.getActualValue() != null) {
                        report.append(String.format("    Actual: %s\n", error.getActualValue()));
                    }
                });
    }

    /**
     * Inner class for error summary statistics
     */
    public static class ErrorSummary {
        private int totalErrors;
        private int totalWarnings;
        private Map<ErrorType, Integer> errorTypeDistribution;
        private ErrorType mostCommonErrorType;
        private Set<String> elementsWithErrors;
        private Set<String> pathsWithErrors;

        // Getters and setters
        public int getTotalErrors() { return totalErrors; }
        public void setTotalErrors(int totalErrors) { this.totalErrors = totalErrors; }

        public int getTotalWarnings() { return totalWarnings; }
        public void setTotalWarnings(int totalWarnings) { this.totalWarnings = totalWarnings; }

        public Map<ErrorType, Integer> getErrorTypeDistribution() { return errorTypeDistribution; }
        public void setErrorTypeDistribution(Map<ErrorType, Integer> errorTypeDistribution) {
            this.errorTypeDistribution = errorTypeDistribution;
        }

        public ErrorType getMostCommonErrorType() { return mostCommonErrorType; }
        public void setMostCommonErrorType(ErrorType mostCommonErrorType) {
            this.mostCommonErrorType = mostCommonErrorType;
        }

        public Set<String> getElementsWithErrors() { return elementsWithErrors; }
        public void setElementsWithErrors(Set<String> elementsWithErrors) {
            this.elementsWithErrors = elementsWithErrors;
        }

        public Set<String> getPathsWithErrors() { return pathsWithErrors; }
        public void setPathsWithErrors(Set<String> pathsWithErrors) {
            this.pathsWithErrors = pathsWithErrors;
        }

        public boolean hasCriticalErrors() {
            if (errorTypeDistribution == null) {
                return false;
            }

            return errorTypeDistribution.containsKey(ErrorType.MISSING_REQUIRED_ELEMENT) ||
                    errorTypeDistribution.containsKey(ErrorType.MISSING_REQUIRED_ATTRIBUTE) ||
                    errorTypeDistribution.containsKey(ErrorType.MALFORMED_XML);
        }

        @Override
        public String toString() {
            return String.format("ErrorSummary{errors=%d, warnings=%d, types=%d, elements=%d}",
                    totalErrors, totalWarnings,
                    errorTypeDistribution != null ? errorTypeDistribution.size() : 0,
                    elementsWithErrors != null ? elementsWithErrors.size() : 0);
        }
    }
}
