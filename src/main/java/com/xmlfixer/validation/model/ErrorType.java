package com.xmlfixer.validation.model;

/**
 * Enumeration of different types of validation errors
 */
public enum ErrorType {
    
    // Structural errors
    MISSING_REQUIRED_ELEMENT("Missing required element"),
    INVALID_ELEMENT_ORDER("Invalid element order"),
    UNEXPECTED_ELEMENT("Unexpected element"),
    MALFORMED_XML("Malformed XML structure"),
    
    // Cardinality errors
    TOO_FEW_OCCURRENCES("Too few element occurrences"),
    TOO_MANY_OCCURRENCES("Too many element occurrences"),
    
    // Data type errors
    INVALID_DATA_TYPE("Invalid data type"),
    INVALID_FORMAT("Invalid format"),
    INVALID_VALUE_RANGE("Value out of allowed range"),
    
    // Attribute errors
    MISSING_REQUIRED_ATTRIBUTE("Missing required attribute"),
    INVALID_ATTRIBUTE_VALUE("Invalid attribute value"),
    UNEXPECTED_ATTRIBUTE("Unexpected attribute"),
    
    // Schema compliance errors
    SCHEMA_VIOLATION("General schema violation"),
    CONSTRAINT_VIOLATION("Constraint violation"),
    PATTERN_MISMATCH("Pattern mismatch"),
    
    // Content errors
    EMPTY_REQUIRED_CONTENT("Required content is empty"),
    INVALID_CONTENT_MODEL("Invalid content model"),
    MIXED_CONTENT_ERROR("Mixed content error"),
    
    // Namespace errors
    NAMESPACE_ERROR("Namespace error"),
    UNDEFINED_PREFIX("Undefined namespace prefix"),
    
    // Other errors
    UNKNOWN_ERROR("Unknown validation error");
    
    private final String description;
    
    ErrorType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}

