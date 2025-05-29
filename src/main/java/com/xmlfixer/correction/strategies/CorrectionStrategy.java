package com.xmlfixer.correction.strategies;

import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ErrorType;
import com.xmlfixer.validation.model.ValidationError;

import java.util.List;
import java.util.Map;

/**
 * Base interface for XML correction strategies
 */
public interface CorrectionStrategy {

    /**
     * Gets the name of this correction strategy
     */
    String getStrategyName();

    /**
     * Gets the priority of this strategy (lower number = higher priority)
     */
    int getPriority();

    /**
     * Gets the error types that this strategy can handle
     */
    List<ErrorType> getSupportedErrorTypes();

    /**
     * Generates correction actions for the given validation errors
     *
     * @param errorsByType Map of validation errors grouped by error type
     * @param schema The schema element tree for context
     * @return List of correction actions to fix the errors
     */
    List<CorrectionAction> generateCorrections(Map<ErrorType, List<ValidationError>> errorsByType,
                                               SchemaElement schema);

    /**
     * Determines if this strategy can handle the given error type
     */
    default boolean canHandle(ErrorType errorType) {
        return getSupportedErrorTypes().contains(errorType);
    }

    /**
     * Validates that a correction action is safe to apply
     *
     * @param action The correction action to validate
     * @param schema The schema context
     * @return true if the action is safe to apply
     */
    default boolean validateAction(CorrectionAction action, SchemaElement schema) {
        return action != null && action.getActionType() != null;
    }

    /**
     * Estimates the impact of applying corrections from this strategy
     *
     * @param actions List of actions to estimate
     * @return Impact score (0-100, where 0 is no impact and 100 is major structural change)
     */
    default int estimateImpact(List<CorrectionAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return 0;
        }

        int totalImpact = 0;
        for (CorrectionAction action : actions) {
            totalImpact += getActionImpact(action);
        }

        return Math.min(100, totalImpact);
    }

    /**
     * Gets the impact score for a single action
     */
    default int getActionImpact(CorrectionAction action) {
        switch (action.getActionType()) {
            case ADD_ELEMENT:
                return 20;
            case REMOVE_ELEMENT:
                return 25;
            case MOVE_ELEMENT:
                return 15;
            case REORDER_ELEMENTS:
                return 10;
            case MODIFY_ELEMENT:
                return 8;
            case ADD_ATTRIBUTE:
                return 5;
            case REMOVE_ATTRIBUTE:
                return 8;
            case MODIFY_ATTRIBUTE:
                return 3;
            case CHANGE_TEXT_CONTENT:
                return 2;
            case FIX_NAMESPACE:
                return 5;
            default:
                return 10;
        }
    }
}
