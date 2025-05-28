package com.xmlfixer.schema;

import com.xmlfixer.schema.model.ElementConstraint;
import com.xmlfixer.schema.model.SchemaElement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Demo class to test the schema analysis functionality
 */
public class SchemaAnalysisDemo {

    public static void main(String[] args) {
        try {
            // Create a sample XSD file for testing
            File testSchema = createSampleSchema();

            // Initialize the parser and analyzer
            SchemaParser parser = new SchemaParser();
            SchemaAnalyzer analyzer = new SchemaAnalyzer(parser);

            // Test schema validation
            System.out.println("=== Schema Validation Test ===");
            boolean isValid = analyzer.isValidSchema(testSchema);
            System.out.println("Schema is valid: " + isValid);

            // Test schema info extraction
            System.out.println("\n=== Schema Info Test ===");
            String schemaInfo = analyzer.getSchemaInfo(testSchema);
            System.out.println("Schema Info: " + schemaInfo);

            // Test full schema analysis
            System.out.println("\n=== Full Schema Analysis Test ===");
            try {
                SchemaElement rootElement = analyzer.analyzeSchema(testSchema);
                printSchemaStructure(rootElement, 0);
            } catch (Exception e) {
                System.out.println("Schema analysis failed: " + e.getMessage());
                e.printStackTrace();
            }

            // Clean up
            testSchema.delete();

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a sample XSD schema file for testing
     */
    private static File createSampleSchema() throws IOException {
        File tempSchema = File.createTempFile("test_schema", ".xsd");

        String schemaContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "            <xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "                       targetNamespace=\"http://example.com/test\"\n" +
                "                       xmlns:tns=\"http://example.com/test\"\n" +
                "                       elementFormDefault=\"qualified\">\n" +
                "\n" +
                "                <!-- Root element -->\n" +
                "                <xs:element name=\"library\" type=\"tns:libraryType\"/>\n" +
                "\n" +
                "                <!-- Complex type for library -->\n" +
                "                <xs:complexType name=\"libraryType\">\n" +
                "                    <xs:sequence>\n" +
                "                        <xs:element name=\"book\" type=\"tns:bookType\" maxOccurs=\"unbounded\"/>\n" +
                "                    </xs:sequence>\n" +
                "                    <xs:attribute name=\"name\" type=\"xs:string\" use=\"required\"/>\n" +
                "                </xs:complexType>\n" +
                "\n" +
                "                <!-- Complex type for book -->\n" +
                "                <xs:complexType name=\"bookType\">\n" +
                "                    <xs:sequence>\n" +
                "                        <xs:element name=\"title\" type=\"xs:string\"/>\n" +
                "                        <xs:element name=\"author\" type=\"xs:string\" maxOccurs=\"unbounded\"/>\n" +
                "                        <xs:element name=\"isbn\" type=\"tns:isbnType\"/>\n" +
                "                        <xs:element name=\"publishedDate\" type=\"xs:date\"/>\n" +
                "                        <xs:element name=\"price\" type=\"tns:priceType\" minOccurs=\"0\"/>\n" +
                "                    </xs:sequence>\n" +
                "                    <xs:attribute name=\"id\" type=\"xs:positiveInteger\" use=\"required\"/>\n" +
                "                </xs:complexType>\n" +
                "\n" +
                "                <!-- Simple type for ISBN with pattern -->\n" +
                "                <xs:simpleType name=\"isbnType\">\n" +
                "                    <xs:restriction base=\"xs:string\">\n" +
                "                        <xs:pattern value=\"\\d{3}-\\d{1,5}-\\d{1,7}-\\d{1,7}-\\d{1}\"/>\n" +
                "                    </xs:restriction>\n" +
                "                </xs:simpleType>\n" +
                "\n" +
                "                <!-- Simple type for price with constraints -->\n" +
                "                <xs:simpleType name=\"priceType\">\n" +
                "                    <xs:restriction base=\"xs:decimal\">\n" +
                "                        <xs:minInclusive value=\"0.00\"/>\n" +
                "                        <xs:maxInclusive value=\"999.99\"/>\n" +
                "                        <xs:fractionDigits value=\"2\"/>\n" +
                "                    </xs:restriction>\n" +
                "                </xs:simpleType>\n" +
                "\n" +
                "            </xs:schema>";

        try (FileWriter writer = new FileWriter(tempSchema)) {
            writer.write(schemaContent);
        }

        return tempSchema;
    }

    /**
     * Prints the schema structure recursively
     */
    private static void printSchemaStructure(SchemaElement element, int depth) {
        String indent = "  ".repeat(depth);

        System.out.println(indent + "Element: " + element.getName());
        System.out.println(indent + "  Type: " + element.getType());
        System.out.println(indent + "  Required: " + element.isRequired());
        System.out.println(indent + "  MinOccurs: " + element.getMinOccurs());
        System.out.println(indent + "  MaxOccurs: " + (element.isUnbounded() ? "unbounded" : element.getMaxOccurs()));

        if (element.getDefaultValue() != null) {
            System.out.println(indent + "  Default: " + element.getDefaultValue());
        }

        if (element.hasConstraints()) {
            System.out.println(indent + "  Constraints:");
            for (ElementConstraint constraint : element.getConstraints()) {
                System.out.println(indent + "    - " + constraint.getFullDescription());
            }
        }

        if (element.getDocumentation() != null) {
            System.out.println(indent + "  Documentation: " + element.getDocumentation());
        }

        if (element.hasChildren()) {
            System.out.println(indent + "  Children:");
            for (SchemaElement child : element.getChildren()) {
                printSchemaStructure(child, depth + 1);
            }
        }

        System.out.println();
    }
}
