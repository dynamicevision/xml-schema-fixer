package com.xmlfixer.common.exceptions;

/**
 * Exception thrown during XML correction operations
 */
public class CorrectionException extends XmlFixerException {
    
    public CorrectionException() {
        super();
    }
    
    public CorrectionException(String message) {
        super(message);
    }
    
    public CorrectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public CorrectionException(Throwable cause) {
        super(cause);
    }
    
    public CorrectionException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public CorrectionException(String message, String errorCode, Object... parameters) {
        super(message, errorCode, parameters);
    }
}

