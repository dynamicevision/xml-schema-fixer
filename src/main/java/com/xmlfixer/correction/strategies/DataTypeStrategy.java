package com.xmlfixer.correction.strategies;

import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ErrorType;
import com.xmlfixer.validation.model.ValidationError;
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
import java.util.List;
import java.util.Map;
/**
 * Strategy for correcting data type violations and format issues
 */
public class DataTypeStrategy implements CorrectionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(DataTypeStrategy.class);
    private final DomManipulator domManipulator;

    // Common data type patterns
    private static final Map<String, Pattern> DATA_TYPE_PATTERNS = new HashMap<>();
    static {
        DATA_TYPE_PATTERNS.put("int", Pattern.compile("^-?\\d+$"));
        DATA_TYPE_PATTERNS.put("integer", Pattern.compile("^-?\\d+$"));
        DATA_TYPE_PATTERNS.put("decimal", Pattern.compile("^-?\\d+(\\.\\d+)?$"));
        DATA_TYPE_PATTERNS.put("double", Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$"));
        DATA_TYPE_PATTERNS.put("boolean", Pattern.compile("^(true|false|1|0)$"));
        DATA_TYPE_PATTERNS.put("date", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$"));
        DATA_TYPE_PATTERNS.put("dateTime", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"));
    }

    public DataTypeStrategy(DomManipulator domManipulator) {
        this.domManipulator = domManipulator;
    }

    @Override
    public boolean canCorrect(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();
        return errorType == ErrorType.INVALID_DATA_TYPE ||
                errorType == ErrorType.INVALID_FORMAT ||
                errorType == ErrorType.PATTERN_MISMATCH ||
                errorType == ErrorType.INVALID_VALUE_RANGE;
    }

    @Override
    public boolean applyCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        ErrorType errorType = action.getRelatedErrorType();

        switch (errorType) {
            case INVALID_DATA_TYPE:
                return handleDataTypeCorrection(action, document, rootSchema);
            case INVALID_FORMAT:
                return handleFormatCorrection(action, document, rootSchema);
            case PATTERN_MISMATCH:
                return handlePatternCorrection(action, document, rootSchema);
            case INVALID_VALUE_RANGE:
                return handleRangeCorrection(action, document, rootSchema);
            default:
                return false;
        }
    }

    private boolean handleDataTypeCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();
        String currentValue = action.getOldValue();

        logger.debug("Handling data type correction for path: {}, current value: {}", xPath, currentValue);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null) {
                logger.warn("Could not find element at path: {}", xPath);
                return false;
            }

            SchemaElement schemaElement = StrategyHelper.findSchemaElement(rootSchema, element.getNodeName());
            if (schemaElement == null) {
                logger.warn("Could not find schema element for: {}", element.getNodeName());
                return false;
            }

            String dataType = schemaElement.getType();
            String correctedValue = correctDataType(currentValue, dataType);

            if (correctedValue != null && !correctedValue.equals(currentValue)) {
                domManipulator.modifyElementContent(element, correctedValue);
                action.setNewValue(correctedValue);
                logger.debug("Corrected data type: {} -> {}", currentValue, correctedValue);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Error correcting data type for path: {}", xPath, e);
            return false;
        }
    }

    private boolean handleFormatCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();
        String currentValue = action.getOldValue();

        logger.debug("Handling format correction for path: {}, current value: {}", xPath, currentValue);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null) {
                return false;
            }

            String correctedValue = correctCommonFormats(currentValue);

            if (correctedValue != null && !correctedValue.equals(currentValue)) {
                domManipulator.modifyElementContent(element, correctedValue);
                action.setNewValue(correctedValue);
                logger.debug("Corrected format: {} -> {}", currentValue, correctedValue);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Error correcting format for path: {}", xPath, e);
            return false;
        }
    }

    private boolean handlePatternCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();
        String currentValue = action.getOldValue();

        logger.debug("Handling pattern correction for path: {}, current value: {}", xPath, currentValue);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null) {
                return false;
            }

            SchemaElement schemaElement = StrategyHelper.findSchemaElement(rootSchema, element.getNodeName());
            if (schemaElement == null || !schemaElement.hasConstraints()) {
                return false;
            }

            // Find pattern constraint
            String pattern = schemaElement.getConstraints().stream()
                    .filter(c -> c.getConstraintType().name().equals("PATTERN"))
                    .map(c -> c.getValue())
                    .findFirst()
                    .orElse(null);

            if (pattern != null) {
                String correctedValue = correctPattern(currentValue, pattern);
                if (correctedValue != null && !correctedValue.equals(currentValue)) {
                    domManipulator.modifyElementContent(element, correctedValue);
                    action.setNewValue(correctedValue);
                    logger.debug("Corrected pattern: {} -> {}", currentValue, correctedValue);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            logger.error("Error correcting pattern for path: {}", xPath, e);
            return false;
        }
    }

    private boolean handleRangeCorrection(CorrectionAction action, Document document, SchemaElement rootSchema) {
        String xPath = action.getxPath();
        String currentValue = action.getOldValue();

        logger.debug("Handling range correction for path: {}, current value: {}", xPath, currentValue);

        try {
            Element element = domManipulator.findElement(document, xPath);
            if (element == null) {
                return false;
            }

            SchemaElement schemaElement = StrategyHelper.findSchemaElement(rootSchema, element.getNodeName());
            if (schemaElement == null || !schemaElement.hasConstraints()) {
                return false;
            }

            String correctedValue = correctValueRange(currentValue, schemaElement);

            if (correctedValue != null && !correctedValue.equals(currentValue)) {
                domManipulator.modifyElementContent(element, correctedValue);
                action.setNewValue(correctedValue);
                logger.debug("Corrected range: {} -> {}", currentValue, correctedValue);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Error correcting range for path: {}", xPath, e);
            return false;
        }
    }

    private String correctDataType(String value, String dataType) {
        if (value == null || value.trim().isEmpty()) {
            return getDefaultValueForType(dataType);
        }

        String trimmedValue = value.trim();

        switch (dataType.toLowerCase()) {
            case "int":
            case "integer":
                return correctInteger(trimmedValue);
            case "decimal":
            case "double":
                return correctDecimal(trimmedValue);
            case "boolean":
                return correctBoolean(trimmedValue);
            case "date":
                return correctDate(trimmedValue);
            default:
                return trimmedValue;
        }
    }

    private String correctInteger(String value) {
        try {
            // Try to extract numeric part
            String numericPart = value.replaceAll("[^\\d-]", "");
            if (numericPart.isEmpty()) {
                return "0";
            }
            Integer.parseInt(numericPart);
            return numericPart;
        } catch (NumberFormatException e) {
            return "0";
        }
    }

    private String correctDecimal(String value) {
        try {
            // Try to extract numeric part with decimal
            String numericPart = value.replaceAll("[^\\d.-]", "");
            if (numericPart.isEmpty()) {
                return "0.0";
            }
            Double.parseDouble(numericPart);
            return numericPart;
        } catch (NumberFormatException e) {
            return "0.0";
        }
    }

    private String correctBoolean(String value) {
        String lowerValue = value.toLowerCase().trim();
        if (lowerValue.startsWith("t") || lowerValue.equals("1") || lowerValue.equals("yes")) {
            return "true";
        } else if (lowerValue.startsWith("f") || lowerValue.equals("0") || lowerValue.equals("no")) {
            return "false";
        }
        return "false"; // Default to false
    }

    private String correctDate(String value) {
        // Simple date correction - try to extract YYYY-MM-DD pattern
        String datePattern = "\\d{4}-\\d{2}-\\d{2}";
        if (value.matches(".*" + datePattern + ".*")) {
            return value.replaceAll(".*(\\d{4}-\\d{2}-\\d{2}).*", "$1");
        }

        // Try other common formats and convert
        if (value.matches("\\d{2}/\\d{2}/\\d{4}")) {
            String[] parts = value.split("/");
            return parts[2] + "-" + parts[0] + "-" + parts[1];
        }

        return "2000-01-01"; // Default date
    }

    private String correctCommonFormats(String value) {
        if (value == null) return null;

        // Remove extra whitespace
        String corrected = value.trim().replaceAll("\\s+", " ");

        // Fix common formatting issues
        corrected = corrected.replaceAll("([a-z])([A-Z])", "$1 $2"); // camelCase to space
        corrected = corrected.replaceAll("_", " "); // underscore to space

        return corrected;
    }

    private String correctPattern(String value, String pattern) {
        // This is a simplified pattern correction
        // In practice, this would need more sophisticated logic
        if (value == null) return null;

        // For common patterns like phone numbers, emails, etc.
        if (pattern.contains("\\d")) {
            // Extract digits
            String digits = value.replaceAll("[^\\d]", "");
            if (!digits.isEmpty()) {
                return digits;
            }
        }

        return value;
    }

    private String correctValueRange(String value, SchemaElement schemaElement) {
        try {
            double numValue = Double.parseDouble(value);
            double min = Double.MIN_VALUE;
            double max = Double.MAX_VALUE;

            // Extract min/max from constraints
            for (var constraint : schemaElement.getConstraints()) {
                String constraintType = constraint.getConstraintType().name();
                if (constraintType.equals("MIN_INCLUSIVE")) {
                    min = Double.parseDouble(constraint.getValue());
                } else if (constraintType.equals("MAX_INCLUSIVE")) {
                    max = Double.parseDouble(constraint.getValue());
                }
            }

            // Clamp value to range
            if (numValue < min) {
                return String.valueOf((int)min);
            } else if (numValue > max) {
                return String.valueOf((int)max);
            }

        } catch (NumberFormatException e) {
            // Not a numeric value
        }

        return value;
    }

    private String getDefaultValueForType(String dataType) {
        switch (dataType.toLowerCase()) {
            case "int":
            case "integer":
                return "0";
            case "decimal":
            case "double":
                return "0.0";
            case "boolean":
                return "false";
            case "date":
                return "2000-01-01";
            default:
                return "";
        }
    }

    @Override
    public String getStrategyName() {
        return "DataTypeStrategy";
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
