package com.xmlfixer.correction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Enhanced service for manipulating XML DOM structures with precision and efficiency
 * Provides high-level operations for XML correction while preserving document structure
 */
@Singleton
public class DomManipulator {

    private static final Logger logger = LoggerFactory.getLogger(DomManipulator.class);

    private final DocumentBuilderFactory documentBuilderFactory;
    private final XPathFactory xPathFactory;
    private final TransformerFactory transformerFactory;

    @Inject
    public DomManipulator() {
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setNamespaceAware(true);
        this.documentBuilderFactory.setIgnoringComments(false);
        this.documentBuilderFactory.setIgnoringElementContentWhitespace(false);

        this.xPathFactory = XPathFactory.newInstance();
        this.transformerFactory = TransformerFactory.newInstance();

        logger.info("DomManipulator initialized");
    }

    /**
     * Loads an XML document from file
     */
    public Document loadDocument(File xmlFile) {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            document.normalize();

            logger.debug("Successfully loaded XML document: {}", xmlFile.getName());
            return document;

        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Failed to load XML document: {}", xmlFile.getName(), e);
            return null;
        }
    }

    /**
     * Saves a DOM document to file with proper formatting
     */
    public boolean saveDocument(Document document, File outputFile) {
        try {
            Transformer transformer = transformerFactory.newTransformer();

            // Configure output properties for readable XML
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            // Transform and save
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(outputFile);
            transformer.transform(source, result);

            logger.debug("Successfully saved XML document: {}", outputFile.getName());
            return true;

        } catch (TransformerException e) {
            logger.error("Failed to save XML document: {}", outputFile.getName(), e);
            return false;
        }
    }

    /**
     * Finds elements using XPath expression
     */
    public NodeList findElements(Document document, String xpathExpression) {
        try {
            XPath xpath = xPathFactory.newXPath();
            XPathExpression expression = xpath.compile(xpathExpression);

            Object result = expression.evaluate(document, XPathConstants.NODESET);
            return (NodeList) result;

        } catch (XPathExpressionException e) {
            logger.error("Failed to evaluate XPath expression: {}", xpathExpression, e);
            return null;
        }
    }

    /**
     * Finds a single element using XPath expression
     */
    public Element findElement(Document document, String xpathExpression) {
        NodeList nodes = findElements(document, xpathExpression);
        if (nodes != null && nodes.getLength() > 0) {
            Node node = nodes.item(0);
            if (node instanceof Element) {
                return (Element) node;
            }
        }
        return null;
    }

    /**
     * Creates a new element with specified name and content
     */
    public Element createElement(Document document, String elementName, String textContent) {
        Element element = document.createElement(elementName);
        if (textContent != null && !textContent.isEmpty()) {
            element.setTextContent(textContent);
        }
        return element;
    }

    /**
     * Creates a new element with attributes
     */
    public Element createElement(Document document, String elementName,
                                 Map<String, String> attributes, String textContent) {
        Element element = createElement(document, elementName, textContent);

        if (attributes != null) {
            for (Map.Entry<String, String> attr : attributes.entrySet()) {
                element.setAttribute(attr.getKey(), attr.getValue());
            }
        }

        return element;
    }

    /**
     * Inserts an element at the specified position relative to a reference element
     */
    public boolean insertElement(Element newElement, Element referenceElement, InsertPosition position) {
        if (newElement == null || referenceElement == null) {
            logger.warn("Cannot insert element: null element provided");
            return false;
        }

        Node parentNode = referenceElement.getParentNode();
        if (parentNode == null) {
            logger.warn("Cannot insert element: reference element has no parent");
            return false;
        }

        try {
            switch (position) {
                case BEFORE:
                    parentNode.insertBefore(newElement, referenceElement);
                    break;
                case AFTER:
                    Node nextSibling = referenceElement.getNextSibling();
                    if (nextSibling != null) {
                        parentNode.insertBefore(newElement, nextSibling);
                    } else {
                        parentNode.appendChild(newElement);
                    }
                    break;
                case FIRST_CHILD:
                    Node firstChild = referenceElement.getFirstChild();
                    if (firstChild != null) {
                        referenceElement.insertBefore(newElement, firstChild);
                    } else {
                        referenceElement.appendChild(newElement);
                    }
                    break;
                case LAST_CHILD:
                    referenceElement.appendChild(newElement);
                    break;
            }

            logger.debug("Successfully inserted element '{}' {} '{}'",
                    newElement.getNodeName(), position.toString().toLowerCase(),
                    referenceElement.getNodeName());
            return true;

        } catch (DOMException e) {
            logger.error("Failed to insert element", e);
            return false;
        }
    }

    /**
     * Removes an element from the document
     */
    public boolean removeElement(Element element) {
        if (element == null) {
            logger.warn("Cannot remove null element");
            return false;
        }

        Node parentNode = element.getParentNode();
        if (parentNode == null) {
            logger.warn("Cannot remove element: no parent node");
            return false;
        }

        try {
            parentNode.removeChild(element);
            logger.debug("Successfully removed element: {}", element.getNodeName());
            return true;
        } catch (DOMException e) {
            logger.error("Failed to remove element: {}", element.getNodeName(), e);
            return false;
        }
    }

    /**
     * Moves an element to a new position
     */
    public boolean moveElement(Element elementToMove, Element referenceElement, InsertPosition position) {
        if (elementToMove == null || referenceElement == null) {
            logger.warn("Cannot move element: null element provided");
            return false;
        }

        try {
            // Remove from current position
            Node parentNode = elementToMove.getParentNode();
            if (parentNode != null) {
                parentNode.removeChild(elementToMove);
            }

            // Insert at new position
            return insertElement(elementToMove, referenceElement, position);

        } catch (DOMException e) {
            logger.error("Failed to move element", e);
            return false;
        }
    }

    /**
     * Modifies element content while preserving attributes and children
     */
    public boolean modifyElementContent(Element element, String newContent) {
        if (element == null) {
            logger.warn("Cannot modify null element");
            return false;
        }

        try {
            // Only modify text content, preserve child elements
            NodeList children = element.getChildNodes();
            List<Node> textNodes = new ArrayList<>();

            // Find and remove existing text nodes
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    textNodes.add(child);
                }
            }

            for (Node textNode : textNodes) {
                element.removeChild(textNode);
            }

            // Add new text content if provided
            if (newContent != null && !newContent.isEmpty()) {
                Text textNode = element.getOwnerDocument().createTextNode(newContent);
                element.appendChild(textNode);
            }

            logger.debug("Successfully modified content of element: {}", element.getNodeName());
            return true;

        } catch (DOMException e) {
            logger.error("Failed to modify element content", e);
            return false;
        }
    }

    /**
     * Adds or updates an attribute on an element
     */
    public boolean setAttribute(Element element, String attributeName, String attributeValue) {
        if (element == null || attributeName == null) {
            logger.warn("Cannot set attribute: null element or attribute name");
            return false;
        }

        try {
            element.setAttribute(attributeName, attributeValue != null ? attributeValue : "");
            logger.debug("Successfully set attribute '{}' = '{}' on element '{}'",
                    attributeName, attributeValue, element.getNodeName());
            return true;
        } catch (DOMException e) {
            logger.error("Failed to set attribute", e);
            return false;
        }
    }

    /**
     * Removes an attribute from an element
     */
    public boolean removeAttribute(Element element, String attributeName) {
        if (element == null || attributeName == null) {
            logger.warn("Cannot remove attribute: null element or attribute name");
            return false;
        }

        try {
            element.removeAttribute(attributeName);
            logger.debug("Successfully removed attribute '{}' from element '{}'",
                    attributeName, element.getNodeName());
            return true;
        } catch (DOMException e) {
            logger.error("Failed to remove attribute", e);
            return false;
        }
    }

    /**
     * Reorders child elements according to the specified order
     */
    public boolean reorderElements(Element parentElement, List<String> desiredOrder) {
        if (parentElement == null || desiredOrder == null || desiredOrder.isEmpty()) {
            logger.warn("Cannot reorder elements: invalid parameters");
            return false;
        }

        try {
            // Get current child elements
            Map<String, List<Element>> elementsByName = new HashMap<>();
            List<Element> childElements = new ArrayList<>();

            NodeList children = parentElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    Element element = (Element) child;
                    childElements.add(element);
                    elementsByName.computeIfAbsent(element.getNodeName(), k -> new ArrayList<>())
                            .add(element);
                }
            }

            // Remove all child elements
            for (Element element : childElements) {
                parentElement.removeChild(element);
            }

            // Re-add elements in desired order
            for (String elementName : desiredOrder) {
                List<Element> elements = elementsByName.get(elementName);
                if (elements != null) {
                    for (Element element : elements) {
                        parentElement.appendChild(element);
                    }
                }
            }

            logger.debug("Successfully reordered {} child elements in parent '{}'",
                    childElements.size(), parentElement.getNodeName());
            return true;

        } catch (DOMException e) {
            logger.error("Failed to reorder elements", e);
            return false;
        }
    }

    /**
     * Gets the XPath expression for an element
     */
    public String getElementPath(Element element) {
        if (element == null) {
            return "";
        }

        StringBuilder path = new StringBuilder();
        Node current = element;

        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            Element currentElement = (Element) current;
            String nodeName = currentElement.getNodeName();

            // Calculate position among siblings with same name
            int position = 1;
            Node sibling = currentElement.getPreviousSibling();
            while (sibling != null) {
                if (sibling.getNodeType() == Node.ELEMENT_NODE &&
                        sibling.getNodeName().equals(nodeName)) {
                    position++;
                }
                sibling = sibling.getPreviousSibling();
            }

            // Build path segment
            String segment = nodeName;
            if (position > 1) {
                segment += "[" + position + "]";
            }

            if (path.length() > 0) {
                path.insert(0, "/" + segment);
            } else {
                path.insert(0, segment);
            }

            current = current.getParentNode();
        }

        return "/" + path.toString();
    }

    /**
     * Creates a deep copy of an element
     */
    public Element cloneElement(Element element, boolean deep) {
        if (element == null) {
            return null;
        }

        try {
            return (Element) element.cloneNode(deep);
        } catch (DOMException e) {
            logger.error("Failed to clone element", e);
            return null;
        }
    }

    /**
     * Validates if an element has the expected structure
     */
    public boolean validateElementStructure(Element element, String expectedName,
                                            Set<String> requiredAttributes) {
        if (element == null) {
            return false;
        }

        // Check element name
        if (expectedName != null && !expectedName.equals(element.getNodeName())) {
            return false;
        }

        // Check required attributes
        if (requiredAttributes != null) {
            NamedNodeMap attributes = element.getAttributes();
            for (String requiredAttr : requiredAttributes) {
                if (attributes.getNamedItem(requiredAttr) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets all child elements with specified name
     */
    public List<Element> getChildElements(Element parentElement, String elementName) {
        List<Element> childElements = new ArrayList<>();

        if (parentElement == null) {
            return childElements;
        }

        NodeList children = parentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element element = (Element) child;
                if (elementName == null || elementName.equals(element.getNodeName())) {
                    childElements.add(element);
                }
            }
        }

        return childElements;
    }

    /**
     * Counts occurrences of child elements with specified name
     */
    public int countChildElements(Element parentElement, String elementName) {
        return getChildElements(parentElement, elementName).size();
    }

    /**
     * Enumeration for element insertion positions
     */
    public enum InsertPosition {
        BEFORE,
        AFTER,
        FIRST_CHILD,
        LAST_CHILD
    }
}
