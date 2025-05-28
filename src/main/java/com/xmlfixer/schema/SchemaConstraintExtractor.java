package com.xmlfixer.schema;

import com.xmlfixer.schema.model.ElementConstraint;
import com.xmlfixer.schema.model.OrderingRule;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.schema.model.ValidationRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Advanced service for extracting complex constraints and rules from XSD schemas
 */
@Singleton
public class SchemaConstraintExtractor {

    private static final Logger logger = LoggerFactory.getLogger(SchemaConstraintExtractor.class);

    // XSD namespace and common prefixes
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    // Built-in XSD data types and their validation patterns
    private static final Map<String, String> XSD_TYPE_PATTERNS = new HashMap<>();

    static {
        XSD_TYPE_PATTERNS.put("int", "^-?\\d+$");
        XSD_TYPE_PATTERNS.put("integer", "^-?\\d+$");
        XSD_TYPE_PATTERNS.put("positiveInteger", "^\\d*[1-9]\\d*$");
        XSD_TYPE_PATTERNS.put("negativeInteger", "^-\\d*[1-9]\\d*$");
        XSD_TYPE_PATTERNS.put("nonNegativeInteger", "^\\d+$");
        XSD_TYPE_PATTERNS.put("nonPositiveInteger", "^-?\\d+$");
        XSD_TYPE_PATTERNS.put("decimal", "^-?\\d+(\\.\\d+)?$");
        XSD_TYPE_PATTERNS.put("double", "^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");
        XSD_TYPE_PATTERNS.put("float", "^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");
        XSD_TYPE_PATTERNS.put("boolean", "^(true|false|1|0)$");
        XSD_TYPE_PATTERNS.put("date", "^\\d{4}-\\d{2}-\\d{2}$");
        XSD_TYPE_PATTERNS.put("time", "^\\d{2}:\\d{2}:\\d{2}$");
        XSD_TYPE_PATTERNS.put("dateTime", "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$");
    }

    @Inject
    public SchemaConstraintExtractor() {
        logger.info("SchemaConstraintExtractor initialized");
    }

    /**
     * Extracts comprehensive constraints from a schema document
     */
    public Map<String, List<ElementConstraint>> extractAllConstraints(Document schemaDocument) {
        logger.debug("Extracting all constraints from schema document");

        Map<String, List<ElementConstraint>> constraintMap = new HashMap<>();

        // Extract constraints from global elements
        NodeList elements = schemaDocument.getElementsByTagNameNS(XSD_NAMESPACE, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String elementName = element.getAttribute("name");

            if (elementName != null && !elementName.isEmpty()) {
                List<ElementConstraint> constraints = extractElementConstraints(element);
                if (!constraints.isEmpty()) {
                    constraintMap.put(elementName, constraints);
                }
            }
        }

        // Extract constraints from named complex types
        NodeList complexTypes = schemaDocument.getElementsByTagNameNS(XSD_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element complexType = (Element) complexTypes.item(i);
            String typeName = complexType.getAttribute("name");

            if (typeName != null && !typeName.isEmpty()) {
                List<ElementConstraint> constraints = extractComplexTypeConstraints(complexType);
                if (!constraints.isEmpty()) {
                    constraintMap.put(typeName, constraints);
                }
            }
        }

        // Extract constraints from named simple types
        NodeList simpleTypes = schemaDocument.getElementsByTagNameNS(XSD_NAMESPACE, "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            Element simpleType = (Element) simpleTypes.item(i);
            String typeName = simpleType.getAttribute("name");

            if (typeName != null && !typeName.isEmpty()) {
                List<ElementConstraint> constraints = extractSimpleTypeConstraints(simpleType);
                if (!constraints.isEmpty()) {
                    constraintMap.put(typeName, constraints);
                }
            }
        }

        logger.debug("Extracted constraints for {} elements/types", constraintMap.size());
        return constraintMap;
    }

    /**
     * Extracts constraints specific to an element
     */
    public List<ElementConstraint> extractElementConstraints(Element elementNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        // Extract occurrence constraints
        constraints.addAll(extractOccurrenceConstraints(elementNode));

        // Extract type-based constraints
        constraints.addAll(extractTypeConstraints(elementNode));

        // Extract inline constraints from nested types
        NodeList simpleTypes = elementNode.getElementsByTagNameNS(XSD_NAMESPACE, "simpleType");
        for (int i = 0; i < simpleTypes.getLength(); i++) {
            constraints.addAll(extractSimpleTypeConstraints((Element) simpleTypes.item(i)));
        }

        NodeList complexTypes = elementNode.getElementsByTagNameNS(XSD_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            constraints.addAll(extractComplexTypeConstraints((Element) complexTypes.item(i)));
        }

        return constraints;
    }

