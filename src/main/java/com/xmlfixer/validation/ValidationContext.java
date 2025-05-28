package com.xmlfixer.validation;

import com.xmlfixer.schema.model.SchemaElement;
import com.xmlfixer.validation.model.ValidationError;

import java.util.*;

/**
 * Tracks validation context during streaming processing for better error reporting
 */
public class ValidationContext {

    // Current position tracking
    private String currentPath;
    private Stack<String> pathStack;
    private int currentLine;
    private int currentColumn;

    // Element tracking
    private Map<String, Integer> elementCounts;
    private Map<String, List<Integer>> elementLines;
    private Stack<SchemaElement> schemaStack;

    // Error context
    private List<ValidationError> contextualErrors;
    private Map<String, Object> contextData;

    // Parent-child relationships
    private Map<String, Set<String>> parentChildMap;
    private String currentParent;

    public ValidationContext() {
        this.pathStack = new Stack<>();
        this.elementCounts = new HashMap<>();
        this.elementLines = new HashMap<>();
        this.schemaStack = new Stack<>();
        this.contextualErrors = new ArrayList<>();
        this.contextData = new HashMap<>();
        this.parentChildMap = new HashMap<>();
        this.currentPath = "";
        this.currentLine = 1;
        this.currentColumn = 1;
    }

    /**
     * Enters a new element context
     */
    public void enterElement(String elementName, int line, int column, SchemaElement schemaElement) {
        // Update path
        pathStack.push(elementName);
        currentPath = buildCurrentPath();

        // Update position
        currentLine = line;
        currentColumn = column;

        // Track element count
        elementCounts.merge(currentPath, 1, Integer::sum);

        // Track element lines
        elementLines.computeIfAbsent(currentPath, k -> new ArrayList<>()).add(line);

        // Update schema context
        if (schemaElement != null) {
            schemaStack.push(schemaElement);
        }

        // Track parent-child relationships
        if (currentParent != null) {
            parentChildMap.computeIfAbsent(currentParent, k -> new HashSet<>()).add(elementName);
        }
        currentParent = currentPath;
    }

    /**
     * Exits the current element context
     */
    public void exitElement() {
        if (!pathStack.isEmpty()) {
            pathStack.pop();
            currentPath = buildCurrentPath();

            if (!schemaStack.isEmpty()) {
                schemaStack.pop();
            }

            currentParent = currentPath.isEmpty() ? null : currentPath;
        }
    }

    /**
     * Gets the current element path
     */
    public String getCurrentPath() {
        return currentPath;
    }

    /**
     * Gets the parent path
     */
    public String getParentPath() {
        if (pathStack.size() <= 1) {
            return "";
        }

        // Build parent path
        StringBuilder parentPath = new StringBuilder();
        for (int i = 0; i < pathStack.size() - 1; i++) {
            if (i > 0) parentPath.append("/");
            parentPath.append(pathStack.get(i));
        }
        return parentPath.toString();
    }

    /**
     * Gets the current element name
     */
    public String getCurrentElement() {
        return pathStack.isEmpty() ? null : pathStack.peek();
    }

    /**
     * Gets the occurrence count for the current element
     */
    public int getElementOccurrenceCount(String path) {
        return elementCounts.getOrDefault(path, 0);
    }

    /**
     * Gets all line numbers where an element appears
     */
    public List<Integer> getElementLines(String path) {
        return elementLines.getOrDefault(path, Collections.emptyList());
    }

    /**
     * Gets the current schema element
     */
    public SchemaElement getCurrentSchemaElement() {
        return schemaStack.isEmpty() ? null : schemaStack.peek();
    }

    /**
     * Adds contextual data
     */
    public void addContextData(String key, Object value) {
        contextData.put(key, value);
    }

    /**
     * Gets contextual data
     */
    public Object getContextData(String key) {
        return contextData.get(key);
    }

    /**
     * Creates a contextual error with full context information
     */
    public ValidationError createContextualError(ValidationError baseError) {
        // Enhance error with context
        baseError.setxPath(currentPath);
        baseError.setLineNumber(currentLine);
        baseError.setColumnNumber(currentColumn);

        // Add element context
        if (getCurrentElement() != null) {
            baseError.setElementName(getCurrentElement());
        }

        // Add schema rule context
        if (getCurrentSchemaElement() != null) {
            SchemaElement schema = getCurrentSchemaElement();
            String schemaInfo = String.format("Element '%s' (min=%d, max=%d, type=%s)",
                    schema.getName(), schema.getMinOccurs(), schema.getMaxOccurs(), schema.getType());
            baseError.setSchemaRule(schemaInfo);
        }

        contextualErrors.add(baseError);
        return baseError;
    }

