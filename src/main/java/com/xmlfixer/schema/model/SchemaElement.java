package com.xmlfixer.schema.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an element definition from an XSD schema
 */
public class SchemaElement {
    
    private String name;
    private String namespace;
    private String type;
    private File schemaFile;
    private boolean required;
    private int minOccurs;
    private int maxOccurs;
    private List<SchemaElement> children;
    private List<ElementConstraint> constraints;
    private String defaultValue;
    private String documentation;
    
    public SchemaElement() {
        this.children = new ArrayList<>();
        this.constraints = new ArrayList<>();
        this.minOccurs = 1;
        this.maxOccurs = 1;
        this.required = false;
    }
    
    public SchemaElement(String name) {
        this();
        this.name = name;
    }
    
    // Basic properties
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public File getSchemaFile() { return schemaFile; }
    public void setSchemaFile(File schemaFile) { this.schemaFile = schemaFile; }
    
    public String getDocumentation() { return documentation; }
    public void setDocumentation(String documentation) { this.documentation = documentation; }
    
    // Occurrence constraints
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public int getMinOccurs() { return minOccurs; }
    public void setMinOccurs(int minOccurs) { 
        this.minOccurs = minOccurs;
        this.required = minOccurs > 0;
    }
    
    public int getMaxOccurs() { return maxOccurs; }
    public void setMaxOccurs(int maxOccurs) { this.maxOccurs = maxOccurs; }
    
    // Default values
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    
    // Hierarchy
    public List<SchemaElement> getChildren() { return children; }
    public void setChildren(List<SchemaElement> children) { this.children = children; }
    
    public void addChild(SchemaElement child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
    
    // Constraints
    public List<ElementConstraint> getConstraints() { return constraints; }
    public void setConstraints(List<ElementConstraint> constraints) { this.constraints = constraints; }
    
    public void addConstraint(ElementConstraint constraint) {
        if (this.constraints == null) {
            this.constraints = new ArrayList<>();
        }
        this.constraints.add(constraint);
    }
    
    // Utility methods
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
    
    public boolean hasConstraints() {
        return constraints != null && !constraints.isEmpty();
    }
    
    public boolean isOptional() {
        return !required && minOccurs == 0;
    }
    
    public boolean isUnbounded() {
        return maxOccurs == Integer.MAX_VALUE;
    }
    
    public boolean allowsMultiple() {
        return maxOccurs > 1 || isUnbounded();
    }
    
    public String getQualifiedName() {
        if (namespace != null && !namespace.isEmpty()) {
            return namespace + ":" + name;
        }
        return name;
    }
    
    @Override
    public String toString() {
        return String.format("SchemaElement{name='%s', type='%s', required=%s, minOccurs=%d, maxOccurs=%d}", 
            name, type, required, minOccurs, maxOccurs);
    }
}

