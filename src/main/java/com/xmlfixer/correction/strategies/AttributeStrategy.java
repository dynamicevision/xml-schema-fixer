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

import javax.inject.Inject;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
/**
 * Strategy for correcting attribute issues
 */
public class AttributeStrategy implements CorrectionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AttributeStrategy.class);
    private final DomManipulator domManipulator;

    @Inject
    public AttributeStrategy(DomManipulator domManipulator) {
        this.domManipulator = domManipulator;
    }

    @Override
    public boolean canCorrect(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();
        return errorType == ErrorType.MISSING_REQUIRED_ATTRIBUTE ||
                errorType == ErrorType.INVALID_ATTRIBUTE_VALUE;
    }

    @Override
    public boolean applyCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();

        if (errorType == ErrorType.MISSING_REQUIRED_ATTRIBUTE) {
            return handleMissingAttribute(action, document, rootSchema);
        } else if (errorType == ErrorType.INVALID_ATTRIBUTE_VALUE) {
            return handleInvalidAttributeValue(action, document, rootSchema);
        }

        return false;
    }

    private boolean handleMissingAttribute(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();
        String attributeName = extractAttributeName(action.getDescription());

        logger.debug("Adding missing required attribute: {} at path: {}", attributeName, xPath);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null || attributeName == null) {
                return false;
            }

            String defaultValue = getDefaultAttributeValue(attributeName);
            return domManipulator.setAttribute(element, attributeName, defaultValue);

        } catch (Exception e) {
            logger.error("Error adding missing attribute: {}", attributeName, e);
            return false;
        }
    }

    private boolean handleInvalidAttributeValue(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();
        String attributeName = extractAttributeName(action.getDescription());
        String currentValue = action.getOldValue();

        logger.debug("Correcting invalid attribute value: {} = {} at path: {}",
                attributeName, currentValue, xPath);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null || attributeName == null) {
                return false;
            }

            String correctedValue = correctAttributeValue(attributeName, currentValue);
            if (correctedValue != null && !correctedValue.equals(currentValue)) {
                action.setNewValue(correctedValue);
                return domManipulator.setAttribute(element, attributeName, correctedValue);
            }

            return false;

        } catch (Exception e) {
            logger.error("Error correcting attribute value: {}", attributeName, e);
            return false;
        }
    }

    private String extractAttributeName(String description) {
        // Simple extraction - in practice would need more sophisticated parsing
        if (description != null && description.contains("attribute")) {
            String[] parts = description.split("'");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return null;
    }

    private String getDefaultAttributeValue(String attributeName) {
        // Provide sensible defaults for common attributes
        switch (attributeName.toLowerCase()) {
            case "id":
                return "1";
            case "version":
                return "1.0";
            case "type":
                return "default";
            default:
                return "";
        }
    }

    private String correctAttributeValue(String attributeName, String currentValue) {
        if (currentValue == null || currentValue.trim().isEmpty()) {
            return getDefaultAttributeValue(attributeName);
        }

        // Apply basic corrections
        return currentValue.trim();
    }

    @Override
    public String getStrategyName() {
        return "AttributeStrategy";
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
