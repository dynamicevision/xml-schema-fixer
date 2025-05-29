package com.xmlfixer.correction.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of related correction actions
 */
public class CorrectionGroup {
    private CorrectionGroupType groupType;
    private int priority;
    private String description;
    private List<CorrectionAction> actions;

    public CorrectionGroup() {
        this.actions = new ArrayList<>();
    }

    // Getters and setters
    public CorrectionGroupType getGroupType() { return groupType; }
    public void setGroupType(CorrectionGroupType groupType) { this.groupType = groupType; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<CorrectionAction> getActions() { return actions; }
    public void setActions(List<CorrectionAction> actions) { this.actions = actions; }

    public int getActionCount() { return actions != null ? actions.size() : 0; }

    @Override
    public String toString() {
        return String.format("CorrectionGroup{type=%s, priority=%d, actions=%d}",
                groupType, priority, getActionCount());
    }
}
