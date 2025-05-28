package com.xmlfixer.schema.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents ordering rules for schema elements (sequence, choice, all)
 */
public class OrderingRule {

    public enum OrderingType {
        SEQUENCE("sequence", "Elements must appear in specified order"),
        CHOICE("choice", "Only one of the specified elements should appear"),
        ALL("all", "All elements must appear but order is not important");

        private final String xmlName;
        private final String description;

        OrderingType(String xmlName, String description) {
            this.xmlName = xmlName;
            this.description = description;
        }

        public String getXmlName() { return xmlName; }
        public String getDescription() { return description; }
    }

    private OrderingType type;
    private boolean strict;
    private int minOccurs;
    private int maxOccurs;
    private List<String> elementOrder;
    private String groupName;
    private String description;

    public OrderingRule() {
        this.elementOrder = new ArrayList<>();
        this.minOccurs = 1;
        this.maxOccurs = 1;
        this.strict = true;
    }

    public OrderingRule(OrderingType type) {
        this();
        this.type = type;
        this.strict = (type == OrderingType.SEQUENCE);
    }

    // Basic properties
    public OrderingType getType() { return type; }
    public void setType(OrderingType type) {
        this.type = type;
        if (type == OrderingType.SEQUENCE) {
            this.strict = true;
        }
    }

    public boolean isStrict() { return strict; }
    public void setStrict(boolean strict) { this.strict = strict; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Occurrence constraints
    public int getMinOccurs() { return minOccurs; }
    public void setMinOccurs(int minOccurs) { this.minOccurs = minOccurs; }

    public int getMaxOccurs() { return maxOccurs; }
    public void setMaxOccurs(int maxOccurs) { this.maxOccurs = maxOccurs; }

    // Element ordering
    public List<String> getElementOrder() { return elementOrder; }
    public void setElementOrder(List<String> elementOrder) { this.elementOrder = elementOrder; }

    public void addElement(String elementName) {
        if (this.elementOrder == null) {
            this.elementOrder = new ArrayList<>();
        }
        this.elementOrder.add(elementName);
    }

    public void addElements(List<String> elementNames) {
        if (this.elementOrder == null) {
            this.elementOrder = new ArrayList<>();
        }
        this.elementOrder.addAll(elementNames);
    }

    // Utility methods
    public boolean isOptional() {
        return minOccurs == 0;
    }

    public boolean isUnbounded() {
        return maxOccurs == Integer.MAX_VALUE;
    }

    public boolean allowsMultiple() {
        return maxOccurs > 1 || isUnbounded();
    }

    public boolean hasElementOrder() {
        return elementOrder != null && !elementOrder.isEmpty();
    }

    public int getElementCount() {
        return elementOrder != null ? elementOrder.size() : 0;
    }

    public boolean containsElement(String elementName) {
        return elementOrder != null && elementOrder.contains(elementName);
    }

    public int getElementPosition(String elementName) {
        if (elementOrder == null) {
            return -1;
        }
        return elementOrder.indexOf(elementName);
    }

    /**
     * Validates if the given element order matches this rule
     */
    public boolean validateOrder(List<String> actualOrder) {
        if (actualOrder == null || actualOrder.isEmpty()) {
            return minOccurs == 0;
        }

        switch (type) {
            case SEQUENCE:
                return validateSequenceOrder(actualOrder);
            case CHOICE:
                return validateChoiceOrder(actualOrder);
            case ALL:
                return validateAllOrder(actualOrder);
            default:
                return false;
        }
    }

    private boolean validateSequenceOrder(List<String> actualOrder) {
        if (elementOrder == null || elementOrder.isEmpty()) {
            return true;
        }

        int expectedIndex = 0;
        for (String actualElement : actualOrder) {
            if (expectedIndex >= elementOrder.size()) {
                return false; // Too many elements
            }

            if (!elementOrder.get(expectedIndex).equals(actualElement)) {
                return false; // Wrong order
            }
            expectedIndex++;
        }

        return expectedIndex >= elementOrder.size() || minOccurs == 0;
    }

    private boolean validateChoiceOrder(List<String> actualOrder) {
        if (elementOrder == null || elementOrder.isEmpty()) {
            return true;
        }

        // For choice, only one element should be present
        if (actualOrder.size() > 1) {
            return false;
        }

        String actualElement = actualOrder.get(0);
        return elementOrder.contains(actualElement);
    }

    private boolean validateAllOrder(List<String> actualOrder) {
        if (elementOrder == null || elementOrder.isEmpty()) {
            return true;
        }

        // For all, every expected element should be present (order doesn't matter)
        for (String expectedElement : elementOrder) {
            if (!actualOrder.contains(expectedElement)) {
                return false;
            }
        }

        // Check for unexpected elements
        for (String actualElement : actualOrder) {
            if (!elementOrder.contains(actualElement)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format("OrderingRule{type=%s, strict=%s, elements=%d, minOccurs=%d, maxOccurs=%d}",
                type, strict, getElementCount(), minOccurs, maxOccurs);
    }
}
