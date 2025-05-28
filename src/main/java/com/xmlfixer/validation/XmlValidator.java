package com.xmlfixer.validation;

import com.xmlfixer.common.exceptions.ValidationException;
import com.xmlfixer.parsing.XmlParser;
import com.xmlfixer.schema.SchemaAnalyzer;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.schema.model.ValidationRule;
import com.xmlfixer.validation.model.ValidationResult;
import com.xmlfixer.validation.model.ValidationError;
import com.xmlfixer.validation.model.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating XML files against XSD schemas
 * Integrates schema analysis with streaming validation for comprehensive error detection
 */
@Singleton
public class XmlValidator {

    private static final Logger logger = LoggerFactory.getLogger(XmlValidator.class);

    private final StreamingValidator streamingValidator;
    private final ErrorCollector errorCollector;
    private final XmlParser xmlParser;
    private final SchemaAnalyzer schemaAnalyzer;

    @Inject
    public XmlValidator(StreamingValidator streamingValidator,
                        ErrorCollector errorCollector,
                        XmlParser xmlParser,
                        SchemaAnalyzer schemaAnalyzer) {
        this.streamingValidator = streamingValidator;
        this.errorCollector = errorCollector;
        this.xmlParser = xmlParser;
        this.schemaAnalyzer = schemaAnalyzer;
        logger.info("XmlValidator initialized");
    }

    /**
     * Validates an XML file against a schema with comprehensive error detection
     */
    public ValidationResult validate(File xmlFile, File schemaFile) {
        logger.info("Validating XML file: {} against schema: {}",
                xmlFile.getName(), schemaFile.getName());

        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult();
        result.setXmlFile(xmlFile);
        result.setSchemaFile(schemaFile);

        try {
            // Clear any previous errors
            errorCollector.clear();

            // Step 1: Analyze the schema
            logger.debug("Analyzing schema structure");
            SchemaElement rootSchema = schemaAnalyzer.analyzeSchema(schemaFile);

            // Step 2: Extract validation rules from schema
            Map<String, List<ValidationRule>> validationRules = extractValidationRules(rootSchema);
            logger.debug("Extracted {} validation rule sets", validationRules.size());

            // Step 3: Perform streaming validation
            logger.debug("Starting streaming validation");
            ValidationResult streamingResult = streamingValidator.validateStreaming(
                    xmlFile, rootSchema, validationRules);

            // Step 4: Merge results
            result.setErrors(streamingResult.getErrors());
            result.setWarnings(streamingResult.getWarnings());

            // Step 5: Perform additional validation checks
            performAdditionalValidation(xmlFile, rootSchema, result);

            // Step 6: Generate validation summary
            result.setValid(result.getErrors().isEmpty());

            long endTime = System.currentTimeMillis();
            result.setValidationTimeMs(endTime - startTime);

            // Log summary
            logValidationSummary(result);

            return result;

        } catch (Exception e) {
            logger.error("Validation failed for: {}", xmlFile.getName(), e);

            ValidationError error = new ValidationError(
                    ErrorType.UNKNOWN_ERROR,
                    "Validation failed: " + e.getMessage()
            );
            result.addError(error);
            result.setValid(false);
            result.setValidationTimeMs(System.currentTimeMillis() - startTime);

            return result;
        }
    }

    /**
     * Quick validation check without detailed results
     */
    public boolean isValid(File xmlFile, File schemaFile) {
        try {
            ValidationResult result = validate(xmlFile, schemaFile);
            return result.isValid();
        } catch (Exception e) {
            logger.warn("Quick validation check failed for: {}", xmlFile.getName(), e);
            return false;
        }
    }

