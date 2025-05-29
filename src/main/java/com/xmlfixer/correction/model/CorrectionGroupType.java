package com.xmlfixer.correction.model;
/**
 * Types of correction groups based on priority and impact
 */
public enum CorrectionGroupType {
    CRITICAL("Critical"),
    STRUCTURAL("Structural"),
    DATA_QUALITY("Data Quality"),
    OPTIONAL("Optional");

    private final String displayName;

    CorrectionGroupType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
