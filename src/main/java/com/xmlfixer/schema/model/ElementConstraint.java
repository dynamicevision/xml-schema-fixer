package com.xmlfixer.schema.model;

/**
 * Represents constraints on schema elements (patterns, ranges, enumerations, etc.)
 */
public class ElementConstraint {
    
    public enum ConstraintType {
        PATTERN("Pattern constraint"),
        ENUMERATION("Enumeration constraint"),
        MIN_LENGTH("Minimum length constraint"),
        MAX_LENGTH("Maximum length constraint"),
        MIN_INCLUSIVE("Minimum inclusive value"),
        MAX_INCLUSIVE("Maximum inclusive value"),
        MIN_EXCLUSIVE("Minimum exclusive value"),
        MAX_EXCLUSIVE("Maximum exclusive value"),
        TOTAL_DIGITS("Total digits constraint"),
        FRACTION_DIGITS("Fraction digits constraint"),
        WHITE_SPACE("Whitespace constraint");
        
        private final String description;
        
        ConstraintType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private ConstraintType constraintType;
    private String value;
    private String description;
    private boolean required;
    
    public ElementConstraint() {
        this.required = false;
    }
    
    public ElementConstraint(ConstraintType constraintType, String value) {
        this();
        this.constraintType = constraintType;
        this.value = value;
    }
    
    public ElementConstraint(ConstraintType constraintType, String value, String description) {
        this(constraintType, value);
        this.description = description;
    }
    
    // Basic properties
    public ConstraintType getConstraintType() { return constraintType; }
    public void setConstraintType(ConstraintType constraintType) { this.constraintType = constraintType; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    // Utility methods
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        
        if (constraintType != null) {
            sb.append(constraintType.getDescription());
        }
        
        if (value != null && !value.isEmpty()) {
            sb.append(": ").append(value);
        }
        
        if (description != null && !description.isEmpty()) {
            sb.append(" (").append(description).append(")");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("ElementConstraint{type=%s, value='%s', required=%s}", 
            constraintType, value, required);
    }
}

