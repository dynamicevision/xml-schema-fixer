package com.xmlfixer.correction.strategies;

import com.xmlfixer.correction.model.*;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ValidationError;
import com.xmlfixer.validation.model.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Plans and organizes correction actions based on validation errors
 * Ensures corrections are applied in the correct order to avoid conflicts
 */
public class CorrectionPlanner {
    private static final Logger logger = LoggerFactory.getLogger(CorrectionPlanner.class);

    // Priority levels for different correction types
    private static final Map<ErrorType, Integer> ERROR_PRIORITIES = new HashMap<>();
    static {
        ERROR_PRIORITIES.put(ErrorType.MALFORMED_XML, 1);                    // Highest priority
        ERROR_PRIORITIES.put(ErrorType.MISSING_REQUIRED_ELEMENT, 2);
        ERROR_PRIORITIES.put(ErrorType.MISSING_REQUIRED_ATTRIBUTE, 3);
        ERROR_PRIORITIES.put(ErrorType.INVALID_ELEMENT_ORDER, 4);
        ERROR_PRIORITIES.put(ErrorType.TOO_FEW_OCCURRENCES, 5);
        ERROR_PRIORITIES.put(ErrorType.TOO_MANY_OCCURRENCES, 6);
        ERROR_PRIORITIES.put(ErrorType.EMPTY_REQUIRED_CONTENT, 7);
        ERROR_PRIORITIES.put(ErrorType.INVALID_DATA_TYPE, 8);
        ERROR_PRIORITIES.put(ErrorType.INVALID_FORMAT, 9);
        ERROR_PRIORITIES.put(ErrorType.PATTERN_MISMATCH, 10);
        ERROR_PRIORITIES.put(ErrorType.INVALID_VALUE_RANGE, 11);
        ERROR_PRIORITIES.put(ErrorType.INVALID_ATTRIBUTE_VALUE, 12);
        ERROR_PRIORITIES.put(ErrorType.INVALID_CONTENT_MODEL, 13);           // Lowest priority
    }

    /**
     * Creates a correction plan from validation errors
     */
    public CorrectionPlan createCorrectionPlan(List<ValidationError> errors, Document document,
                                               SchemaElement rootSchema) {
        logger.debug("Creating correction plan for {} validation errors", errors.size());

        CorrectionPlan plan = new CorrectionPlan();

        // Phase 1: Convert errors to correction actions
        List<CorrectionAction> correctionActions = convertErrorsToActions(errors);
        logger.debug("Converted {} errors to {} correction actions",
                errors.size(), correctionActions.size());

        // Phase 2: Analyze dependencies between actions
        Map<CorrectionAction, Set<CorrectionAction>> dependencies = analyzeDependencies(correctionActions);

        // Phase 3: Group actions by priority and dependencies
        List<CorrectionGroup> correctionGroups = groupCorrectionActions(correctionActions, dependencies);

        // Phase 4: Optimize correction order within groups
        optimizeCorrectionOrder(correctionGroups);

        plan.setCorrectionGroups(correctionGroups);
        plan.setTotalActions(correctionActions.size());

        logger.debug("Created correction plan with {} groups, {} total actions",
                correctionGroups.size(), correctionActions.size());

        return plan;
    }