    /**
     * Validates XML with options for controlling validation behavior
     */
    public ValidationResult validateWithOptions(File xmlFile, File schemaFile,
                                                ValidationOptions options) {
        logger.info("Validating with custom options: {}", options);

        ValidationResult result = validate(xmlFile, schemaFile);

        // Apply options
        if (options != null) {
            if (!options.isIncludeWarnings()) {
                result.setWarnings(new ArrayList<>());
            }

            if (options.getMaxErrors() > 0 && result.getErrorCount() > options.getMaxErrors()) {
                List<ValidationError> limitedErrors = result.getErrors().stream()
                        .limit(options.getMaxErrors())
                        .collect(Collectors.toList());
                result.setErrors(limitedErrors);
            }

            if (options.isStopOnFirstError() && !result.getErrors().isEmpty()) {
                result.setErrors(Collections.singletonList(result.getErrors().get(0)));
            }
        }

        return result;
    }

    /**
     * Extracts validation rules from schema element hierarchy
     */
    private Map<String, List<ValidationRule>> extractValidationRules(SchemaElement rootSchema) {
        Map<String, List<ValidationRule>> rulesMap = new HashMap<>();

        // Recursively extract rules from schema
        extractRulesRecursive(rootSchema, rulesMap);

        return rulesMap;
    }

    /**
     * Recursively extracts validation rules from schema elements
     */
    private void extractRulesRecursive(SchemaElement element,
                                       Map<String, List<ValidationRule>> rulesMap) {
        if (element == null) {
            return;
        }

        List<ValidationRule> elementRules = new ArrayList<>();

        // Create rules based on element properties

        // Required element rule
        if (element.isRequired()) {
            ValidationRule requiredRule = new ValidationRule(
                    ValidationRule.RuleType.ELEMENT_REQUIRED, element.getName());
            requiredRule.setRequired(true);
            elementRules.add(requiredRule);
        }

        // Cardinality rule
        if (element.getMinOccurs() > 0 || element.getMaxOccurs() < Integer.MAX_VALUE) {
            ValidationRule cardinalityRule = new ValidationRule(
                    ValidationRule.RuleType.ELEMENT_CARDINALITY, element.getName());
            cardinalityRule.setMinOccurs(element.getMinOccurs());
            cardinalityRule.setMaxOccurs(element.getMaxOccurs());
            elementRules.add(cardinalityRule);
        }

        // Data type rule
        if (element.getType() != null && !element.getType().equals("complexType")) {
            ValidationRule typeRule = new ValidationRule(
                    ValidationRule.RuleType.DATA_TYPE, element.getName());
            typeRule.setDataType(element.getType());
            elementRules.add(typeRule);
        }

        // Pattern rules from constraints
        if (element.hasConstraints()) {
            element.getConstraints().forEach(constraint -> {
                ValidationRule constraintRule = new ValidationRule(
                        ValidationRule.RuleType.PATTERN_MATCH, element.getName());
                constraintRule.setPattern(constraint.getValue());
                constraintRule.setDescription(constraint.getDescription());
                elementRules.add(constraintRule);
            });
        }

        if (!elementRules.isEmpty()) {
            rulesMap.put(element.getName(), elementRules);
        }

        // Process children
        if (element.hasChildren()) {
            for (SchemaElement child : element.getChildren()) {
                extractRulesRecursive(child, rulesMap);
            }
        }
    }

    /**
     * Performs additional validation checks beyond streaming validation
     */
    private void performAdditionalValidation(File xmlFile, SchemaElement rootSchema,
                                             ValidationResult result) {
        // Check file size
        long fileSizeBytes = xmlFile.length();
        long maxSizeMB = 500; // Could be configurable

        if (fileSizeBytes > maxSizeMB * 1024 * 1024) {
            ValidationError sizeWarning = new ValidationError(
                    ErrorType.UNKNOWN_ERROR,
                    String.format("XML file size (%.2f MB) exceeds recommended maximum (%d MB)",
                            fileSizeBytes / (1024.0 * 1024.0), maxSizeMB)
            );
            sizeWarning.setSeverity(ValidationError.Severity.WARNING);
            result.addWarning(sizeWarning);
        }

        // Check for schema namespace consistency
        validateNamespaceConsistency(rootSchema, result);
    }

