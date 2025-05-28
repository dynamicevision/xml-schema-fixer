package com.xmlfixer.schema;

import com.xmlfixer.common.exceptions.XmlFixerException;
import com.xmlfixer.schema.model.ElementConstraint;
import com.xmlfixer.schema.model.OrderingRule;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.schema.model.ValidationRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for parsing XSD schema files and extracting structural information
 */
@Singleton
public class SchemaParser {

    private static final Logger logger = LoggerFactory.getLogger(SchemaParser.class);

    // XSD namespace constants
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String XS_PREFIX = "xs:";
    private static final String XSD_PREFIX = "xsd:";

    private DocumentBuilderFactory documentBuilderFactory;

    @Inject
    public SchemaParser() {
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setNamespaceAware(true);
        this.documentBuilderFactory.setIgnoringComments(true);
        this.documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        logger.info("SchemaParser initialized");
    }

    /**
     * Parses an XSD schema file and returns the parsed schema document
     */
    public Document parseSchemaDocument(File schemaFile) {
        logger.info("Parsing schema document: {}", schemaFile.getName());

        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.parse(schemaFile);
            document.getDocumentElement().normalize();

            logger.debug("Successfully parsed schema document: {}", schemaFile.getName());
            return document;

        } catch (Exception e) {
            logger.error("Failed to parse schema document: {}", schemaFile.getName(), e);
            throw new XmlFixerException("Schema parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts all element definitions from a schema document
     */
    public List<SchemaElement> extractElements(Document schemaDocument, File schemaFile) {
        logger.debug("Extracting elements from schema document");

        List<SchemaElement> elements = new ArrayList<>();
        Element root = schemaDocument.getDocumentElement();

        // Get all element definitions at schema level
        NodeList elementNodes = root.getElementsByTagNameNS(XSD_NAMESPACE, "element");

        for (int i = 0; i < elementNodes.getLength(); i++) {
            Element elementNode = (Element) elementNodes.item(i);
            SchemaElement schemaElement = parseElementDefinition(elementNode, schemaFile);
            if (schemaElement != null) {
                elements.add(schemaElement);
            }
        }

        logger.debug("Extracted {} elements from schema", elements.size());
        return elements;
    }

    /**
     * Parses a single element definition from XSD
     */
    private SchemaElement parseElementDefinition(Element elementNode, File schemaFile) {
        String name = elementNode.getAttribute("name");
        if (name == null || name.trim().isEmpty()) {
            return null; // Skip elements without names (references)
        }

        SchemaElement schemaElement = new SchemaElement(name);
        schemaElement.setSchemaFile(schemaFile);

        // Parse basic attributes
        parseBasicElementAttributes(elementNode, schemaElement);

        // Parse occurrence constraints
        parseOccurrenceConstraints(elementNode, schemaElement);

        // Parse type information
        parseTypeInformation(elementNode, schemaElement);

        // Parse nested elements and constraints
        parseNestedContent(elementNode, schemaElement, schemaFile);

        // Parse documentation
        parseDocumentation(elementNode, schemaElement);

        logger.debug("Parsed element: {}", schemaElement.getName());
        return schemaElement;
    }

    /**
     * Parses basic element attributes
     */
    private void parseBasicElementAttributes(Element elementNode, SchemaElement schemaElement) {
        // Type attribute
        String type = elementNode.getAttribute("type");
        if (type != null && !type.isEmpty()) {
            schemaElement.setType(type);
        }

        // Default value
        String defaultValue = elementNode.getAttribute("default");
        if (defaultValue != null && !defaultValue.isEmpty()) {
            schemaElement.setDefaultValue(defaultValue);
        }

        // Fixed value
        String fixed = elementNode.getAttribute("fixed");
        if (fixed != null && !fixed.isEmpty()) {
            schemaElement.setDefaultValue(fixed);
            // Add constraint for fixed value
            ElementConstraint fixedConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.PATTERN, fixed, "Fixed value constraint");
            fixedConstraint.setRequired(true);
            schemaElement.addConstraint(fixedConstraint);
        }

        // Namespace information
        String targetNamespace = elementNode.getOwnerDocument().getDocumentElement()
                .getAttribute("targetNamespace");
        if (targetNamespace != null && !targetNamespace.isEmpty()) {
            schemaElement.setNamespace(targetNamespace);
        }
    }

    /**
     * Parses occurrence constraints (minOccurs, maxOccurs)
     */
    private void parseOccurrenceConstraints(Element elementNode, SchemaElement schemaElement) {
        // Parse minOccurs
        String minOccurs = elementNode.getAttribute("minOccurs");
        if (minOccurs != null && !minOccurs.isEmpty()) {
            try {
                int min = Integer.parseInt(minOccurs);
                schemaElement.setMinOccurs(min);
            } catch (NumberFormatException e) {
                logger.warn("Invalid minOccurs value: {} for element: {}", minOccurs, schemaElement.getName());
            }
        }

        // Parse maxOccurs
        String maxOccurs = elementNode.getAttribute("maxOccurs");
        if (maxOccurs != null && !maxOccurs.isEmpty()) {
            try {
                if ("unbounded".equals(maxOccurs)) {
                    schemaElement.setMaxOccurs(Integer.MAX_VALUE);
                } else {
                    int max = Integer.parseInt(maxOccurs);
                    schemaElement.setMaxOccurs(max);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid maxOccurs value: {} for element: {}", maxOccurs, schemaElement.getName());
            }
        }
    }

    /**
     * Parses type information including simple and complex types
     */
    private void parseTypeInformation(Element elementNode, SchemaElement schemaElement) {
        // Check for inline type definition
        NodeList complexTypes = elementNode.getElementsByTagNameNS(XSD_NAMESPACE, "complexType");
        NodeList simpleTypes = elementNode.getElementsByTagNameNS(XSD_NAMESPACE, "simpleType");

        if (complexTypes.getLength() > 0) {
            parseComplexType((Element) complexTypes.item(0), schemaElement);
        } else if (simpleTypes.getLength() > 0) {
            parseSimpleType((Element) simpleTypes.item(0), schemaElement);
        }
    }

    /**
     * Parses complex type definitions
     */
    private void parseComplexType(Element complexTypeNode, SchemaElement schemaElement) {
        schemaElement.setType("complexType");

        // Parse sequence, choice, or all groups
        NodeList sequences = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "sequence");
        NodeList choices = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "choice");
        NodeList alls = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "all");

