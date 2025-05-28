package com.xmlfixer.validation;

import com.xmlfixer.common.exceptions.ValidationException;
import com.xmlfixer.schema.model.*;
import com.xmlfixer.validation.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Streaming validator for memory-efficient processing of large XML files
 * Uses SAX parsing to validate XML against schema constraints with precise error location tracking
 */
@Singleton
public class StreamingValidator {

    private static final Logger logger = LoggerFactory.getLogger(StreamingValidator.class);

    private final ErrorCollector errorCollector;
    private SAXParserFactory saxParserFactory;

    @Inject
    public StreamingValidator(ErrorCollector errorCollector) {
        this.errorCollector = errorCollector;
        this.saxParserFactory = SAXParserFactory.newInstance();
        this.saxParserFactory.setNamespaceAware(true);
        this.saxParserFactory.setValidating(false); // We'll do custom validation
        logger.info("StreamingValidator initialized");
    }

    /**
     * Validates XML using streaming approach with schema constraints
     */
    public ValidationResult validateStreaming(File xmlFile, SchemaElement rootSchema,
                                              Map<String, List<ValidationRule>> validationRules) {
        logger.info("Starting streaming validation of: {}", xmlFile.getName());

        ValidationResult result = new ValidationResult();
        result.setXmlFile(xmlFile);

        try (InputStream inputStream = new FileInputStream(xmlFile)) {
            SAXParser saxParser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();

            // Create validation handler
            StreamingValidationHandler handler = new StreamingValidationHandler(
                    rootSchema, validationRules, errorCollector);

            xmlReader.setContentHandler(handler);
            xmlReader.setErrorHandler(handler);

            // Parse and validate
            InputSource inputSource = new InputSource(inputStream);
            xmlReader.parse(inputSource);

            // Collect results
            result.setErrors(handler.getErrors());
            result.setWarnings(handler.getWarnings());
            result.setValid(handler.getErrors().isEmpty());

            logger.info("Streaming validation completed with {} errors, {} warnings",
                    result.getErrorCount(), result.getWarningCount());

        } catch (Exception e) {
            logger.error("Streaming validation failed", e);
            ValidationError error = new ValidationError(
                    ErrorType.MALFORMED_XML,
                    "XML parsing failed: " + e.getMessage()
            );
            result.addError(error);
            result.setValid(false);
        }

        return result;
    }

    /**
     * Inner class that handles SAX events and performs validation
     */
    private static class StreamingValidationHandler extends DefaultHandler implements ErrorHandler {

        private final SchemaElement rootSchema;
        private final Map<String, List<ValidationRule>> validationRules;
        private final ErrorCollector errorCollector;

        // Validation state
        private final Stack<ElementContext> elementStack;
        private final Map<String, Integer> elementOccurrences;
        private final List<ValidationError> errors;
        private final List<ValidationError> warnings;

        // Location tracking
        private Locator locator;
        private int currentLine = 1;
        private int currentColumn = 1;

        // Content handling
        private StringBuilder currentContent;
        private boolean collectingContent;

