package com.xmlfixer.correction.strategies;

import com.xmlfixer.correction.DomManipulator;
import com.xmlfixer.correction.model.ActionType;
import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ErrorType;
import com.xmlfixer.validation.model.ValidationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy for correcting element ordering issues using minimal movement algorithms
 */
@Singleton
public class OrderingStrategy implements CorrectionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(OrderingStrategy.class);

    private static final List<ErrorType> SUPPORTED_ERROR_TYPES = Arrays.asList(
            ErrorType.INVALID_ELEMENT_ORDER,
            ErrorType.UNEXPECTED_ELEMENT
    );
    private final DomManipulator domManipulator;

    @Inject
    public OrderingStrategy(DomManipulator domManipulator) {
        logger.info("OrderingStrategy initialized");
        this.domManipulator = domManipulator;
    }

    @Override
    public String getStrategyName() {
        return "Element Ordering Correction";
    }

    @Override
    public int getPriority() {
        return 2; // High priority - structural organization
    }

    @Override
    public List<ErrorType> getSupportedErrorTypes() {
        return SUPPORTED_ERROR_TYPES;
    }

    @Override
    public List<CorrectionAction> generateCorrections(Map<ErrorType, List<ValidationError>> errorsByType,
                                                      SchemaElement schema) {
        List<CorrectionAction> actions = new ArrayList<>();

        // Handle invalid element order
        List<ValidationError> orderingErrors = errorsByType.get(ErrorType.INVALID_ELEMENT_ORDER);
        if (orderingErrors != null) {
            Map<String, List<ValidationError>> errorsByParent = groupErrorsByParent(orderingErrors);

            for (Map.Entry<String, List<ValidationError>> entry : errorsByParent.entrySet()) {
                String parentPath = entry.getKey();
                List<ValidationError> parentErrors = entry.getValue();

                List<CorrectionAction> orderingActions = createOrderingActions(parentPath, parentErrors, schema);
                actions.addAll(orderingActions);
            }
        }

        // Handle unexpected elements (may need repositioning)
        List<ValidationError> unexpectedElements = errorsByType.get(ErrorType.UNEXPECTED_ELEMENT);
        if (unexpectedElements != null) {
            for (ValidationError error : unexpectedElements) {
                CorrectionAction action = createRepositionAction(error, schema);
                if (action != null) {
                    actions.add(action);
                }
            }
        }

        logger.debug("Generated {} ordering correction actions", actions.size());
        return actions;
    }

    @Override
    public boolean canCorrect(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();
        // Handle both INVALID_ELEMENT_ORDER and UNEXPECTED_ELEMENT
        return errorType == ErrorType.INVALID_ELEMENT_ORDER ||
                errorType == ErrorType.UNEXPECTED_ELEMENT;
    }

    @Override
    public boolean applyCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();

        logger.debug("Correcting element ordering at path: {}", xPath);

        try {
            Element parentElement = domManipulator.findElement(document, xPath);
            if (parentElement == null) {
                logger.warn("Could not find parent element for ordering correction: {}", xPath);
                return false;
            }

            SchemaElement schemaElement = StrategyHelper.findSchemaElement(rootSchema, parentElement.getNodeName());
            if (schemaElement == null || !schemaElement.hasChildren()) {
                return false;
            }

            // Get desired order from schema
            List<String> desiredOrder = schemaElement.getChildren().stream()
                    .map(SchemaElement::getName)
                    .collect(Collectors.toList());

            return domManipulator.reorderElements(parentElement, desiredOrder);

        } catch (Exception e) {
            logger.error("Error correcting element ordering: {}", xPath, e);
            return false;
        }
    }

    /**
     * Creates correction actions to fix element ordering within a parent element
     */
    private List<CorrectionAction> createOrderingActions(String parentPath,
                                                         List<ValidationError> errors,
                                                         SchemaElement schema) {
        List<CorrectionAction> actions = new ArrayList<>();

        // Get the expected order from schema
        List<String> expectedOrder = getExpectedElementOrder(parentPath, schema);
        if (expectedOrder.isEmpty()) {
            logger.warn("Cannot determine expected order for parent: {}", parentPath);
            return actions;
        }

        // Get current order from validation errors
        List<String> currentOrder = getCurrentElementOrder(errors);
        if (currentOrder.isEmpty()) {
            return actions;
        }

        // Calculate minimal moves needed using longest common subsequence
        List<ElementMove> moves = calculateMinimalMoves(currentOrder, expectedOrder);

        // Create correction actions for each move
        for (ElementMove move : moves) {
            CorrectionAction action = createMoveAction(parentPath, move);
            if (action != null) {
                actions.add(action);
            }
        }

        logger.debug("Created {} move actions for parent: {}", actions.size(), parentPath);
        return actions;
    }

    /**
     * Gets the expected element order from schema definition
     */
    private List<String> getExpectedElementOrder(String parentPath, SchemaElement schema) {
        SchemaElement parentElement = findSchemaElementByPath(schema, parentPath);
        if (parentElement == null || !parentElement.hasChildren()) {
            return Collections.emptyList();
        }

        return parentElement.getChildren().stream()
                .map(SchemaElement::getName)
                .collect(Collectors.toList());
    }

    /**
     * Extracts current element order from validation errors
     */
    private List<String> getCurrentElementOrder(List<ValidationError> errors) {
        return errors.stream()
                .filter(e -> e.getElementName() != null)
                .sorted(Comparator.comparing(ValidationError::getLineNumber))
                .map(ValidationError::getElementName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Calculates minimal moves needed to transform current order to expected order
     * Uses a simplified algorithm based on longest common subsequence
     */
    private List<ElementMove> calculateMinimalMoves(List<String> current, List<String> expected) {
        List<ElementMove> moves = new ArrayList<>();

        // Create a working copy of current order
        List<String> working = new ArrayList<>(current);

        // For each expected position, check if element is in correct place
        for (int expectedPos = 0; expectedPos < expected.size(); expectedPos++) {
            String expectedElement = expected.get(expectedPos);

            // Find current position of this element
            int currentPos = working.indexOf(expectedElement);

            if (currentPos == -1) {
                // Element not found - might need to be added (handled by other strategies)
                continue;
            }

            if (currentPos != expectedPos) {
                // Element needs to be moved
                ElementMove move = new ElementMove();
                move.elementName = expectedElement;
                move.fromPosition = currentPos;
                move.toPosition = expectedPos;
                move.anchorElement = getAnchorElement(expected, expectedPos);
                moves.add(move);

                // Update working order to reflect the move
                working.remove(currentPos);
                working.add(expectedPos, expectedElement);

                logger.debug("Planned move: {} from position {} to {}",
                        expectedElement, currentPos, expectedPos);
            }
        }

        return moves;
    }

    /**
     * Gets an anchor element to position the moved element relative to
     */
    private String getAnchorElement(List<String> expectedOrder, int position) {
        if (position > 0) {
            return expectedOrder.get(position - 1); // Previous sibling
        } else if (position < expectedOrder.size() - 1) {
            return expectedOrder.get(position + 1); // Next sibling
        }
        return null;
    }

    /**
     * Creates a correction action for moving an element
     */
    private CorrectionAction createMoveAction(String parentPath, ElementMove move) {
        CorrectionAction action = new CorrectionAction(
                ActionType.MOVE_ELEMENT,
                String.format("Move element '%s' to correct position", move.elementName)
        );

        action.setElementName(move.elementName);
        action.setxPath(parentPath + "/" + move.elementName);
        action.setRelatedErrorType(ErrorType.INVALID_ELEMENT_ORDER);

        // Encode move information in old/new values
        action.setOldValue(String.valueOf(move.fromPosition));
        if (move.anchorElement != null) {
            action.setNewValue("after:" + move.anchorElement);
        } else {
            action.setNewValue(String.valueOf(move.toPosition));
        }

        return action;
    }

    /**
     * Creates an action to reposition an unexpected element
     */
    private CorrectionAction createRepositionAction(ValidationError error, SchemaElement schema) {
        String elementName = error.getElementName();
        if (elementName == null) {
            return null;
        }

        // Try to find a valid position for this element
        String validPosition = findValidPosition(elementName, error.getxPath(), schema);
        if (validPosition == null) {
            // Cannot reposition - might need to be removed (handled by other strategies)
            return null;
        }

        CorrectionAction action = new CorrectionAction(
                ActionType.MOVE_ELEMENT,
                String.format("Reposition unexpected element '%s'", elementName)
        );

        action.setElementName(elementName);
        action.setxPath(error.getxPath());
        action.setRelatedErrorType(ErrorType.UNEXPECTED_ELEMENT);
        action.setOldValue(error.getxPath());
        action.setNewValue(validPosition);

        return action;
    }

    /**
     * Finds a valid position for an element based on schema constraints
     */
    private String findValidPosition(String elementName, String currentPath, SchemaElement schema) {
        // Search for valid parent elements that can contain this element
        return searchValidParent(elementName, schema, "");
    }

    /**
     * Recursively searches for a valid parent that can contain the element
     */
    private String searchValidParent(String elementName, SchemaElement current, String currentPath) {
        if (current == null) {
            return null;
        }

        // Check if current element can contain the target element
        if (current.hasChildren()) {
            for (SchemaElement child : current.getChildren()) {
                if (child.getName().equals(elementName)) {
                    return currentPath.isEmpty() ? current.getName() : currentPath + "/" + current.getName();
                }
            }
        }

        // Recursively check children
        if (current.hasChildren()) {
            for (SchemaElement child : current.getChildren()) {
                String childPath = currentPath.isEmpty() ? child.getName() : currentPath + "/" + child.getName();
                String result = searchValidParent(elementName, child, childPath);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Groups validation errors by their parent element path
     */
    private Map<String, List<ValidationError>> groupErrorsByParent(List<ValidationError> errors) {
        return errors.stream()
                .filter(e -> e.getxPath() != null)
                .collect(Collectors.groupingBy(e -> getParentPath(e.getxPath())));
    }

    /**
     * Gets parent path from XPath
     */
    private String getParentPath(String xpath) {
        if (xpath == null || xpath.isEmpty()) {
            return "/";
        }

        int lastSlash = xpath.lastIndexOf('/');
        return lastSlash > 0 ? xpath.substring(0, lastSlash) : "/";
    }

    /**
     * Finds schema element by path
     */
    private SchemaElement findSchemaElementByPath(SchemaElement root, String path) {
        if (root == null || path == null || path.isEmpty() || "/".equals(path)) {
            return root;
        }

        String[] pathParts = path.split("/");
        SchemaElement current = root;

        for (String part : pathParts) {
            if (part.isEmpty()) continue;

            if (current.hasChildren()) {
                SchemaElement found = null;
                for (SchemaElement child : current.getChildren()) {
                    if (part.equals(child.getName())) {
                        found = child;
                        break;
                    }
                }
                if (found == null) {
                    return null;
                }
                current = found;
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Inner class representing an element move operation
     */
    private static class ElementMove {
        String elementName;
        int fromPosition;
        int toPosition;
        String anchorElement; // Element to position relative to

        @Override
        public String toString() {
            return String.format("Move[%s: %d->%d, anchor=%s]",
                    elementName, fromPosition, toPosition, anchorElement);
        }
    }
}