        if (sequences.getLength() > 0) {
            parseSequenceGroup((Element) sequences.item(0), schemaElement);
        } else if (choices.getLength() > 0) {
            parseChoiceGroup((Element) choices.item(0), schemaElement);
        } else if (alls.getLength() > 0) {
            parseAllGroup((Element) alls.item(0), schemaElement);
        }

        // Parse attributes
        parseAttributes(complexTypeNode, schemaElement);
    }

    /**
     * Parses simple type definitions and constraints
     */
    private void parseSimpleType(Element simpleTypeNode, SchemaElement schemaElement) {
        schemaElement.setType("simpleType");

        // Parse restrictions
        NodeList restrictions = simpleTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "restriction");
        if (restrictions.getLength() > 0) {
            parseRestrictions((Element) restrictions.item(0), schemaElement);
        }
    }

    /**
     * Parses restriction facets (patterns, enumerations, etc.)
     */
    private void parseRestrictions(Element restrictionNode, SchemaElement schemaElement) {
        String base = restrictionNode.getAttribute("base");
        if (base != null && !base.isEmpty()) {
            schemaElement.setType(base);
        }

        NodeList children = restrictionNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element facetElement = (Element) child;
                parseFacet(facetElement, schemaElement);
            }
        }
    }

    /**
     * Parses individual restriction facets
     */
    private void parseFacet(Element facetElement, SchemaElement schemaElement) {
        String facetName = facetElement.getLocalName();
        String value = facetElement.getAttribute("value");

        if (value == null || value.isEmpty()) {
            return;
        }

        ElementConstraint constraint = null;

        switch (facetName) {
            case "pattern":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.PATTERN, value);
                break;
            case "enumeration":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.ENUMERATION, value);
                break;
            case "minLength":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.MIN_LENGTH, value);
                break;
            case "maxLength":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.MAX_LENGTH, value);
                break;
            case "minInclusive":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.MIN_INCLUSIVE, value);
                break;
            case "maxInclusive":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.MAX_INCLUSIVE, value);
                break;
            case "minExclusive":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.MIN_EXCLUSIVE, value);
                break;
            case "maxExclusive":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.MAX_EXCLUSIVE, value);
                break;
            case "totalDigits":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.TOTAL_DIGITS, value);
                break;
            case "fractionDigits":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.FRACTION_DIGITS, value);
                break;
            case "whiteSpace":
                constraint = new ElementConstraint(ElementConstraint.ConstraintType.WHITE_SPACE, value);
                break;
            default:
                logger.debug("Unknown facet type: {}", facetName);
        }

        if (constraint != null) {
            schemaElement.addConstraint(constraint);
            logger.debug("Added constraint: {} = {} to element: {}",
                    facetName, value, schemaElement.getName());
        }
    }

    /**
     * Parses sequence groups (ordered elements)
     */
    private void parseSequenceGroup(Element sequenceNode, SchemaElement parentElement) {
        NodeList elements = sequenceNode.getElementsByTagNameNS(XSD_NAMESPACE, "element");

        for (int i = 0; i < elements.getLength(); i++) {
            Element childElementNode = (Element) elements.item(i);
            SchemaElement childElement = parseElementDefinition(childElementNode, parentElement.getSchemaFile());
            if (childElement != null) {
                parentElement.addChild(childElement);
            }
        }

        // Add ordering rule for sequence
        OrderingRule orderingRule = new OrderingRule();
        orderingRule.setType(OrderingRule.OrderingType.SEQUENCE);
        orderingRule.setStrict(true);
        // Store ordering rule in parent element (will be implemented in OrderingRule class)
    }

    /**
     * Parses choice groups (alternative elements)
     */
    private void parseChoiceGroup(Element choiceNode, SchemaElement parentElement) {
        NodeList elements = choiceNode.getElementsByTagNameNS(XSD_NAMESPACE, "element");

        for (int i = 0; i < elements.getLength(); i++) {
            Element childElementNode = (Element) elements.item(i);
            SchemaElement childElement = parseElementDefinition(childElementNode, parentElement.getSchemaFile());
            if (childElement != null) {
                parentElement.addChild(childElement);
            }
        }

        // Add ordering rule for choice
        OrderingRule orderingRule = new OrderingRule();
        orderingRule.setType(OrderingRule.OrderingType.CHOICE);
        orderingRule.setStrict(false);
    }

    /**
     * Parses all groups (unordered elements)
     */
    private void parseAllGroup(Element allNode, SchemaElement parentElement) {
        NodeList elements = allNode.getElementsByTagNameNS(XSD_NAMESPACE, "element");

        for (int i = 0; i < elements.getLength(); i++) {
            Element childElementNode = (Element) elements.item(i);
            SchemaElement childElement = parseElementDefinition(childElementNode, parentElement.getSchemaFile());
            if (childElement != null) {
                parentElement.addChild(childElement);
            }
        }

        // Add ordering rule for all
        OrderingRule orderingRule = new OrderingRule();
        orderingRule.setType(OrderingRule.OrderingType.ALL);
        orderingRule.setStrict(false);
    }

    /**
     * Parses attributes of complex types
     */
    private void parseAttributes(Element complexTypeNode, SchemaElement schemaElement) {
        NodeList attributes = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "attribute");

        for (int i = 0; i < attributes.getLength(); i++) {
            Element attrNode = (Element) attributes.item(i);
            String name = attrNode.getAttribute("name");
            String use = attrNode.getAttribute("use");
            String type = attrNode.getAttribute("type");

            if (name != null && !name.isEmpty()) {
                // Create a validation rule for the attribute
                ValidationRule attrRule = new ValidationRule();
                attrRule.setRuleType(ValidationRule.RuleType.ATTRIBUTE_REQUIRED);
                attrRule.setElementName(schemaElement.getName());
                attrRule.setAttributeName(name);
                attrRule.setRequired("required".equals(use));

                logger.debug("Added attribute rule: {} for element: {}", name, schemaElement.getName());
            }
        }
    }

    /**
     * Parses nested content (child elements)
     */
    private void parseNestedContent(Element elementNode, SchemaElement schemaElement, File schemaFile) {
        // This method handles complex nested structures
        // Implementation depends on the specific XSD structure
        logger.debug("Parsing nested content for element: {}", schemaElement.getName());
    }

    /**
     * Parses documentation annotations
     */
    private void parseDocumentation(Element elementNode, SchemaElement schemaElement) {
        NodeList annotations = elementNode.getElementsByTagNameNS(XSD_NAMESPACE, "annotation");
        if (annotations.getLength() > 0) {
            Element annotation = (Element) annotations.item(0);
            NodeList docs = annotation.getElementsByTagNameNS(XSD_NAMESPACE, "documentation");
            if (docs.getLength() > 0) {
                String documentation = docs.item(0).getTextContent();
                if (documentation != null) {
                    schemaElement.setDocumentation(documentation.trim());
                }
            }
        }
    }

    /**
     * Validates that a schema file is well-formed XSD
     */
    public boolean isValidSchemaFile(File schemaFile) {
        try {
            Document doc = parseSchemaDocument(schemaFile);
            Element root = doc.getDocumentElement();

            // Check if root element is schema
            return "schema".equals(root.getLocalName()) &&
                    XSD_NAMESPACE.equals(root.getNamespaceURI());

        } catch (Exception e) {
            logger.debug("Schema validation failed for: {}", schemaFile.getName(), e);
            return false;
        }
    }
}