    /**
     * Gets siblings of the current element
     */
    public Set<String> getSiblingElements() {
        String parent = getParentPath();
        if (parent.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> siblings = parentChildMap.get(parent);
        if (siblings == null) {
            return Collections.emptySet();
        }

        // Remove current element from siblings
        Set<String> result = new HashSet<>(siblings);
        result.remove(getCurrentElement());
        return result;
    }

    /**
     * Checks if an element has appeared before at this level
     */
    public boolean hasElementAppearedBefore(String elementName) {
        String path = getParentPath().isEmpty() ? elementName : getParentPath() + "/" + elementName;
        return elementCounts.getOrDefault(path, 0) > 0;
    }

    /**
     * Gets the sequence of elements at the current level
     */
    public List<String> getElementSequence() {
        List<String> sequence = new ArrayList<>();
        String parent = getParentPath();

        // Build sequence from element lines
        Map<Integer, String> lineToElement = new HashMap<>();

        for (Map.Entry<String, List<Integer>> entry : elementLines.entrySet()) {
            if (entry.getKey().startsWith(parent)) {
                String elementName = extractElementName(entry.getKey(), parent);
                for (Integer line : entry.getValue()) {
                    lineToElement.put(line, elementName);
                }
            }
        }

        // Sort by line number to get sequence
        lineToElement.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sequence.add(entry.getValue()));

        return sequence;
    }

    /**
     * Generates a validation summary
     */
    public ValidationSummary generateSummary() {
        ValidationSummary summary = new ValidationSummary();

        summary.setTotalElements(elementCounts.size());
        summary.setTotalOccurrences(elementCounts.values().stream().mapToInt(Integer::intValue).sum());
        summary.setElementPaths(new HashSet<>(elementCounts.keySet()));
        summary.setMaxDepth(calculateMaxDepth());
        summary.setRepeatedElements(findRepeatedElements());

        return summary;
    }

    /**
     * Clears the context
     */
    public void clear() {
        pathStack.clear();
        elementCounts.clear();
        elementLines.clear();
        schemaStack.clear();
        contextualErrors.clear();
        contextData.clear();
        parentChildMap.clear();
        currentPath = "";
        currentParent = null;
        currentLine = 1;
        currentColumn = 1;
    }

    // Helper methods

    private String buildCurrentPath() {
        if (pathStack.isEmpty()) {
            return "";
        }
        return "/" + String.join("/", pathStack);
    }

    private String extractElementName(String path, String parentPath) {
        if (parentPath.isEmpty()) {
            return path.substring(1); // Remove leading "/"
        }
        return path.substring(parentPath.length() + 1);
    }

    private int calculateMaxDepth() {
        return elementCounts.keySet().stream()
                .mapToInt(path -> path.split("/").length - 1)
                .max()
                .orElse(0);
    }

    private Set<String> findRepeatedElements() {
        return elementCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    /**
     * Inner class for validation summary
     */
    public static class ValidationSummary {
        private int totalElements;
        private int totalOccurrences;
        private Set<String> elementPaths;
        private int maxDepth;
        private Set<String> repeatedElements;

        // Getters and setters
        public int getTotalElements() { return totalElements; }
        public void setTotalElements(int totalElements) { this.totalElements = totalElements; }

        public int getTotalOccurrences() { return totalOccurrences; }
        public void setTotalOccurrences(int totalOccurrences) { this.totalOccurrences = totalOccurrences; }

        public Set<String> getElementPaths() { return elementPaths; }
        public void setElementPaths(Set<String> elementPaths) { this.elementPaths = elementPaths; }

        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

        public Set<String> getRepeatedElements() { return repeatedElements; }
        public void setRepeatedElements(Set<String> repeatedElements) { this.repeatedElements = repeatedElements; }

        @Override
        public String toString() {
            return String.format("ValidationSummary{elements=%d, occurrences=%d, maxDepth=%d, repeated=%d}",
                    totalElements, totalOccurrences, maxDepth,
                    repeatedElements != null ? repeatedElements.size() : 0);
        }
    }
}