    /**
     * Validates namespace consistency
     */
    private void validateNamespaceConsistency(SchemaElement rootSchema, ValidationResult result) {
        if (rootSchema.getNamespace() != null) {
            // Check if namespace is properly declared and used
            logger.debug("Validating namespace: {}", rootSchema.getNamespace());
        }
    }

    /**
     * Logs a summary of validation results
     */
    private void logValidationSummary(ValidationResult result) {
        logger.info("Validation completed for: {} in {}ms",
                result.getXmlFile().getName(), result.getValidationTimeMs());
        logger.info("Valid: {}, Errors: {}, Warnings: {}",
                result.isValid(), result.getErrorCount(), result.getWarningCount());

        if (logger.isDebugEnabled() && !result.isValid()) {
            logger.debug("Error summary:");

            // Group errors by type
            Map<ErrorType, Long> errorTypeCounts = result.getErrors().stream()
                    .collect(Collectors.groupingBy(ValidationError::getErrorType,
                            Collectors.counting()));

            errorTypeCounts.forEach((type, count) ->
                    logger.debug("  {}: {}", type.getDescription(), count));
        }
    }

    /**
     * Gets a detailed validation report
     */
    public String getValidationReport(ValidationResult result) {
        if (result == null) {
            return "No validation result available";
        }

        StringBuilder report = new StringBuilder();

        report.append("=== XML VALIDATION REPORT ===\n\n");
        report.append("File: ").append(result.getXmlFile().getName()).append("\n");
        report.append("Schema: ").append(result.getSchemaFile().getName()).append("\n");
        report.append("Valid: ").append(result.isValid() ? "YES" : "NO").append("\n");
        report.append("Validation Time: ").append(result.getValidationTimeMs()).append(" ms\n");
        report.append("\n");

        if (!result.isValid()) {
            report.append("ERRORS (").append(result.getErrorCount()).append("):\n");
            result.getErrors().forEach(error -> {
                report.append("  - ").append(error.getFullMessage()).append("\n");
            });
            report.append("\n");
        }

        if (result.getWarningCount() > 0) {
            report.append("WARNINGS (").append(result.getWarningCount()).append("):\n");
            result.getWarnings().forEach(warning -> {
                report.append("  - ").append(warning.getFullMessage()).append("\n");
            });
            report.append("\n");
        }

        if (result.isValid()) {
            report.append("✓ The XML file is valid according to the schema.\n");
        } else {
            report.append("✗ The XML file contains validation errors that need to be fixed.\n");
        }

        return report.toString();
    }

    /**
     * Options for controlling validation behavior
     */
    public static class ValidationOptions {
        private boolean includeWarnings = true;
        private boolean stopOnFirstError = false;
        private int maxErrors = 0; // 0 means no limit
        private boolean validateNamespaces = true;
        private boolean validateReferences = true;

        // Getters and setters
        public boolean isIncludeWarnings() { return includeWarnings; }
        public void setIncludeWarnings(boolean includeWarnings) {
            this.includeWarnings = includeWarnings;
        }

        public boolean isStopOnFirstError() { return stopOnFirstError; }
        public void setStopOnFirstError(boolean stopOnFirstError) {
            this.stopOnFirstError = stopOnFirstError;
        }

        public int getMaxErrors() { return maxErrors; }
        public void setMaxErrors(int maxErrors) { this.maxErrors = maxErrors; }

        public boolean isValidateNamespaces() { return validateNamespaces; }
        public void setValidateNamespaces(boolean validateNamespaces) {
            this.validateNamespaces = validateNamespaces;
        }

        public boolean isValidateReferences() { return validateReferences; }
        public void setValidateReferences(boolean validateReferences) {
            this.validateReferences = validateReferences;
        }

        @Override
        public String toString() {
            return String.format("ValidationOptions{warnings=%s, stopFirst=%s, maxErrors=%d}",
                    includeWarnings, stopOnFirstError, maxErrors);
        }
    }
}
