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
 * Strategy for correcting content issues
 */
public class ContentStrategy implements CorrectionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ContentStrategy.class);
    private final DomManipulator domManipulator;

    public ContentStrategy(DomManipulator domManipulator) {
        this.domManipulator = domManipulator;
    }

    @Override
    public boolean canCorrect(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();
        return errorType == ErrorType.EMPTY_REQUIRED_CONTENT ||
                errorType == ErrorType.INVALID_CONTENT_MODEL;
    }

    @Override
    public boolean applyCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();

        if (errorType == ErrorType.EMPTY_REQUIRED_CONTENT) {
            return handleEmptyRequiredContent(action, document, rootSchema);
        } else if (errorType == ErrorType.INVALID_CONTENT_MODEL) {
            return handleInvalidContentModel(action, document, rootSchema);
        }

        return false;
    }

    private boolean handleEmptyRequiredContent(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();

        logger.debug("Filling empty required content at path: {}", xPath);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null) {
                return false;
            }

            SchemaElement schemaElement = StrategyHelper.findSchemaElement(rootSchema, element.getNodeName());
            if (schemaElement != null) {
                String defaultContent = schemaElement.getDefaultValue();
                if (defaultContent == null || defaultContent.isEmpty()) {
                    defaultContent = "default_value";
                }

                return domManipulator.modifyElementContent(element, defaultContent);
            }

            return false;

        } catch (Exception e) {
            logger.error("Error filling empty required content: {}", xPath, e);
            return false;
        }
    }

    private boolean handleInvalidContentModel(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();

        logger.debug("Correcting invalid content model at path: {}", xPath);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null) {
                return false;
            }

            // This would involve more complex content model validation and correction
            // For now, we'll implement basic cleanup
            return cleanupInvalidContent(element);

        } catch (Exception e) {
            logger.error("Error correcting invalid content model: {}", xPath, e);
            return false;
        }
    }

    private boolean cleanupInvalidContent(Element element) {
        // Remove any text content from elements that should only have child elements
        NodeList children = element.getChildNodes();
        boolean hasElementChildren = false;

        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                hasElementChildren = true;
                break;
            }
        }

        if (hasElementChildren) {
            // Remove text nodes from mixed content
            List<org.w3c.dom.Node> textNodes = new ArrayList<>();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE &&
                        child.getNodeValue().trim().isEmpty()) {
                    textNodes.add(child);
                }
            }

            for (org.w3c.dom.Node textNode : textNodes) {
                element.removeChild(textNode);
            }
        }

        return true;
    }

    @Override
    public String getStrategyName() {
        return "ContentStrategy";
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
