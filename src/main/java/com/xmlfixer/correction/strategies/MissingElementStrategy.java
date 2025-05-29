package com.xmlfixer.correction.strategies;

import com.xmlfixer.correction.DomManipulator;
import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.schema.model.ElementConstraint;
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

/**
 * Strategy for resolving missing required elements by adding them with appropriate default values
 */
@Singleton
public class MissingElementStrategy implements CorrectionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MissingElementStrategy.class);
    private final DomManipulator domManipulator;
    private static final List<ErrorType> SUPPORTED_ERROR_TYPES = Arrays.asList(
            ErrorType.MISSING_REQUIRED_ELEMENT,
            ErrorType.TOO_FEW_OCCURRENCES
    );

    @Inject
    public MissingElementStrategy(DomManipulator domManipulator) {
        logger.info("MissingElementStrategy initialized");
        this.domManipulator = domManipulator;
    }

    @Override
    public String getStrategyName() {
        return "Missing Element Resolution";
    }

    @Override
    public int getPriority() {
        return 1; // Highest priority - structural fixes
    }

    @Override
    public List<ErrorType> getSupportedErrorTypes() {
        return SUPPORTED_ERROR_TYPES;
    }

    @Override
    public List<CorrectionAction> generateCorrections(Map<ErrorType, List<ValidationError>> errorsByType,
                                                      SchemaElement schema) {
        List<CorrectionAction> actions = new ArrayList<>();

        // Handle missing required elements
        List<ValidationError> missingElements = errorsByType.get(ErrorType.MISSING_REQUIRED_ELEMENT);
        if (missingElements != null) {
            for (ValidationError error : missingElements) {
                CorrectionAction action = createAddElementAction(error, schema);
                if (action != null) {
                    actions.add(action);
                }
            }
        }

        // Handle too few occurrences (need to add more elements)
        List<ValidationError> tooFewOccurrences = errorsByType.get(ErrorType.TOO_FEW_OCCURRENCES);
        if (tooFewOccurrences != null) {
            for (ValidationError error : tooFewOccurrences) {
                List<CorrectionAction> cardinalityActions = createCardinalityAddActions(error, schema);
                actions.addAll(cardinalityActions);
            }
        }

        logger.debug("Generated {} missing element correction actions", actions.size());
        return actions;
    }

    @Override
    public boolean canCorrect(CorrectionAction action, Document document, SchemaElement rootSchema) {
        return action.getRelatedErrorType() == ErrorType.MISSING_REQUIRED_ELEMENT;
    }

    @Override
    public boolean applyCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String elementName = action.getElementName();
        String xPath = action.getxPath();

        logger.debug("Adding missing required element: {} at path: {}", elementName, xPath);

        try {
            Element parentElement = StrategyHelper.findParentElement(document, xPath, elementName);
            if (parentElement == null) {
                logger.warn("Could not find parent element for missing element: {}", elementName);
                return false;
            }

            SchemaElement schemaElement = findSchemaElement(rootSchema, elementName);
            Element newElement = createElementFromSchema(document, schemaElement);

            if (newElement != null) {
                parentElement.appendChild(newElement);
                logger.debug("Successfully added missing element: {}", elementName);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Error adding missing element: {}", elementName, e);
            return false;
        }
    }

    private Element createElementFromSchema(Document document, SchemaElement schemaElement) {
        if (schemaElement == null) {
            return null;
        }

        String elementName = schemaElement.getName();
        String defaultValue = schemaElement.getDefaultValue();

        Element element = domManipulator.createElement(document, elementName, defaultValue);

        // Add required child elements
        if (schemaElement.hasChildren()) {
            for (SchemaElement child : schemaElement.getChildren()) {
                if (child.isRequired()) {
                    Element childElement = createElementFromSchema(document, child);
                    if (childElement != null) {
                        element.appendChild(childElement);
                    }
                }
            }
        }

        return element;
    }

    /**
     * Creates a correction action to add a missing required element
     */
    private CorrectionAction createAddElementAction(ValidationError error, SchemaElement schema) {
        String elementName = error.getElementName();
        String parentPath = getParentPath(error.getxPath());

        if (elementName == null || elementName.isEmpty()) {
            logger.warn("Cannot create add element action - missing element name");
            return null;
        }

        // Find the schema definition for this element
        SchemaElement elementSchema = findSchemaElement(schema, elementName);
        if (elementSchema == null) {
            logger.warn("Cannot find schema definition for element: {}", elementName);
            return null;
        }

        CorrectionAction action = new CorrectionAction(
                CorrectionAction.ActionType.ADD_ELEMENT,
                "Add missing required element: " + elementName
        );

        action.setElementName(elementName);
        action.setxPath(parentPath);
        action.setRelatedErrorType(error.getErrorType());

        // Determine the best insertion position
        String insertionPosition = determineInsertionPosition(parentPath, elementName, schema);
        action.setNewValue(insertionPosition);

        // Generate appropriate default content for the element
        String defaultContent = generateDefaultContent(elementSchema);
        if (defaultContent != null) {
            action.setOldValue(""); // No old value since element is missing
            action.setNewValue(defaultContent);
        }

        logger.debug("Created add element action for: {} at path: {}", elementName, parentPath);
        return action;
    }

    /**
     * Creates correction actions to add multiple occurrences for cardinality violations
     */
    private List<CorrectionAction> createCardinalityAddActions(ValidationError error, SchemaElement schema) {
        List<CorrectionAction> actions = new ArrayList<>();

        String elementName = error.getElementName();
        if (elementName == null) {
            return actions;
        }

        // Parse expected vs actual occurrences from error message
        CardinalityInfo info = parseCardinalityInfo(error);
        if (info == null || info.actualCount >= info.minRequired) {
            return actions;
        }

        int elementsToAdd = info.minRequired - info.actualCount;
        logger.debug("Need to add {} more occurrences of element: {}", elementsToAdd, elementName);

        // Create multiple add actions
        for (int i = 0; i < elementsToAdd; i++) {
            CorrectionAction action = createAddElementAction(error, schema);
            if (action != null) {
                action.setDescription(String.format("Add element '%s' (occurrence %d of %d needed)",
                        elementName, i + 1, elementsToAdd));
                actions.add(action);
            }
        }

        return actions;
    }

    /**
     * Determines the best position to insert a new element based on schema order
     */
    private String determineInsertionPosition(String parentPath, String elementName, SchemaElement schema) {
        // Find the parent element in the schema
        SchemaElement parentSchema = findSchemaElementByPath(schema, parentPath);
        if (parentSchema == null || !parentSchema.hasChildren()) {
            return "last"; // Default to end if no schema guidance
        }

        // Find the position of this element in the schema sequence
        List<SchemaElement> children = parentSchema.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getName().equals(elementName)) {
                // Found the element - determine insertion position relative to siblings
                if (i == 0) {
                    return "first";
                } else if (i == children.size() - 1) {
                    return "last";
                } else {
                    // Insert after the previous sibling if it exists, otherwise before next sibling
                    String previousSibling = children.get(i - 1).getName();
                    return "after:" + previousSibling;
                }
            }
        }

        return "last"; // Default fallback
    }

    /**
     * Generates appropriate default content for a new element based on its schema definition
     */
    private String generateDefaultContent(SchemaElement elementSchema) {
        // Check if element has a default value defined in schema
        if (elementSchema.getDefaultValue() != null) {
            return elementSchema.getDefaultValue();
        }

        // Generate content based on element type
        String type = elementSchema.getType();
        if (type != null) {
            return generateDefaultValueForType(type, elementSchema);
        }

        // For complex types, create empty element with required children
        if (elementSchema.hasChildren()) {
            return generateDefaultComplexContent(elementSchema);
        }

        // Default empty content
        return "";
    }

    /**
     * Generates default value based on XML Schema data type
     */
    private String generateDefaultValueForType(String type, SchemaElement elementSchema) {
        // Remove namespace prefix if present
        String baseType = type.contains(":") ? type.split(":")[1] : type;

        // Check constraints for specific default values
        if (elementSchema.hasConstraints()) {
            String constraintDefault = getDefaultFromConstraints(elementSchema.getConstraints());
            if (constraintDefault != null) {
                return constraintDefault;
            }
        }

        // Generate appropriate defaults for common XSD types
        switch (baseType.toLowerCase()) {
            case "string":
                return "";
            case "int":
            case "integer":
            case "positiveinteger":
            case "nonnegativeinteger":
                return "0";
            case "decimal":
            case "double":
            case "float":
                return "0.0";
            case "boolean":
                return "false";
            case "date":
                return "1970-01-01";
            case "time":
                return "00:00:00";
            case "datetime":
                return "1970-01-01T00:00:00";
            default:
                return ""; // Safe default for unknown types
        }
    }

    /**
     * Gets default value from element constraints (e.g., enumeration, ranges)
     */
    private String getDefaultFromConstraints(List<ElementConstraint> constraints) {
        for (ElementConstraint constraint : constraints) {
            switch (constraint.getConstraintType()) {
                case ENUMERATION:
                    // Use first enumerated value
                    String enumValues = constraint.getValue();
                    if (enumValues != null && !enumValues.isEmpty()) {
                        String[] values = enumValues.split(",");
                        return values[0].trim();
                    }
                    break;
                case MIN_INCLUSIVE:
                    // Use minimum value if specified
                    return constraint.getValue();
                case MIN_EXCLUSIVE:
                    // Use minimum + 1 for exclusive bounds
                    try {
                        double min = Double.parseDouble(constraint.getValue());
                        return String.valueOf((int)(min + 1));
                    } catch (NumberFormatException e) {
                        // Ignore and continue
                    }
                    break;
            }
        }
        return null;
    }

    /**
     * Generates default content for complex type elements with required children
     */
    private String generateDefaultComplexContent(SchemaElement elementSchema) {
        StringBuilder content = new StringBuilder();

        // Add required child elements
        if (elementSchema.hasChildren()) {
            for (SchemaElement child : elementSchema.getChildren()) {
                if (child.isRequired()) {
                    String childContent = generateDefaultContent(child);
                    content.append("<").append(child.getName()).append(">");
                    content.append(childContent);
                    content.append("</").append(child.getName()).append(">");
                }
            }
        }

        return content.toString();
    }

    /**
     * Parses cardinality information from validation error message
     */
    private CardinalityInfo parseCardinalityInfo(ValidationError error) {
        // This is a simplified parser - in a real implementation,
        // this would parse the actual error message or use structured error data
        CardinalityInfo info = new CardinalityInfo();
        info.actualCount = 0; // Would be parsed from error
        info.minRequired = 1; // Would be parsed from error or schema
        info.maxAllowed = Integer.MAX_VALUE;

        return info;
    }

    /**
     * Helper method to get parent path from full XPath
     */
    private String getParentPath(String xpath) {
        if (xpath == null || xpath.isEmpty()) {
            return "/";
        }

        int lastSlash = xpath.lastIndexOf('/');
        return lastSlash > 0 ? xpath.substring(0, lastSlash) : "/";
    }

    /**
     * Finds a schema element by name in the schema tree
     */
    private SchemaElement findSchemaElement(SchemaElement root, String elementName) {
        if (root == null || elementName == null) {
            return null;
        }

        if (elementName.equals(root.getName())) {
            return root;
        }

        if (root.hasChildren()) {
            for (SchemaElement child : root.getChildren()) {
                SchemaElement found = findSchemaElement(child, elementName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Finds a schema element by XPath in the schema tree
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
     * Inner class for cardinality information
     */
    private static class CardinalityInfo {
        int actualCount;
        int minRequired;
        int maxAllowed;
    }
}
