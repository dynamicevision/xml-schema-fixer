package com.xmlfixer.correction.strategies;

import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ErrorType;
import com.xmlfixer.validation.model.ValidationError;
import com.xmlfixer.correction.DomManipulator;
import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
/**
 * Strategy for correcting cardinality violations (too few/many element occurrences)
 */
public class CardinalityStrategy implements CorrectionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(CardinalityStrategy.class);
    private final DomManipulator domManipulator;

    public CardinalityStrategy(DomManipulator domManipulator) {
        this.domManipulator = domManipulator;
    }

    @Override
    public boolean canCorrect(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();
        return errorType == ErrorType.TOO_FEW_OCCURRENCES ||
                errorType == ErrorType.TOO_MANY_OCCURRENCES;
    }

    @Override
    public boolean applyCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();

        if (errorType == ErrorType.TOO_FEW_OCCURRENCES) {
            return handleTooFewOccurrences(action, document, rootSchema);
        } else if (errorType == ErrorType.TOO_MANY_OCCURRENCES) {
            return handleTooManyOccurrences(action, document, rootSchema);
        }

        return false;
    }

    private boolean handleTooFewOccurrences(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String elementName = action.getElementName();
        String xPath = action.getxPath();

        logger.debug("Handling too few occurrences for element: {} at path: {}", elementName, xPath);

        try {
            // Find the parent element where missing elements should be added
            Element parentElement = StrategyHelper.findParentElement(document, xPath, elementName);
            if (parentElement == null) {
                logger.warn("Could not find parent element for path: {}", xPath);
                return false;
            }

            // Find schema element to determine how many elements to add
            SchemaElement schemaElement = StrategyHelper.findSchemaElement(rootSchema, elementName);
            if (schemaElement == null) {
                logger.warn("Could not find schema element for: {}", elementName);
                return false;
            }

            // Count current occurrences
            int currentCount = domManipulator.countChildElements(parentElement, elementName);
            int requiredMinimum = schemaElement.getMinOccurs();
            int elementsToAdd = Math.max(0, requiredMinimum - currentCount);

            logger.debug("Current count: {}, Required minimum: {}, Elements to add: {}",
                    currentCount, requiredMinimum, elementsToAdd);

            // Add missing elements
            for (int i = 0; i < elementsToAdd; i++) {
                Element newElement = createDefaultElement(document, schemaElement);
                if (newElement != null) {
                    // Find the best insertion point
                    Element insertionPoint = findBestInsertionPoint(parentElement, elementName, schemaElement);
                    if (insertionPoint != null) {
                        domManipulator.insertElement(newElement, insertionPoint, DomManipulator.InsertPosition.AFTER);
                    } else {
                        parentElement.appendChild(newElement);
                    }

                    logger.debug("Added missing element: {} (instance {})", elementName, i + 1);
                } else {
                    logger.warn("Failed to create default element for: {}", elementName);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("Error handling too few occurrences for element: {}", elementName, e);
            return false;
        }
    }

    private boolean handleTooManyOccurrences(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String elementName = action.getElementName();
        String xPath = action.getxPath();

        logger.debug("Handling too many occurrences for element: {} at path: {}", elementName, xPath);

        try {
            // Find the parent element containing excess elements
            Element parentElement = StrategyHelper.findParentElement(document, xPath, elementName);
            if (parentElement == null) {
                logger.warn("Could not find parent element for path: {}", xPath);
                return false;
            }

            // Find schema element to determine maximum allowed
            SchemaElement schemaElement = StrategyHelper.findSchemaElement(rootSchema, elementName);
            if (schemaElement == null) {
                logger.warn("Could not find schema element for: {}", elementName);
                return false;
            }

            // Get all child elements with this name
            List<Element> childElements = domManipulator.getChildElements(parentElement, elementName);
            int currentCount = childElements.size();
            int maxAllowed = schemaElement.getMaxOccurs();
            int elementsToRemove = Math.max(0, currentCount - maxAllowed);

            logger.debug("Current count: {}, Maximum allowed: {}, Elements to remove: {}",
                    currentCount, maxAllowed, elementsToRemove);

            // Remove excess elements (from the end to minimize disruption)
            List<Element> elementsToDelete = childElements.subList(
                    childElements.size() - elementsToRemove, childElements.size());

            for (Element element : elementsToDelete) {
                if (domManipulator.removeElement(element)) {
                    logger.debug("Removed excess element: {}", elementName);
                } else {
                    logger.warn("Failed to remove excess element: {}", elementName);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("Error handling too many occurrences for element: {}", elementName, e);
            return false;
        }
    }

    private Element createDefaultElement(Document document, SchemaElement schemaElement) {
        String elementName = schemaElement.getName();
        String defaultValue = schemaElement.getDefaultValue();

        // Create element with default content
        Element element = domManipulator.createElement(document, elementName, defaultValue);

        // Add required child elements if this is a complex type
        if (schemaElement.hasChildren()) {
            for (SchemaElement child : schemaElement.getChildren()) {
                if (child.isRequired()) {
                    Element childElement = createDefaultElement(document, child);
                    if (childElement != null) {
                        element.appendChild(childElement);
                    }
                }
            }
        }

        return element;
    }

    private Element findBestInsertionPoint(Element parentElement, String elementName, SchemaElement schemaElement) {
        // Find the last occurrence of this element type
        List<Element> existingElements = domManipulator.getChildElements(parentElement, elementName);
        if (!existingElements.isEmpty()) {
            return existingElements.get(existingElements.size() - 1);
        }

        // If no existing elements, find the best position based on schema order
        return findInsertionPointBySchemaOrder(parentElement, elementName, schemaElement);
    }

    private Element findInsertionPointBySchemaOrder(Element parentElement, String elementName, SchemaElement schemaElement) {
        // This would require more complex schema analysis to determine proper ordering
        // For now, return null to append at the end
        return null;
    }

    @Override
    public String getStrategyName() {
        return "CardinalityStrategy";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public List<ErrorType> getSupportedErrorTypes() {
        return List.of();
    }

    @Override
    public List<CorrectionAction> generateCorrections(Map<ErrorType, List<ValidationError>> errorsByType, SchemaElement schema) {
        return List.of();
    }
}
