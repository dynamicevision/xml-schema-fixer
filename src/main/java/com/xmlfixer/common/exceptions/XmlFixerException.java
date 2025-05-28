package com.xmlfixer.common.exceptions;

/**
 * Base exception for all XML Fixer application errors
 */
public class XmlFixerException extends RuntimeException {
    
    private String errorCode;
    private Object[] parameters;
    
    public XmlFixerException() {
        super();
    }
    
    public XmlFixerException(String message) {
        super(message);
    }
    
    public XmlFixerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public XmlFixerException(Throwable cause) {
        super(cause);
    }
    
    public XmlFixerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public XmlFixerException(String message, String errorCode, Object... parameters) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = parameters;
    }
    
    public XmlFixerException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (errorCode != null) {
            sb.append(" [Error Code: ").append(errorCode).append("]");
        }
        return sb.toString();
    }
}

