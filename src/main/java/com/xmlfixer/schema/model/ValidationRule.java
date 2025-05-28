package com.xmlfixer.schema.model;

import com.xmlfixer.validation.model.ErrorType;

/**
 * Represents a validation rule derived from schema constraints
 */
public class ValidationRule {

    public enum RuleType {
        ELEMENT_REQUIRED("Element is required"),
        ELEMENT_CARDINALITY("Element occurrence constraints"),
        ATTRIBUTE_REQUIRED("Attribute is required"),
        ATTRIBUTE_VALUE("Attribute value constraints"),
        DATA_TYPE("Data type validation"),
        PATTERN_MATCH("Pattern matching"),
        VALUE_RANGE("Value range validation"),
        ENUMERATION("Enumeration validation"),
        ELEMENT_ORDER("Element ordering validation"),
        CONTENT_MODEL("Content model validation");

        private final String description;

        RuleType(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    public enum Severity {
        ERROR, WARNING, INFO
    }

    private RuleType ruleType;
    private String elementName;
    private String attributeName;
    private String expectedValue;
    private String pattern;
    private boolean required;
    private int minOccurs;
    private int maxOccurs;
    private String dataType;
    private String description;
    private Severity severity;
    private ErrorType relatedErrorType;

    public ValidationRule() {
        this.severity = Severity.ERROR;
        this.required = false;
        this.minOccurs = 0;
        this.maxOccurs = Integer.MAX_VALUE;
    }

    public ValidationRule(RuleType ruleType, String elementName) {
        this();
        this.ruleType = ruleType;
        this.elementName = elementName;
    }

    // Basic properties
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }

    public String getElementName() { return elementName; }
    public void setElementName(String elementName) { this.elementName = elementName; }

    public String getAttributeName() { return attributeName; }
    public void setAttributeName(String attributeName) { this.attributeName = attributeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public ErrorType getRelatedErrorType() { return relatedErrorType; }
    public void setRelatedErrorType(ErrorType relatedErrorType) { this.relatedErrorType = relatedErrorType; }

    // Constraint properties
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) {
        this.required = required;
        if (required && ruleType == RuleType.ELEMENT_REQUIRED) {
            this.relatedErrorType = ErrorType.MISSING_REQUIRED_ELEMENT;
        } else if (required && ruleType == RuleType.ATTRIBUTE_REQUIRED) {
            this.relatedErrorType = ErrorType.MISSING_REQUIRED_ATTRIBUTE;
        }
    }

    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) {
        this.pattern = pattern;
        if (pattern != null && ruleType == RuleType.PATTERN_MATCH) {
            this.relatedErrorType = ErrorType.PATTERN_MISMATCH;
        }
    }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) {
        this.dataType = dataType;
        if (dataType != null && ruleType == RuleType.DATA_TYPE) {
            this.relatedErrorType = ErrorType.INVALID_DATA_TYPE;
        }
    }

    // Occurrence constraints
    public int getMinOccurs() { return minOccurs; }
    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
        if (minOccurs > 0 && ruleType == RuleType.ELEMENT_CARDINALITY) {
            this.relatedErrorType = ErrorType.TOO_FEW_OCCURRENCES;
        }
    }

    public int getMaxOccurs() { return maxOccurs; }
    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
        if (maxOccurs < Integer.MAX_VALUE && ruleType == RuleType.ELEMENT_CARDINALITY) {
            this.relatedErrorType = ErrorType.TOO_MANY_OCCURRENCES;
        }
    }

    // Utility methods
    public boolean isOptional() {
        return !required && minOccurs == 0;
    }

    public boolean isUnbounded() {
        return maxOccurs == Integer.MAX_VALUE;
    }

    public boolean allowsMultiple() {
        return maxOccurs > 1 || isUnbounded();
    }

    public boolean hasPattern() {
        return pattern != null && !pattern.isEmpty();
    }

    public boolean hasExpectedValue() {
        return expectedValue != null && !expectedValue.isEmpty();
    }

    public boolean isCardinalityRule() {
        return ruleType == RuleType.ELEMENT_CARDINALITY;
    }

    public boolean isAttributeRule() {
        return ruleType == RuleType.ATTRIBUTE_REQUIRED || ruleType == RuleType.ATTRIBUTE_VALUE;
    }

    public boolean isElementRule() {
        return ruleType == RuleType.ELEMENT_REQUIRED || ruleType == RuleType.ELEMENT_CARDINALITY;
    }

    public boolean isDataTypeRule() {
        return ruleType == RuleType.DATA_TYPE || ruleType == RuleType.PATTERN_MATCH ||
                ruleType == RuleType.VALUE_RANGE || ruleType == RuleType.ENUMERATION;
    }

    /**
     * Validates if the given value matches this rule
     */
    public boolean validate(String actualValue) {
        if (actualValue == null || actualValue.isEmpty()) {
            return !required;
        }

        switch (ruleType) {
            case PATTERN_MATCH:
                return validatePattern(actualValue);
            case VALUE_RANGE:
                return validateRange(actualValue);
            case ENUMERATION:
                return validateEnumeration(actualValue);
            case DATA_TYPE:
                return validateDataType(actualValue);
            default:
                return true; // For rules that don't validate values directly
        }
    }

    /**
     * Validates if the occurrence count matches cardinality constraints
     */
    public boolean validateCardinality(int actualCount) {
        if (ruleType != RuleType.ELEMENT_CARDINALITY) {
            return true;
        }

        return actualCount >= minOccurs && actualCount <= maxOccurs;
    }

    private boolean validatePattern(String value) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }

        try {
            return value.matches(pattern);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateRange(String value) {
        // TODO: Implement range validation based on data type
        return true;
    }

    private boolean validateEnumeration(String value) {
        if (expectedValue == null || expectedValue.isEmpty()) {
            return true;
        }

        // For enumeration, expectedValue contains comma-separated allowed values
        String[] allowedValues = expectedValue.split(",");
        for (String allowedValue : allowedValues) {
            if (allowedValue.trim().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean validateDataType(String value) {
        if (dataType == null || dataType.isEmpty()) {
            return true;
        }

        try {
            switch (dataType.toLowerCase()) {
                case "int":
                case "integer":
                    Integer.parseInt(value);
                    return true;
                case "double":
                case "decimal":
                    Double.parseDouble(value);
                    return true;
                case "boolean":
                    return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
                case "date":
                    // TODO: Implement date validation
                    return true;
                default:
                    return true; // For string and other types
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets a human-readable description of this rule
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();

        if (ruleType != null) {
            sb.append(ruleType.getDescription());
        }

        if (elementName != null) {
            sb.append(" for element '").append(elementName).append("'");
        }

        if (attributeName != null) {
            sb.append(" attribute '").append(attributeName).append("'");
        }

        if (required) {
            sb.append(" (required)");
        }

        if (isCardinalityRule()) {
            sb.append(" (").append(minOccurs).append("-");
            if (isUnbounded()) {
                sb.append("unbounded");
            } else {
                sb.append(maxOccurs);
            }
            sb.append(" occurrences)");
        }

        if (hasPattern()) {
            sb.append(" matching pattern: ").append(pattern);
        }

        if (hasExpectedValue()) {
            sb.append(" expected: ").append(expectedValue);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("ValidationRule{type=%s, element='%s', required=%s, severity=%s}",
                ruleType, elementName, required, severity);
    }
}