    /**
     * Extracts occurrence-based constraints (minOccurs, maxOccurs)
     */
    private List<ElementConstraint> extractOccurrenceConstraints(Element element) {
        List<ElementConstraint> constraints = new ArrayList<>();

        String minOccurs = element.getAttribute("minOccurs");
        String maxOccurs = element.getAttribute("maxOccurs");

        if (minOccurs != null && !minOccurs.isEmpty() && !minOccurs.equals("1")) {
            ElementConstraint minConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.MIN_INCLUSIVE, minOccurs);
            minConstraint.setDescription("Minimum occurrences: " + minOccurs);
            minConstraint.setRequired(Integer.parseInt(minOccurs) > 0);
            constraints.add(minConstraint);
        }

        if (maxOccurs != null && !maxOccurs.isEmpty() && !maxOccurs.equals("1")) {
            ElementConstraint maxConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.MAX_INCLUSIVE,
                    "unbounded".equals(maxOccurs) ? "∞" : maxOccurs);
            maxConstraint.setDescription("Maximum occurrences: " + maxOccurs);
            constraints.add(maxConstraint);
        }

        return constraints;
    }

    /**
     * Extracts type-based constraints
     */
    private List<ElementConstraint> extractTypeConstraints(Element element) {
        List<ElementConstraint> constraints = new ArrayList<>();

        String type = element.getAttribute("type");
        if (type != null && !type.isEmpty()) {
            // Remove namespace prefix if present
            String baseType = type.contains(":") ? type.split(":")[1] : type;

            // Add built-in type pattern constraints
            if (XSD_TYPE_PATTERNS.containsKey(baseType)) {
                ElementConstraint patternConstraint = new ElementConstraint(
                        ElementConstraint.ConstraintType.PATTERN, XSD_TYPE_PATTERNS.get(baseType));
                patternConstraint.setDescription("Built-in type pattern for: " + baseType);
                constraints.add(patternConstraint);
            }
        }

        // Extract fixed and default value constraints
        String fixed = element.getAttribute("fixed");
        if (fixed != null && !fixed.isEmpty()) {
            ElementConstraint fixedConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.ENUMERATION, fixed);
            fixedConstraint.setDescription("Fixed value: " + fixed);
            fixedConstraint.setRequired(true);
            constraints.add(fixedConstraint);
        }

        String defaultValue = element.getAttribute("default");
        if (defaultValue != null && !defaultValue.isEmpty()) {
            ElementConstraint defaultConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.ENUMERATION, defaultValue);
            defaultConstraint.setDescription("Default value: " + defaultValue);
            constraints.add(defaultConstraint);
        }

        return constraints;
    }

    /**
     * Extracts constraints from simple type definitions
     */
    public List<ElementConstraint> extractSimpleTypeConstraints(Element simpleTypeNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        // Process restrictions
        NodeList restrictions = simpleTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "restriction");
        for (int i = 0; i < restrictions.getLength(); i++) {
            Element restriction = (Element) restrictions.item(i);
            constraints.addAll(extractRestrictionConstraints(restriction));
        }

        // Process unions
        NodeList unions = simpleTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "union");
        for (int i = 0; i < unions.getLength(); i++) {
            Element union = (Element) unions.item(i);
            constraints.addAll(extractUnionConstraints(union));
        }

        // Process lists
        NodeList lists = simpleTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "list");
        for (int i = 0; i < lists.getLength(); i++) {
            Element list = (Element) lists.item(i);
            constraints.addAll(extractListConstraints(list));
        }

        return constraints;
    }

    /**
     * Extracts constraints from complex type definitions
     */
    public List<ElementConstraint> extractComplexTypeConstraints(Element complexTypeNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        // Extract attribute constraints
        NodeList attributes = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            constraints.addAll(extractAttributeConstraints(attribute));
        }

        // Extract content model constraints
        constraints.addAll(extractContentModelConstraints(complexTypeNode));

        return constraints;
    }

    /**
     * Extracts restriction facet constraints
     */
    private List<ElementConstraint> extractRestrictionConstraints(Element restrictionNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        // Map of facet names to constraint types
        Map<String, ElementConstraint.ConstraintType> facetMapping = new HashMap<>();
        facetMapping.put("pattern", ElementConstraint.ConstraintType.PATTERN);
        facetMapping.put("enumeration", ElementConstraint.ConstraintType.ENUMERATION);
        facetMapping.put("minLength", ElementConstraint.ConstraintType.MIN_LENGTH);
        facetMapping.put("maxLength", ElementConstraint.ConstraintType.MAX_LENGTH);
        facetMapping.put("minInclusive", ElementConstraint.ConstraintType.MIN_INCLUSIVE);
        facetMapping.put("maxInclusive", ElementConstraint.ConstraintType.MAX_INCLUSIVE);
        facetMapping.put("minExclusive", ElementConstraint.ConstraintType.MIN_EXCLUSIVE);
        facetMapping.put("maxExclusive", ElementConstraint.ConstraintType.MAX_EXCLUSIVE);
        facetMapping.put("totalDigits", ElementConstraint.ConstraintType.TOTAL_DIGITS);
        facetMapping.put("fractionDigits", ElementConstraint.ConstraintType.FRACTION_DIGITS);
        facetMapping.put("whiteSpace", ElementConstraint.ConstraintType.WHITE_SPACE);

        // Extract each facet type
        for (Map.Entry<String, ElementConstraint.ConstraintType> entry : facetMapping.entrySet()) {
            NodeList facets = restrictionNode.getElementsByTagNameNS(XSD_NAMESPACE, entry.getKey());

            for (int i = 0; i < facets.getLength(); i++) {
                Element facet = (Element) facets.item(i);
                String value = facet.getAttribute("value");

                if (value != null && !value.isEmpty()) {
                    ElementConstraint constraint = new ElementConstraint(entry.getValue(), value);
                    constraint.setDescription(entry.getKey() + ": " + value);

                    // Special handling for enumeration - collect all values
                    if (entry.getValue() == ElementConstraint.ConstraintType.ENUMERATION) {
                        constraint = handleEnumerationConstraint(restrictionNode, constraints);
                        if (constraint != null) {
                            constraints.add(constraint);
                        }
                        break; // Skip individual enumeration processing
                    } else {
                        constraints.add(constraint);
                    }
                }
            }
        }

        return constraints;
    }

    /**
     * Handles enumeration constraints by collecting all enumeration values
     */
    private ElementConstraint handleEnumerationConstraint(Element restrictionNode,
                                                          List<ElementConstraint> existingConstraints) {
        // Check if enumeration constraint already exists
        for (ElementConstraint existing : existingConstraints) {
            if (existing.getConstraintType() == ElementConstraint.ConstraintType.ENUMERATION) {
                return null; // Already processed
            }
        }

        NodeList enumerations = restrictionNode.getElementsByTagNameNS(XSD_NAMESPACE, "enumeration");
        if (enumerations.getLength() == 0) {
            return null;
        }

        List<String> enumValues = new ArrayList<>();
        for (int i = 0; i < enumerations.getLength(); i++) {
            Element enumElement = (Element) enumerations.item(i);
            String value = enumElement.getAttribute("value");
            if (value != null && !value.isEmpty()) {
                enumValues.add(value);
            }
        }

        if (!enumValues.isEmpty()) {
            ElementConstraint enumConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.ENUMERATION, String.join(",", enumValues));
            enumConstraint.setDescription("Allowed values: " + enumValues);
            return enumConstraint;
        }

        return null;
    }

    /**
     * Extracts constraints from union types
     */
    private List<ElementConstraint> extractUnionConstraints(Element unionNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        String memberTypes = unionNode.getAttribute("memberTypes");
        if (memberTypes != null && !memberTypes.isEmpty()) {
            ElementConstraint unionConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.ENUMERATION, memberTypes);
            unionConstraint.setDescription("Union of types: " + memberTypes);
            constraints.add(unionConstraint);
        }

        return constraints;
    }

    /**
     * Extracts constraints from list types
     */
    private List<ElementConstraint> extractListConstraints(Element listNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        String itemType = listNode.getAttribute("itemType");
        if (itemType != null && !itemType.isEmpty()) {
            ElementConstraint listConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.PATTERN, ".*");
            listConstraint.setDescription("List of " + itemType + " values");
            constraints.add(listConstraint);
        }

        return constraints;
    }

    /**
     * Extracts attribute constraints
     */
    private List<ElementConstraint> extractAttributeConstraints(Element attributeNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        String use = attributeNode.getAttribute("use");
        String name = attributeNode.getAttribute("name");

        if ("required".equals(use) && name != null && !name.isEmpty()) {
            ElementConstraint requiredConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.PATTERN, ".*");
            requiredConstraint.setDescription("Required attribute: " + name);
            requiredConstraint.setRequired(true);
            constraints.add(requiredConstraint);
        }

        String fixed = attributeNode.getAttribute("fixed");
        if (fixed != null && !fixed.isEmpty()) {
            ElementConstraint fixedConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.ENUMERATION, fixed);
            fixedConstraint.setDescription("Fixed attribute value: " + fixed);
            fixedConstraint.setRequired(true);
            constraints.add(fixedConstraint);
        }

        return constraints;
    }

    /**
     * Extracts content model constraints (sequence, choice, all)
     */
    private List<ElementConstraint> extractContentModelConstraints(Element complexTypeNode) {
        List<ElementConstraint> constraints = new ArrayList<>();

        // Check for sequence, choice, or all groups
        NodeList sequences = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "sequence");
        NodeList choices = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "choice");
        NodeList alls = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "all");

        if (sequences.getLength() > 0) {
            ElementConstraint sequenceConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.PATTERN, "sequence");
            sequenceConstraint.setDescription("Elements must appear in sequence");
            constraints.add(sequenceConstraint);
        }

        if (choices.getLength() > 0) {
            ElementConstraint choiceConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.ENUMERATION, "choice");
            choiceConstraint.setDescription("Only one element from choice group allowed");
            constraints.add(choiceConstraint);
        }

        if (alls.getLength() > 0) {
            ElementConstraint allConstraint = new ElementConstraint(
                    ElementConstraint.ConstraintType.PATTERN, "all");
            allConstraint.setDescription("All elements must be present (any order)");
            constraints.add(allConstraint);
        }

        return constraints;
    }

    /**
     * Extracts ordering rules from schema elements
     */
    public List<OrderingRule> extractOrderingRules(Document schemaDocument) {
        List<OrderingRule> orderingRules = new ArrayList<>();

        // Extract ordering rules from complex types
        NodeList complexTypes = schemaDocument.getElementsByTagNameNS(XSD_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element complexType = (Element) complexTypes.item(i);
            orderingRules.addAll(extractOrderingRulesFromComplexType(complexType));
        }

        return orderingRules;
    }

    /**
     * Extracts ordering rules from a complex type
     */
    private List<OrderingRule> extractOrderingRulesFromComplexType(Element complexTypeNode) {
        List<OrderingRule> rules = new ArrayList<>();

        // Process sequence groups
        NodeList sequences = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "sequence");
        for (int i = 0; i < sequences.getLength(); i++) {
            Element sequence = (Element) sequences.item(i);
            OrderingRule rule = createOrderingRuleFromGroup(sequence, OrderingRule.OrderingType.SEQUENCE);
            if (rule != null) {
                rules.add(rule);
            }
        }

        // Process choice groups
        NodeList choices = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "choice");
        for (int i = 0; i < choices.getLength(); i++) {
            Element choice = (Element) choices.item(i);
            OrderingRule rule = createOrderingRuleFromGroup(choice, OrderingRule.OrderingType.CHOICE);
            if (rule != null) {
                rules.add(rule);
            }
        }

        // Process all groups
        NodeList alls = complexTypeNode.getElementsByTagNameNS(XSD_NAMESPACE, "all");
        for (int i = 0; i < alls.getLength(); i++) {
            Element all = (Element) alls.item(i);
            OrderingRule rule = createOrderingRuleFromGroup(all, OrderingRule.OrderingType.ALL);
            if (rule != null) {
                rules.add(rule);
            }
        }

        return rules;
    }

    /**
     * Creates an ordering rule from a group element (sequence, choice, all)
     */
    private OrderingRule createOrderingRuleFromGroup(Element groupElement, OrderingRule.OrderingType type) {
        OrderingRule rule = new OrderingRule(type);

        // Extract occurrence constraints
        String minOccurs = groupElement.getAttribute("minOccurs");
        String maxOccurs = groupElement.getAttribute("maxOccurs");

        if (minOccurs != null && !minOccurs.isEmpty()) {
            try {
                rule.setMinOccurs(Integer.parseInt(minOccurs));
            } catch (NumberFormatException e) {
                logger.warn("Invalid minOccurs in group: {}", minOccurs);
            }
        }

        if (maxOccurs != null && !maxOccurs.isEmpty()) {
            try {
                if ("unbounded".equals(maxOccurs)) {
                    rule.setMaxOccurs(Integer.MAX_VALUE);
                } else {
                    rule.setMaxOccurs(Integer.parseInt(maxOccurs));
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid maxOccurs in group: {}", maxOccurs);
            }
        }

        // Extract element names in the group
        NodeList elements = groupElement.getElementsByTagNameNS(XSD_NAMESPACE, "element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            String name = element.getAttribute("name");
            String ref = element.getAttribute("ref");

            String elementName = name != null && !name.isEmpty() ? name : ref;
            if (elementName != null && !elementName.isEmpty()) {
                rule.addElement(elementName);
            }
        }

        rule.setDescription(type.getDescription() + " with " + rule.getElementCount() + " elements");

        return rule.getElementCount() > 0 ? rule : null;
    }

    /**
     * Validates constraint consistency within a schema
     */
    public List<String> validateConstraintConsistency(Map<String, List<ElementConstraint>> constraintMap) {
        List<String> inconsistencies = new ArrayList<>();

        for (Map.Entry<String, List<ElementConstraint>> entry : constraintMap.entrySet()) {
            String elementName = entry.getKey();
            List<ElementConstraint> constraints = entry.getValue();

            inconsistencies.addAll(validateElementConstraints(elementName, constraints));
        }

        return inconsistencies;
    }

    /**
     * Validates constraints for a single element
     */
    private List<String> validateElementConstraints(String elementName, List<ElementConstraint> constraints) {
        List<String> issues = new ArrayList<>();

        // Check for conflicting length constraints
        Integer minLength = null;
        Integer maxLength = null;

        for (ElementConstraint constraint : constraints) {
            switch (constraint.getConstraintType()) {
                case MIN_LENGTH:
                    try {
                        minLength = Integer.parseInt(constraint.getValue());
                    } catch (NumberFormatException e) {
                        issues.add(elementName + ": Invalid minLength value: " + constraint.getValue());
                    }
                    break;
                case MAX_LENGTH:
                    try {
                        maxLength = Integer.parseInt(constraint.getValue());
                    } catch (NumberFormatException e) {
                        issues.add(elementName + ": Invalid maxLength value: " + constraint.getValue());
                    }
                    break;
            }
        }

        if (minLength != null && maxLength != null && minLength > maxLength) {
            issues.add(elementName + ": minLength (" + minLength + ") is greater than maxLength (" + maxLength + ")");
        }

        // Check for conflicting value constraints
        validateValueConstraints(elementName, constraints, issues);

        return issues;
    }

    /**
     * Validates value-based constraints for consistency
     */
    private void validateValueConstraints(String elementName, List<ElementConstraint> constraints, List<String> issues) {
        Double minInclusive = null;
        Double maxInclusive = null;
        Double minExclusive = null;
        Double maxExclusive = null;

        for (ElementConstraint constraint : constraints) {
            try {
                switch (constraint.getConstraintType()) {
                    case MIN_INCLUSIVE:
                        minInclusive = Double.parseDouble(constraint.getValue());
                        break;
                    case MAX_INCLUSIVE:
                        maxInclusive = Double.parseDouble(constraint.getValue());
                        break;
                    case MIN_EXCLUSIVE:
                        minExclusive = Double.parseDouble(constraint.getValue());
                        break;
                    case MAX_EXCLUSIVE:
                        maxExclusive = Double.parseDouble(constraint.getValue());
                        break;
                }
            } catch (NumberFormatException e) {
                // Skip non-numeric constraints
            }
        }

        // Validate constraint combinations
        if (minInclusive != null && maxInclusive != null && minInclusive > maxInclusive) {
            issues.add(elementName + ": minInclusive is greater than maxInclusive");
        }

        if (minExclusive != null && maxExclusive != null && minExclusive >= maxExclusive) {
            issues.add(elementName + ": minExclusive is greater than or equal to maxExclusive");
        }

        if (minInclusive != null && maxExclusive != null && minInclusive >= maxExclusive) {
            issues.add(elementName + ": minInclusive is greater than or equal to maxExclusive");
        }

        if (minExclusive != null && maxInclusive != null && minExclusive >= maxInclusive) {
            issues.add(elementName + ": minExclusive is greater than or equal to maxInclusive");
        }
    }
}
