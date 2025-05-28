package com.xmlfixer.schema;

import com.xmlfixer.common.exceptions.XmlFixerException;
import com.xmlfixer.schema.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing XSD schema files and extracting element definitions and constraints
 */
@Singleton
public class SchemaAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SchemaAnalyzer.class);

    private final SchemaParser schemaParser;

    @Inject
    public SchemaAnalyzer(SchemaParser schemaParser) {
        this.schemaParser = schemaParser;
        logger.info("SchemaAnalyzer initialized");
    }

    /**
     * Analyzes a schema file and returns the complete schema structure
     */
    public SchemaElement analyzeSchema(File schemaFile) {
        logger.info("Analyzing schema file: {}", schemaFile.getName());

        try {
            long startTime = System.currentTimeMillis();

            // Parse the schema document
            Document schemaDocument = schemaParser.parseSchemaDocument(schemaFile);

            // Extract all element definitions
            List<SchemaElement> allElements = schemaParser.extractElements(schemaDocument, schemaFile);

            // Build the schema structure
            SchemaElement rootElement = buildSchemaStructure(allElements, schemaFile);

            // Generate validation rules
            List<ValidationRule> validationRules = generateValidationRules(rootElement);

            // Build element hierarchy
            buildElementHierarchy(rootElement);

            // Calculate analysis metrics
            long endTime = System.currentTimeMillis();
            logger.info("Schema analysis completed for: {} in {}ms. Found {} elements, {} validation rules",
                    schemaFile.getName(), (endTime - startTime),
                    countAllElements(rootElement), validationRules.size());

            return rootElement;

        } catch (Exception e) {
            logger.error("Failed to analyze schema: {}", schemaFile.getName(), e);
            throw new XmlFixerException("Schema analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the complete schema structure from parsed elements
     */
    private SchemaElement buildSchemaStructure(List<SchemaElement> allElements, File schemaFile) {
        if (allElements.isEmpty()) {
            throw new XmlFixerException("No elements found in schema: " + schemaFile.getName());
        }

        // Find the root element (typically the first global element)
        SchemaElement rootElement = findRootElement(allElements);

        if (rootElement == null) {
            // Create a synthetic root if no clear root is found
            rootElement = new SchemaElement("schema_root");
            rootElement.setSchemaFile(schemaFile);
            rootElement.setType("complexType");

            // Add all top-level elements as children
            for (SchemaElement element : allElements) {
                rootElement.addChild(element);
            }
        }

        // Build element index for quick lookup
        Map<String, SchemaElement> elementIndex = buildElementIndex(allElements);

        // Resolve element references and build complete hierarchy
        resolveElementReferences(rootElement, elementIndex);

        return rootElement;
    }

    /**
     * Finds the root element from the list of schema elements
     */
    private SchemaElement findRootElement(List<SchemaElement> elements) {
        // Look for elements that are not referenced by others
        Set<String> referencedElements = new HashSet<>();

        for (SchemaElement element : elements) {
            if (element.hasChildren()) {
                for (SchemaElement child : element.getChildren()) {
                    referencedElements.add(child.getName());
                }
            }
        }

        // Find elements that are not referenced (potential root elements)
        List<SchemaElement> potentialRoots = elements.stream()
                .filter(e -> !referencedElements.contains(e.getName()))
                .collect(Collectors.toList());

        if (!potentialRoots.isEmpty()) {
            return potentialRoots.get(0); // Return the first potential root
        }

        // If all elements are referenced, return the first one
        return elements.get(0);
    }

    /**
     * Builds an index of elements for quick lookup
     */
    private Map<String, SchemaElement> buildElementIndex(List<SchemaElement> elements) {
        Map<String, SchemaElement> index = new HashMap<>();

        for (SchemaElement element : elements) {
            index.put(element.getName(), element);

            // Also index by qualified name if namespace is present
            if (element.getNamespace() != null) {
                index.put(element.getQualifiedName(), element);
            }
        }

        return index;
    }

    /**
     * Resolves element references and builds the complete hierarchy
     */
    private void resolveElementReferences(SchemaElement element, Map<String, SchemaElement> elementIndex) {
        if (element.hasChildren()) {
            List<SchemaElement> resolvedChildren = new ArrayList<>();

            for (SchemaElement child : element.getChildren()) {
                // If child is a reference, resolve it
                SchemaElement resolvedChild = elementIndex.get(child.getName());
                if (resolvedChild != null && resolvedChild != child) {
                    resolvedChildren.add(resolvedChild);
                } else {
                    resolvedChildren.add(child);
                }

                // Recursively resolve children
                resolveElementReferences(child, elementIndex);
            }

            element.setChildren(resolvedChildren);
        }
    }

    /**
     * Generates validation rules from the schema structure
     */
    private List<ValidationRule> generateValidationRules(SchemaElement rootElement) {
        List<ValidationRule> rules = new ArrayList<>();

        // Generate rules recursively
        generateValidationRulesRecursive(rootElement, rules);

        logger.debug("Generated {} validation rules", rules.size());
        return rules;
    }

    /**
     * Recursively generates validation rules for an element and its children
     */
    private void generateValidationRulesRecursive(SchemaElement element, List<ValidationRule> rules) {
        // Generate cardinality rules
        if (element.getMinOccurs() > 0 || element.getMaxOccurs() < Integer.MAX_VALUE) {
            ValidationRule cardinalityRule = new ValidationRule(
                    ValidationRule.RuleType.ELEMENT_CARDINALITY, element.getName());
            cardinalityRule.setMinOccurs(element.getMinOccurs());
            cardinalityRule.setMaxOccurs(element.getMaxOccurs());
            cardinalityRule.setRequired(element.isRequired());
            rules.add(cardinalityRule);
        }

        // Generate required element rules
        if (element.isRequired()) {
            ValidationRule requiredRule = new ValidationRule(
                    ValidationRule.RuleType.ELEMENT_REQUIRED, element.getName());
            requiredRule.setRequired(true);
            rules.add(requiredRule);
        }

        // Generate constraint-based rules
        if (element.hasConstraints()) {
            for (ElementConstraint constraint : element.getConstraints()) {
                ValidationRule constraintRule = createRuleFromConstraint(element, constraint);
                if (constraintRule != null) {
                    rules.add(constraintRule);
                }
            }
        }

        // Generate data type rules
        if (element.getType() != null && !element.getType().equals("complexType")) {
            ValidationRule typeRule = new ValidationRule(
                    ValidationRule.RuleType.DATA_TYPE, element.getName());
            typeRule.setDataType(element.getType());
            rules.add(typeRule);
        }

        // Process child elements
        if (element.hasChildren()) {
            for (SchemaElement child : element.getChildren()) {
                generateValidationRulesRecursive(child, rules);
            }
        }
    }

    /**
     * Creates a validation rule from an element constraint
     */
    private ValidationRule createRuleFromConstraint(SchemaElement element, ElementConstraint constraint) {
        ValidationRule rule = null;

        switch (constraint.getConstraintType()) {
            case PATTERN:
                rule = new ValidationRule(ValidationRule.RuleType.PATTERN_MATCH, element.getName());
                rule.setPattern(constraint.getValue());
                break;

            case ENUMERATION:
                rule = new ValidationRule(ValidationRule.RuleType.ENUMERATION, element.getName());
                rule.setExpectedValue(constraint.getValue());
                break;

            case MIN_LENGTH:
            case MAX_LENGTH:
            case MIN_INCLUSIVE:
            case MAX_INCLUSIVE:
            case MIN_EXCLUSIVE:
            case MAX_EXCLUSIVE:
                rule = new ValidationRule(ValidationRule.RuleType.VALUE_RANGE, element.getName());
                rule.setExpectedValue(constraint.getValue());
                break;

            default:
                logger.debug("No rule mapping for constraint type: {}", constraint.getConstraintType());
        }

        if (rule != null) {
            rule.setRequired(constraint.isRequired());
            rule.setDescription(constraint.getFullDescription());
        }

        return rule;
    }

    /**
     * Builds element hierarchy information
     */
    private void buildElementHierarchy(SchemaElement rootElement) {
        // Build parent-child relationships
        buildParentChildRelationships(rootElement, null);

        // Calculate element depths
        calculateElementDepths(rootElement, 0);

        // Build element paths
        buildElementPaths(rootElement, "");
    }

    /**
     * Builds parent-child relationships
     */
    private void buildParentChildRelationships(SchemaElement element, SchemaElement parent) {
        // Set parent reference (if SchemaElement has parent field)
        // element.setParent(parent);

        if (element.hasChildren()) {
            for (SchemaElement child : element.getChildren()) {
                buildParentChildRelationships(child, element);
            }
        }
    }

    /**
     * Calculates the depth of each element in the hierarchy
     */
    private void calculateElementDepths(SchemaElement element, int depth) {
        // Store depth information (if SchemaElement has depth field)
        // element.setDepth(depth);

        if (element.hasChildren()) {
            for (SchemaElement child : element.getChildren()) {
                calculateElementDepths(child, depth + 1);
            }
        }
    }

    /**
     * Builds XPath-like paths for elements
     */
    private void buildElementPaths(SchemaElement element, String parentPath) {
        String currentPath = parentPath.isEmpty() ? element.getName() : parentPath + "/" + element.getName();
        // Store path information (if SchemaElement has path field)
        // element.setPath(currentPath);

        if (element.hasChildren()) {
            for (SchemaElement child : element.getChildren()) {
                buildElementPaths(child, currentPath);
            }
        }
    }

    /**
     * Counts the total number of elements in the schema
     */
    private int countAllElements(SchemaElement rootElement) {
        int count = 1; // Count the root element

        if (rootElement.hasChildren()) {
            for (SchemaElement child : rootElement.getChildren()) {
                count += countAllElements(child);
            }
        }

        return count;
    }

    /**
     * Validates that a schema file is well-formed and can be processed
     */
    public boolean isValidSchema(File schemaFile) {
        try {
            if (!schemaFile.exists() || !schemaFile.canRead()) {
                return false;
            }

            return schemaParser.isValidSchemaFile(schemaFile);

        } catch (Exception e) {
            logger.debug("Schema validation failed for: {}", schemaFile.getName(), e);
            return false;
        }
    }

    /**
     * Gets basic schema information without full analysis
     */
    public String getSchemaInfo(File schemaFile) {
        try {
            if (!schemaFile.exists()) {
                return "Schema file not found";
            }

            Document doc = schemaParser.parseSchemaDocument(schemaFile);
            List<SchemaElement> elements = schemaParser.extractElements(doc, schemaFile);

            return String.format("Schema: %s (Size: %d bytes, Elements: %d)",
                    schemaFile.getName(), schemaFile.length(), elements.size());

        } catch (Exception e) {
            logger.warn("Failed to get schema info for: {}", schemaFile.getName(), e);
            return "Schema analysis failed: " + e.getMessage();
        }
    }

    /**
     * Finds an element by name in the schema
     */
    public SchemaElement findElement(SchemaElement rootElement, String elementName) {
        if (rootElement.getName().equals(elementName)) {
            return rootElement;
        }

        if (rootElement.hasChildren()) {
            for (SchemaElement child : rootElement.getChildren()) {
                SchemaElement found = findElement(child, elementName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Gets all elements of a specific type
     */
    public List<SchemaElement> getElementsByType(SchemaElement rootElement, String type) {
        List<SchemaElement> result = new ArrayList<>();
        getElementsByTypeRecursive(rootElement, type, result);
        return result;
    }

    private void getElementsByTypeRecursive(SchemaElement element, String type, List<SchemaElement> result) {
        if (type.equals(element.getType())) {
            result.add(element);
        }

        if (element.hasChildren()) {
            for (SchemaElement child : element.getChildren()) {
                getElementsByTypeRecursive(child, type, result);
            }
        }
    }

    /**
     * Gets all required elements in the schema
     */
    public List<SchemaElement> getRequiredElements(SchemaElement rootElement) {
        List<SchemaElement> result = new ArrayList<>();
        getRequiredElementsRecursive(rootElement, result);
        return result;
    }

    private void getRequiredElementsRecursive(SchemaElement element, List<SchemaElement> result) {
        if (element.isRequired()) {
            result.add(element);
        }

        if (element.hasChildren()) {
            for (SchemaElement child : element.getChildren()) {
                getRequiredElementsRecursive(child, result);
            }
        }
    }
}