        public StreamingValidationHandler(SchemaElement rootSchema,
                                          Map<String, List<ValidationRule>> validationRules,
                                          ErrorCollector errorCollector) {
            this.rootSchema = rootSchema;
            this.validationRules = validationRules != null ? validationRules : new HashMap<>();
            this.errorCollector = errorCollector;

            this.elementStack = new Stack<>();
            this.elementOccurrences = new HashMap<>();
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.currentContent = new StringBuilder();
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startDocument() throws SAXException {
            logger.debug("Starting document validation");
            elementStack.clear();
            elementOccurrences.clear();
            errors.clear();
            warnings.clear();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {

            updateLocation();
            String elementName = localName.isEmpty() ? qName : localName;

            logger.debug("Start element: {} at line {}, column {}",
                    elementName, currentLine, currentColumn);

            // Create element context
            ElementContext context = new ElementContext(elementName, currentLine, currentColumn);
            context.setNamespaceURI(uri);
            context.setQualifiedName(qName);

            // Validate element
            validateElement(context, attributes);

            // Push to stack
            elementStack.push(context);

            // Update occurrence count
            String path = getCurrentPath();
            elementOccurrences.merge(path, 1, Integer::sum);

            // Reset content collection
            currentContent.setLength(0);
            collectingContent = true;
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            updateLocation();

            if (!elementStack.isEmpty()) {
                ElementContext context = elementStack.pop();
                String elementName = context.getElementName();

                logger.debug("End element: {} at line {}", elementName, currentLine);

                // Validate element content if collected
                if (collectingContent && currentContent.length() > 0) {
                    validateElementContent(context, currentContent.toString().trim());
                }

                // Validate child elements completeness
                validateChildElements(context);
            }

            collectingContent = false;
            currentContent.setLength(0);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (collectingContent) {
                currentContent.append(ch, start, length);
            }
        }

        @Override
        public void endDocument() throws SAXException {
            logger.debug("Document validation completed");

            // Final validation checks
            performFinalValidation();

            // Report all collected errors
            errorCollector.reportErrors(errors);
            errorCollector.reportWarnings(warnings);
        }

        /**
         * Validates an element against schema constraints
         */
        private void validateElement(ElementContext context, Attributes attributes) {
            String elementName = context.getElementName();
            String parentPath = getParentPath();

            // Find schema element definition
            SchemaElement schemaElement = findSchemaElement(elementName, parentPath);

            if (schemaElement == null) {
                // Unexpected element
                addError(ErrorType.UNEXPECTED_ELEMENT,
                        String.format("Unexpected element '%s' at path: %s", elementName, getCurrentPath()),
                        context.getLineNumber(), context.getColumnNumber(), elementName);
                return;
            }

            context.setSchemaElement(schemaElement);

            // Validate attributes
            validateAttributes(context, attributes, schemaElement);

            // Check element occurrence constraints
            validateElementOccurrence(context, schemaElement);

            // Apply validation rules
            applyValidationRules(context, elementName);
        }

        /**
         * Validates element attributes
         */
        private void validateAttributes(ElementContext context, Attributes attributes,
                                        SchemaElement schemaElement) {
            // Check for required attributes
            // This would need to be enhanced based on actual schema attribute definitions

            for (int i = 0; i < attributes.getLength(); i++) {
                String attrName = attributes.getLocalName(i);
                if (attrName.isEmpty()) {
                    attrName = attributes.getQName(i);
                }
                String attrValue = attributes.getValue(i);

                // Validate attribute against schema rules
                validateAttributeValue(context, attrName, attrValue);
            }
        }

        /**
         * Validates element content against data type constraints
         */
        private void validateElementContent(ElementContext context, String content) {
            if (context.getSchemaElement() == null) {
                return;
            }

            SchemaElement schemaElement = context.getSchemaElement();
            String elementName = context.getElementName();

            // Apply data type validation
            if (schemaElement.getType() != null && !schemaElement.hasChildren()) {
                List<ValidationRule> rules = validationRules.get(elementName);
                if (rules != null) {
                    for (ValidationRule rule : rules) {
                        if (rule.isDataTypeRule()) {
                            validateDataType(context, content, rule);
                        }
                    }
                }
            }

            // Apply constraint validation
            if (schemaElement.hasConstraints()) {
                for (ElementConstraint constraint : schemaElement.getConstraints()) {
                    validateConstraint(context, content, constraint);
                }
            }
        }

        /**
         * Validates data type according to rule
         */
        private void validateDataType(ElementContext context, String content, ValidationRule rule) {
            if (!rule.validate(content)) {
                String message = String.format(
                        "Invalid %s value '%s' for element '%s'",
                        rule.getDataType(), content, context.getElementName()
                );

                addError(ErrorType.INVALID_DATA_TYPE, message,
                        context.getLineNumber(), context.getColumnNumber(),
                        context.getElementName());
            }
        }

        /**
         * Validates content against a constraint
         */
        private void validateConstraint(ElementContext context, String content,
                                        ElementConstraint constraint) {
            boolean valid = true;
            String errorMessage = null;

            switch (constraint.getConstraintType()) {
                case PATTERN:
                    if (!content.matches(constraint.getValue())) {
                        valid = false;
                        errorMessage = String.format(
                                "Value '%s' does not match pattern '%s'",
                                content, constraint.getValue()
                        );
                    }
                    break;

                case ENUMERATION:
                    String[] allowedValues = constraint.getValue().split(",");
                    valid = Arrays.asList(allowedValues).contains(content);
                    if (!valid) {
                        errorMessage = String.format(
                                "Value '%s' is not in allowed values: %s",
                                content, constraint.getValue()
                        );
                    }
                    break;

                case MIN_LENGTH:
                    int minLen = Integer.parseInt(constraint.getValue());
                    if (content.length() < minLen) {
                        valid = false;
                        errorMessage = String.format(
                                "Value length %d is less than minimum %d",
                                content.length(), minLen
                        );
                    }
                    break;

                case MAX_LENGTH:
                    int maxLen = Integer.parseInt(constraint.getValue());
                    if (content.length() > maxLen) {
                        valid = false;
                        errorMessage = String.format(
                                "Value length %d exceeds maximum %d",
                                content.length(), maxLen
                        );
                    }
                    break;

                // Add more constraint types as needed
            }

            if (!valid && errorMessage != null) {
                addError(ErrorType.CONSTRAINT_VIOLATION, errorMessage,
                        context.getLineNumber(), context.getColumnNumber(),
                        context.getElementName());
            }
        }

        /**
         * Validates element occurrence constraints
         */
        private void validateElementOccurrence(ElementContext context, SchemaElement schemaElement) {
            String path = getCurrentPath();
            int occurrences = elementOccurrences.getOrDefault(path, 0);

            if (occurrences > schemaElement.getMaxOccurs()) {
                String message = String.format(
                        "Element '%s' appears %d times, but maximum allowed is %d",
                        context.getElementName(), occurrences, schemaElement.getMaxOccurs()
                );
                addError(ErrorType.TOO_MANY_OCCURRENCES, message,
                        context.getLineNumber(), context.getColumnNumber(),
                        context.getElementName());
            }
        }

        /**
         * Validates child elements when parent element closes
         */
        private void validateChildElements(ElementContext parentContext) {
            if (parentContext.getSchemaElement() == null ||
                    !parentContext.getSchemaElement().hasChildren()) {
                return;
            }

            SchemaElement parentSchema = parentContext.getSchemaElement();
            Set<String> requiredChildren = new HashSet<>();

            // Collect required children
            for (SchemaElement child : parentSchema.getChildren()) {
                if (child.isRequired()) {
                    requiredChildren.add(child.getName());
                }
            }

            // Check which required children are missing
            for (String requiredChild : requiredChildren) {
                String childPath = getCurrentPath() + "/" + requiredChild;
                if (!elementOccurrences.containsKey(childPath)) {
                    String message = String.format(
                            "Required element '%s' is missing in '%s'",
                            requiredChild, parentContext.getElementName()
                    );
                    addError(ErrorType.MISSING_REQUIRED_ELEMENT, message,
                            parentContext.getLineNumber(), parentContext.getColumnNumber(),
                            requiredChild);
                }
            }
        }

        /**
         * Applies validation rules to the element
         */
        private void applyValidationRules(ElementContext context, String elementName) {
            List<ValidationRule> rules = validationRules.get(elementName);
            if (rules == null) {
                return;
            }

            for (ValidationRule rule : rules) {
                if (rule.getRuleType() == ValidationRule.RuleType.ELEMENT_REQUIRED &&
                        rule.isRequired()) {
                    // Mark element as validated (required element found)
                    context.setValidated(true);
                }
            }
        }

        /**
         * Performs final validation checks at document end
         */
        private void performFinalValidation() {
            // Check for missing required elements at root level
            if (rootSchema != null && rootSchema.hasChildren()) {
                for (SchemaElement child : rootSchema.getChildren()) {
                    if (child.isRequired()) {
                        String path = "/" + rootSchema.getName() + "/" + child.getName();
                        if (!elementOccurrences.containsKey(path)) {
                            addError(ErrorType.MISSING_REQUIRED_ELEMENT,
                                    String.format("Required element '%s' is missing", child.getName()),
                                    1, 1, child.getName());
                        }
                    }
                }
            }
        }

        /**
         * Validates attribute values
         */
        private void validateAttributeValue(ElementContext context, String attrName, String attrValue) {
            // Apply attribute validation rules
            String elementName = context.getElementName();
            List<ValidationRule> rules = validationRules.get(elementName);

            if (rules != null) {
                for (ValidationRule rule : rules) {
                    if (rule.isAttributeRule() &&
                            attrName.equals(rule.getAttributeName())) {
                        if (!rule.validate(attrValue)) {
                            addError(ErrorType.INVALID_ATTRIBUTE_VALUE,
                                    String.format("Invalid value '%s' for attribute '%s'",
                                            attrValue, attrName),
                                    context.getLineNumber(), context.getColumnNumber(),
                                    context.getElementName());
                        }
                    }
                }
            }
        }

        /**
         * Finds schema element definition for the given element
         */
        private SchemaElement findSchemaElement(String elementName, String parentPath) {
            // Simple implementation - would need enhancement for complex schemas
            if (elementStack.isEmpty() && rootSchema != null &&
                    rootSchema.getName().equals(elementName)) {
                return rootSchema;
            }

            if (!elementStack.isEmpty()) {
                ElementContext parent = elementStack.peek();
                if (parent.getSchemaElement() != null &&
                        parent.getSchemaElement().hasChildren()) {
                    for (SchemaElement child : parent.getSchemaElement().getChildren()) {
                        if (child.getName().equals(elementName)) {
                            return child;
                        }
                    }
                }
            }

            return null;
        }

        /**
         * Updates current location from locator
         */
        private void updateLocation() {
            if (locator != null) {
                currentLine = locator.getLineNumber();
                currentColumn = locator.getColumnNumber();
            }
        }

        /**
         * Gets current element path
         */
        private String getCurrentPath() {
            StringBuilder path = new StringBuilder();
            for (ElementContext ctx : elementStack) {
                path.append("/").append(ctx.getElementName());
            }
            return path.toString();
        }

        /**
         * Gets parent element path
         */
        private String getParentPath() {
            if (elementStack.isEmpty()) {
                return "";
            }

            StringBuilder path = new StringBuilder();
            for (int i = 0; i < elementStack.size() - 1; i++) {
                path.append("/").append(elementStack.get(i).getElementName());
            }
            return path.toString();
        }

        /**
         * Adds a validation error
         */
        private void addError(ErrorType errorType, String message, int line, int column,
                              String elementName) {
            ValidationError error = new ValidationError(errorType, message, line, column);
            error.setElementName(elementName);
            error.setxPath(getCurrentPath());
            errors.add(error);

            logger.debug("Validation error: {} at line {}, column {}", message, line, column);
        }

        /**
         * Adds a validation warning
         */
        private void addWarning(ErrorType errorType, String message, int line, int column,
                                String elementName) {
            ValidationError warning = new ValidationError(errorType, message, line, column);
            warning.setSeverity(ValidationError.Severity.WARNING);
            warning.setElementName(elementName);
            warning.setxPath(getCurrentPath());
            warnings.add(warning);

            logger.debug("Validation warning: {} at line {}, column {}", message, line, column);
        }

        // SAX ErrorHandler methods
        @Override
        public void warning(SAXParseException e) throws SAXException {
            addWarning(ErrorType.MALFORMED_XML, e.getMessage(),
                    e.getLineNumber(), e.getColumnNumber(), "");
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            addError(ErrorType.MALFORMED_XML, e.getMessage(),
                    e.getLineNumber(), e.getColumnNumber(), "");
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            addError(ErrorType.MALFORMED_XML, "Fatal: " + e.getMessage(),
                    e.getLineNumber(), e.getColumnNumber(), "");
            throw e; // Re-throw to stop parsing
        }

        public List<ValidationError> getErrors() { return errors; }
        public List<ValidationError> getWarnings() { return warnings; }
    }

    /**
     * Inner class to maintain element context during validation
     */
    private static class ElementContext {
        private final String elementName;
        private final int lineNumber;
        private final int columnNumber;
        private String namespaceURI;
        private String qualifiedName;
        private SchemaElement schemaElement;
        private boolean validated;
        private Map<String, String> attributes;

        public ElementContext(String elementName, int lineNumber, int columnNumber) {
            this.elementName = elementName;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.attributes = new HashMap<>();
        }

        // Getters and setters
        public String getElementName() { return elementName; }
        public int getLineNumber() { return lineNumber; }
        public int getColumnNumber() { return columnNumber; }

        public String getNamespaceURI() { return namespaceURI; }
        public void setNamespaceURI(String namespaceURI) { this.namespaceURI = namespaceURI; }

        public String getQualifiedName() { return qualifiedName; }
        public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }

        public SchemaElement getSchemaElement() { return schemaElement; }
        public void setSchemaElement(SchemaElement schemaElement) { this.schemaElement = schemaElement; }

        public boolean isValidated() { return validated; }
        public void setValidated(boolean validated) { this.validated = validated; }

        public Map<String, String> getAttributes() { return attributes; }
    }
}
