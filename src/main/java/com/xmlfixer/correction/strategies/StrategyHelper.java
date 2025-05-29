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
 * Helper methods shared across strategies
 */
public class StrategyHelper {
    private static final Logger logger = LoggerFactory.getLogger(StrategyHelper.class);

    /**
     * Finds parent element for a given XPath and element name
     */
    public static Element findParentElement(Document document, String xPath, String elementName) {
        if (xPath == null || xPath.isEmpty()) {
            return document.getDocumentElement();
        }

        // Remove the last path segment to get parent path
        int lastSlash = xPath.lastIndexOf('/');
        if (lastSlash > 0) {
            String parentPath = xPath.substring(0, lastSlash);
            return findElementByPath(document, parentPath);
        }

        return document.getDocumentElement();
    }

    /**
     * Finds element by XPath
     */
    public static Element findElementByPath(Document document, String xPath) {
        try {
            javax.xml.xpath.XPathFactory xPathFactory = javax.xml.xpath.XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xPathFactory.newXPath();
            javax.xml.xpath.XPathExpression expression = xpath.compile(xPath);

            Object result = expression.evaluate(document, javax.xml.xpath.XPathConstants.NODE);
            if (result instanceof Element) {
                return (Element) result;
            }
        } catch (Exception e) {
            logger.warn("Failed to find element by XPath: {}", xPath, e);
        }

        return null;
    }

    /**
     * Finds schema element by name recursively
     */
    public static SchemaElement findSchemaElement(SchemaElement rootSchema, String elementName) {
        if (rootSchema == null || elementName == null) {
            return null;
        }

        if (elementName.equals(rootSchema.getName())) {
            return rootSchema;
        }

        if (rootSchema.hasChildren()) {
            for (SchemaElement child : rootSchema.getChildren()) {
                SchemaElement found = findSchemaElement(child, elementName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}
