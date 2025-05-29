package com.xmlfixer.correction.model;

import com.xmlfixer.validation.model.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a comprehensive plan for correcting XML validation errors
 */
public class CorrectionPlan {

    private static final Logger logger = LoggerFactory.getLogger(CorrectionPlan.class);

    private List<CorrectionAction> actions;
    private Map<String, List<CorrectionAction>> actionsByPath;
    private Map<CorrectionAction.ActionType, List<CorrectionAction>> actionsByType;
    private Set<String> affectedPaths;
    private int priority;

    // Action priority mapping (lower number = higher priority)
    private static final Map<CorrectionAction.ActionType, Integer> ACTION_PRIORITIES = new HashMap<>();
    static {
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.ADD_ELEMENT, 1);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.ADD_ATTRIBUTE, 2);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.REORDER_ELEMENTS, 3);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.MOVE_ELEMENT, 4);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.MODIFY_ELEMENT, 5);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.MODIFY_ATTRIBUTE, 6);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.CHANGE_TEXT_CONTENT, 7);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.REMOVE_ATTRIBUTE, 8);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.REMOVE_ELEMENT, 9);
        ACTION_PRIORITIES.put(CorrectionAction.ActionType.FIX_NAMESPACE, 10);
    }

    public CorrectionPlan() {
        this.actions = new ArrayList<>();
        this.actionsByPath = new HashMap<>();
        this.actionsByType = new HashMap<>();
        this.affectedPaths = new HashSet<>();
        this.priority = 0;
    }

    /**
     * Adds a single correction action to the plan
     */
    public void addAction(CorrectionAction action) {
        if (action == null) {
            return;
        }

        actions.add(action);

        // Index by path
        String path = action.getxPath();
        if (path != null) {
            actionsByPath.computeIfAbsent(path, k -> new ArrayList<>()).add(action);
            affectedPaths.add(path);
        }

        // Index by type
        actionsByType.computeIfAbsent(action.getActionType(), k -> new ArrayList<>()).add(action);

        logger.debug("Added action to plan: {} at path: {}", action.getActionType(), path);
    }

    /**
     * Adds multiple correction actions to the plan
     */
    public void addActions(List<CorrectionAction> newActions) {
        if (newActions != null) {
            for (CorrectionAction action : newActions) {
                addAction(action);
            }
        }
    }

    /**
     * Sorts actions by priority (structural fixes first, then data quality)
     */
    public void sortActionsByPriority() {
        actions.sort((a1, a2) -> {
            // First compare by action type priority
            int priority1 = ACTION_PRIORITIES.getOrDefault(a1.getActionType(), 100);
            int priority2 = ACTION_PRIORITIES.getOrDefault(a2.getActionType(), 100);

            if (priority1 != priority2) {
                return Integer.compare(priority1, priority2);
            }

            // Then by error type severity (if available)
            if (a1.getRelatedErrorType() != null && a2.getRelatedErrorType() != null) {
                int severity1 = getErrorTypeSeverity(a1.getRelatedErrorType());
                int severity2 = getErrorTypeSeverity(a2.getRelatedErrorType());
                if (severity1 != severity2) {
                    return Integer.compare(severity1, severity2);
                }
            }

            // Finally by path depth (deeper elements first to avoid parent modification issues)
            String path1 = a1.getxPath() != null ? a1.getxPath() : "";
            String path2 = a2.getxPath() != null ? a2.getxPath() : "";
            int depth1 = path1.split("/").length;
            int depth2 = path2.split("/").length;

            return Integer.compare(depth2, depth1); // Reverse order - deeper first
        });

        logger.debug("Sorted {} actions by priority", actions.size());
    }

    /**
     * Gets severity ranking for error types (lower = more severe)
     */
    private int getErrorTypeSeverity(ErrorType errorType) {
        switch (errorType) {
            case MALFORMED_XML:
            case MISSING_REQUIRED_ELEMENT:
            case MISSING_REQUIRED_ATTRIBUTE:
                return 1; // Critical
            case INVALID_ELEMENT_ORDER:
            case TOO_FEW_OCCURRENCES:
            case TOO_MANY_OCCURRENCES:
                return 2; // High
            case INVALID_DATA_TYPE:
            case PATTERN_MISMATCH:
            case CONSTRAINT_VIOLATION:
                return 3; // Medium
            case INVALID_FORMAT:
            case EMPTY_REQUIRED_CONTENT:
                return 4; // Low
            default:
                return 5; // Lowest
        }
    }

    /**
     * Removes conflicting actions that would interfere with each other
     */
    public void removeConflictingActions() {
        List<CorrectionAction> conflictsToRemove = new ArrayList<>();

        // Check for conflicts between actions on the same path
        for (Map.Entry<String, List<CorrectionAction>> entry : actionsByPath.entrySet()) {
            String path = entry.getKey();
            List<CorrectionAction> pathActions = entry.getValue();

            if (pathActions.size() > 1) {
                resolvePathConflicts(path, pathActions, conflictsToRemove);
            }
        }

        // Remove conflicting actions
        for (CorrectionAction conflict : conflictsToRemove) {
            removeAction(conflict);
        }

        if (!conflictsToRemove.isEmpty()) {
            logger.debug("Removed {} conflicting actions", conflictsToRemove.size());
        }
    }

    /**
     * Resolves conflicts between actions targeting the same path
     */
    private void resolvePathConflicts(String path, List<CorrectionAction> pathActions,
                                      List<CorrectionAction> conflictsToRemove) {

        // Check for add/remove conflicts
        boolean hasAdd = pathActions.stream().anyMatch(a ->
                a.getActionType() == CorrectionAction.ActionType.ADD_ELEMENT);
        boolean hasRemove = pathActions.stream().anyMatch(a ->
                a.getActionType() == CorrectionAction.ActionType.REMOVE_ELEMENT);

        if (hasAdd && hasRemove) {
            // Keep add, remove remove (structural fixes take priority)
            pathActions.stream()
                    .filter(a -> a.getActionType() == CorrectionAction.ActionType.REMOVE_ELEMENT)
                    .forEach(conflictsToRemove::add);
        }

        // Check for multiple modifications of the same element
        List<CorrectionAction> modifications = pathActions.stream()
                .filter(a -> a.getActionType() == CorrectionAction.ActionType.MODIFY_ELEMENT ||
                        a.getActionType() == CorrectionAction.ActionType.CHANGE_TEXT_CONTENT)
                .collect(Collectors.toList());

        if (modifications.size() > 1) {
            // Keep only the first modification (by priority order)
            modifications.stream().skip(1).forEach(conflictsToRemove::add);
        }
    }

    /**
     * Groups related actions for batch processing efficiency
     */
    public void groupRelatedActions() {
        // Group actions by parent element for batch processing
        Map<String, List<CorrectionAction>> parentGroups = new HashMap<>();

        for (CorrectionAction action : actions) {
            String path = action.getxPath();
            if (path != null) {
                String parentPath = getParentPath(path);
                parentGroups.computeIfAbsent(parentPath, k -> new ArrayList<>()).add(action);
            }
        }

        // Log grouping information
        int groups = parentGroups.size();
        logger.debug("Grouped {} actions into {} parent element groups for batch processing",
                actions.size(), groups);
    }

    /**
     * Gets the parent path from an XPath
     */
    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "";
    }

    /**
     * Removes an action from the plan and all indices
     */
    private void removeAction(CorrectionAction action) {
        actions.remove(action);

        // Remove from path index
        String path = action.getxPath();
        if (path != null) {
            List<CorrectionAction> pathActions = actionsByPath.get(path);
            if (pathActions != null) {
                pathActions.remove(action);
                if (pathActions.isEmpty()) {
                    actionsByPath.remove(path);
                    affectedPaths.remove(path);
                }
            }
        }

        // Remove from type index
        List<CorrectionAction> typeActions = actionsByType.get(action.getActionType());
        if (typeActions != null) {
            typeActions.remove(action);
            if (typeActions.isEmpty()) {
                actionsByType.remove(action.getActionType());
            }
        }
    }

    // Getters and utility methods
    public List<CorrectionAction> getActions() { return new ArrayList<>(actions); }
    public int getActionCount() { return actions.size(); }
    public Set<String> getAffectedPaths() { return new HashSet<>(affectedPaths); }
    public int getAffectedPathCount() { return affectedPaths.size(); }

    public List<CorrectionAction> getActionsByPath(String path) {
        return actionsByPath.getOrDefault(path, Collections.emptyList());
    }

    public List<CorrectionAction> getActionsByType(CorrectionAction.ActionType type) {
        return actionsByType.getOrDefault(type, Collections.emptyList());
    }

    public boolean isEmpty() { return actions.isEmpty(); }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    /**
     * Gets plan summary statistics
     */
    public PlanSummary getSummary() {
        PlanSummary summary = new PlanSummary();
        summary.totalActions = actions.size();
        summary.affectedPaths = affectedPaths.size();

        // Count by action type
        summary.actionsByType = new HashMap<>();
        for (CorrectionAction.ActionType type : CorrectionAction.ActionType.values()) {
            int count = actionsByType.getOrDefault(type, Collections.emptyList()).size();
            if (count > 0) {
                summary.actionsByType.put(type, count);
            }
        }

        return summary;
    }

    @Override
    public String toString() {
        return String.format("CorrectionPlan{actions=%d, paths=%d, priority=%d}",
                actions.size(), affectedPaths.size(), priority);
    }

    /**
     * Inner class for plan summary information
     */
    public static class PlanSummary {
        public int totalActions;
        public int affectedPaths;
        public Map<CorrectionAction.ActionType, Integer> actionsByType;

        @Override
        public String toString() {
            return String.format("PlanSummary{total=%d, paths=%d, types=%s}",
                    totalActions, affectedPaths, actionsByType);
        }
    }
}