    /**
     * Converts validation errors to correction actions
     */
    private List<CorrectionAction> convertErrorsToActions(List<ValidationError> errors) {
        List<CorrectionAction> actions = new ArrayList<>();

        for (ValidationError error : errors) {
            CorrectionAction action = createCorrectionAction(error);
            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    /**
     * Creates a correction action from a validation error
     */
    private CorrectionAction createCorrectionAction(ValidationError error) {
        CorrectionAction action = new CorrectionAction();

        action.setRelatedErrorType(error.getErrorType());
        action.setDescription(generateActionDescription(error));
        action.setxPath(error.getxPath());
        action.setElementName(error.getElementName());

        // Set action type based on error type
        ActionType actionType = mapErrorToActionType(error.getErrorType());
        action.setActionType(actionType);

        // Set old/new values if available
        if (error.getActualValue() != null) {
            action.setOldValue(error.getActualValue());
        }
        if (error.getExpectedValue() != null) {
            action.setNewValue(error.getExpectedValue());
        }

        return action;
    }

    /**
     * Maps error types to correction action types
     */
    private ActionType mapErrorToActionType(ErrorType errorType) {
        switch (errorType) {
            case MISSING_REQUIRED_ELEMENT:
                return ActionType.ADD_ELEMENT;
            case MISSING_REQUIRED_ATTRIBUTE:
                return ActionType.ADD_ATTRIBUTE;
            case INVALID_ELEMENT_ORDER:
                return ActionType.REORDER_ELEMENTS;
            case TOO_MANY_OCCURRENCES:
                return ActionType.REMOVE_ELEMENT;
            case TOO_FEW_OCCURRENCES:
                return ActionType.ADD_ELEMENT;
            case INVALID_DATA_TYPE:
            case INVALID_FORMAT:
            case PATTERN_MISMATCH:
            case INVALID_VALUE_RANGE:
                return ActionType.CHANGE_TEXT_CONTENT;
            case INVALID_ATTRIBUTE_VALUE:
                return ActionType.MODIFY_ATTRIBUTE;
            case EMPTY_REQUIRED_CONTENT:
                return ActionType.CHANGE_TEXT_CONTENT;
            default:
                return ActionType.MODIFY_ELEMENT;
        }
    }

    /**
     * Generates a human-readable description for the correction action
     */
    private String generateActionDescription(ValidationError error) {
        StringBuilder description = new StringBuilder();

        switch (error.getErrorType()) {
            case MISSING_REQUIRED_ELEMENT:
                description.append("Add missing required element '")
                        .append(error.getElementName()).append("'");
                break;
            case MISSING_REQUIRED_ATTRIBUTE:
                description.append("Add missing required attribute");
                break;
            case INVALID_ELEMENT_ORDER:
                description.append("Reorder elements to match schema sequence");
                break;
            case TOO_MANY_OCCURRENCES:
                description.append("Remove excess occurrences of element '")
                        .append(error.getElementName()).append("'");
                break;
            case TOO_FEW_OCCURRENCES:
                description.append("Add missing occurrences of element '")
                        .append(error.getElementName()).append("'");
                break;
            case INVALID_DATA_TYPE:
                description.append("Correct data type for element '")
                        .append(error.getElementName()).append("'");
                break;
            case INVALID_FORMAT:
                description.append("Fix format of content in element '")
                        .append(error.getElementName()).append("'");
                break;
            case PATTERN_MISMATCH:
                description.append("Correct pattern mismatch in element '")
                        .append(error.getElementName()).append("'");
                break;
            case INVALID_VALUE_RANGE:
                description.append("Adjust value to valid range for element '")
                        .append(error.getElementName()).append("'");
                break;
            case INVALID_ATTRIBUTE_VALUE:
                description.append("Correct invalid attribute value");
                break;
            case EMPTY_REQUIRED_CONTENT:
                description.append("Fill empty required content in element '")
                        .append(error.getElementName()).append("'");
                break;
            default:
                description.append("Fix ").append(error.getErrorType().getDescription().toLowerCase());
        }

        if (error.getxPath() != null && !error.getxPath().isEmpty()) {
            description.append(" at ").append(error.getxPath());
        }

        return description.toString();
    }

    /**
     * Analyzes dependencies between correction actions
     */
    private Map<CorrectionAction, Set<CorrectionAction>> analyzeDependencies(List<CorrectionAction> actions) {
        Map<CorrectionAction, Set<CorrectionAction>> dependencies = new HashMap<>();

        for (CorrectionAction action : actions) {
            Set<CorrectionAction> actionDependencies = new HashSet<>();

            for (CorrectionAction otherAction : actions) {
                if (action != otherAction && hasDependency(action, otherAction)) {
                    actionDependencies.add(otherAction);
                }
            }

            dependencies.put(action, actionDependencies);
        }

        return dependencies;
    }

    /**
     * Determines if one action depends on another
     */
    private boolean hasDependency(CorrectionAction action, CorrectionAction dependency) {
        // Same element path dependencies
        if (isSameElementPath(action.getxPath(), dependency.getxPath())) {
            return getActionPriority(dependency) < getActionPriority(action);
        }

        // Parent-child dependencies
        if (isParentChildRelationship(action.getxPath(), dependency.getxPath())) {
            // Parent must exist before child
            return isParentPath(dependency.getxPath(), action.getxPath()) &&
                    dependency.getActionType() == ActionType.ADD_ELEMENT;
        }

        // Ordering dependencies
        if (action.getActionType() == ActionType.REORDER_ELEMENTS) {
            // Reordering should happen after all elements are added
            return dependency.getActionType() == ActionType.ADD_ELEMENT &&
                    isChildOfPath(dependency.getxPath(), action.getxPath());
        }

        return false;
    }

    /**
     * Groups correction actions by priority and dependencies
     */
    private List<CorrectionGroup> groupCorrectionActions(List<CorrectionAction> actions,
                                                         Map<CorrectionAction, Set<CorrectionAction>> dependencies) {
        List<CorrectionGroup> groups = new ArrayList<>();
        Set<CorrectionAction> processedActions = new HashSet<>();

        // Create priority-based groups
        Map<Integer, List<CorrectionAction>> priorityGroups = actions.stream()
                .collect(Collectors.groupingBy(this::getActionPriority));

        // Process groups in priority order
        List<Integer> sortedPriorities = priorityGroups.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (Integer priority : sortedPriorities) {
            List<CorrectionAction> priorityActions = priorityGroups.get(priority);

            // Create correction group
            CorrectionGroup group = new CorrectionGroup();
            group.setGroupType(getGroupTypeForPriority(priority));
            group.setPriority(priority);
            group.setActions(priorityActions);
            group.setDescription(getGroupDescription(group.getGroupType()));

            groups.add(group);
            processedActions.addAll(priorityActions);
        }

        return groups;
    }

    /**
     * Gets the priority for a correction action
     */
    private int getActionPriority(CorrectionAction action) {
        return ERROR_PRIORITIES.getOrDefault(action.getRelatedErrorType(), 99);
    }

    /**
     * Gets the group type based on priority
     */
    private CorrectionGroupType getGroupTypeForPriority(int priority) {
        if (priority <= 3) {
            return CorrectionGroupType.CRITICAL;
        } else if (priority <= 7) {
            return CorrectionGroupType.STRUCTURAL;
        } else {
            return CorrectionGroupType.DATA_QUALITY;
        }
    }

    /**
     * Gets description for correction group type
     */
    private String getGroupDescription(CorrectionGroupType groupType) {
        switch (groupType) {
            case CRITICAL:
                return "Critical errors that must be fixed first";
            case STRUCTURAL:
                return "Structural corrections to match schema requirements";
            case DATA_QUALITY:
                return "Data quality improvements and format corrections";
            default:
                return "General corrections";
        }
    }

    /**
     * Optimizes the correction order within each group
     */
    private void optimizeCorrectionOrder(List<CorrectionGroup> groups) {
        for (CorrectionGroup group : groups) {
            List<CorrectionAction> optimizedOrder = optimizeGroupOrder(group.getActions());
            group.setActions(optimizedOrder);
        }
    }

    /**
     * Optimizes the order of actions within a group
     */
    private List<CorrectionAction> optimizeGroupOrder(List<CorrectionAction> actions) {
        // Sort by XPath depth (process parents before children)
        return actions.stream()
                .sorted(Comparator.comparing(action -> getPathDepth(action.getxPath())))
                .collect(Collectors.toList());
    }

    // Helper methods

    private boolean isSameElementPath(String path1, String path2) {
        return path1 != null && path2 != null && path1.equals(path2);
    }

    private boolean isParentChildRelationship(String path1, String path2) {
        return isParentPath(path1, path2) || isParentPath(path2, path1);
    }

    private boolean isParentPath(String parentPath, String childPath) {
        if (parentPath == null || childPath == null) {
            return false;
        }
        return childPath.startsWith(parentPath + "/");
    }

    private boolean isChildOfPath(String childPath, String parentPath) {
        return isParentPath(parentPath, childPath);
    }

    private int getPathDepth(String xpath) {
        if (xpath == null || xpath.isEmpty()) {
            return 0;
        }
        return xpath.split("/").length - 1;
    }
}
