package com.xmlfixer.common.exceptions;

/**
 * Exception thrown during XML validation operations
 */
public class ValidationException extends XmlFixerException {
    
    public ValidationException() {
        super();
    }
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ValidationException(Throwable cause) {
        super(cause);
    }
    
    public ValidationException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public ValidationException(String message, String errorCode, Object... parameters) {
        super(message, errorCode, parameters);
    }
}

