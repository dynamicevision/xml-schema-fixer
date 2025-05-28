package com.xmlfixer.correction.model;

import com.xmlfixer.validation.model.ErrorType;

/**
 * Represents a single correction action applied to an XML document
 */
public class CorrectionAction {
    
    public enum ActionType {
        ADD_ELEMENT("Add element"),
        REMOVE_ELEMENT("Remove element"),
        MOVE_ELEMENT("Move element"),
        MODIFY_ELEMENT("Modify element"),
        ADD_ATTRIBUTE("Add attribute"),
        REMOVE_ATTRIBUTE("Remove attribute"),
        MODIFY_ATTRIBUTE("Modify attribute"),
        CHANGE_TEXT_CONTENT("Change text content"),
        REORDER_ELEMENTS("Reorder elements"),
        FIX_NAMESPACE("Fix namespace");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private ActionType actionType;
    private String description;
    private String xPath;
    private String elementName;
    private String oldValue;
    private String newValue;
    private ErrorType relatedErrorType;
    private boolean applied;
    private String failureReason;
    
    public CorrectionAction() {
        this.applied = false;
    }
    
    public CorrectionAction(ActionType actionType, String description) {
        this();
        this.actionType = actionType;
        this.description = description;
    }
    
    // Basic properties
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    // Location information
    public String getxPath() { return xPath; }
    public void setxPath(String xPath) { this.xPath = xPath; }
    
    public String getElementName() { return elementName; }
    public void setElementName(String elementName) { this.elementName = elementName; }
    
    // Value changes
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    
    // Error context
    public ErrorType getRelatedErrorType() { return relatedErrorType; }
    public void setRelatedErrorType(ErrorType relatedErrorType) { 
        this.relatedErrorType = relatedErrorType; 
    }
    
    // Execution status
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { 
        this.failureReason = failureReason;
        if (failureReason != null) {
            this.applied = false;
        }
    }
    
    // Utility methods
    public boolean isSuccessful() {
        return applied && failureReason == null;
    }
    
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        
        if (actionType != null) {
            sb.append(actionType.getDescription());
        }
        
        if (elementName != null) {
            sb.append(" '").append(elementName).append("'");
        }
        
        if (description != null && !description.isEmpty()) {
            sb.append(": ").append(description);
        }
        
        if (xPath != null && !xPath.isEmpty()) {
            sb.append(" (at ").append(xPath).append(")");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("CorrectionAction{type=%s, applied=%s, element='%s', xpath='%s'}", 
            actionType, applied, elementName, xPath);
    }
}

