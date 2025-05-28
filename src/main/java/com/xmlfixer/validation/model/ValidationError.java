package com.xmlfixer.validation.model;

/**
 * Represents a validation error or warning with location and context information
 */
public class ValidationError {
    
    private ErrorType errorType;
    private String message;
    private String xPath;
    private int lineNumber;
    private int columnNumber;
    private String elementName;
    private String expectedValue;
    private String actualValue;
    private String schemaRule;
    private Severity severity;
    
    public enum Severity {
        ERROR, WARNING, INFO
    }
    
    public ValidationError() {
        this.severity = Severity.ERROR;
        this.lineNumber = -1;
        this.columnNumber = -1;
    }
    
    public ValidationError(ErrorType errorType, String message) {
        this();
        this.errorType = errorType;
        this.message = message;
    }
    
    public ValidationError(ErrorType errorType, String message, int lineNumber, int columnNumber) {
        this(errorType, message);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }
    
    // Basic properties
    public ErrorType getErrorType() { return errorType; }
    public void setErrorType(ErrorType errorType) { this.errorType = errorType; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    
    // Location information
    public String getxPath() { return xPath; }
    public void setxPath(String xPath) { this.xPath = xPath; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public int getColumnNumber() { return columnNumber; }
    public void setColumnNumber(int columnNumber) { this.columnNumber = columnNumber; }
    
    public String getElementName() { return elementName; }
    public void setElementName(String elementName) { this.elementName = elementName; }
    
    // Schema context
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    
    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }
    
    public String getSchemaRule() { return schemaRule; }
    public void setSchemaRule(String schemaRule) { this.schemaRule = schemaRule; }
    
    // Utility methods
    public boolean hasLocation() {
        return lineNumber > 0 || (xPath != null && !xPath.isEmpty());
    }
    
    public String getLocationString() {
        if (lineNumber > 0 && columnNumber > 0) {
            return String.format("Line %d, Column %d", lineNumber, columnNumber);
        } else if (lineNumber > 0) {
            return String.format("Line %d", lineNumber);
        } else if (xPath != null && !xPath.isEmpty()) {
            return String.format("XPath: %s", xPath);
        }
        return "Unknown location";
    }
    
    public String getFullMessage() {
        StringBuilder sb = new StringBuilder();
        
        if (severity != null) {
            sb.append("[").append(severity).append("] ");
        }
        
        if (message != null) {
            sb.append(message);
        }
        
        if (hasLocation()) {
            sb.append(" (").append(getLocationString()).append(")");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("ValidationError{type=%s, severity=%s, message='%s', location='%s'}", 
            errorType, severity, message, getLocationString());
    }
}

