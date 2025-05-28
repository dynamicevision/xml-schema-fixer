package com.xmlfixer.validation;

import com.xmlfixer.schema.SchemaAnalyzer;
import com.xmlfixer.schema.SchemaParser;
import com.xmlfixer.parsing.XmlParser;
import com.xmlfixer.validation.model.ValidationResult;
import com.xmlfixer.validation.model.ValidationError;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Demo class to test the streaming validation functionality
 */
public class StreamingValidationDemo {

    public static void main(String[] args) {
        try {
            // Create test files
            File testSchema = createTestSchema();
            File validXml = createValidXml();
            File invalidXml = createInvalidXml();

            // Initialize components
            SchemaParser schemaParser = new SchemaParser();
            SchemaAnalyzer schemaAnalyzer = new SchemaAnalyzer(schemaParser);
            ErrorCollector errorCollector = new ErrorCollector();
            StreamingValidator streamingValidator = new StreamingValidator(errorCollector);
            XmlParser xmlParser = new XmlParser();

            XmlValidator validator = new XmlValidator(
                    streamingValidator, errorCollector, xmlParser, schemaAnalyzer);

            System.out.println("=== XML STREAMING VALIDATION DEMO ===\n");

            // Test 1: Valid XML
            System.out.println("TEST 1: Validating correct XML file");
            System.out.println("-".repeat(50));
            testValidation(validator, validXml, testSchema);

            // Test 2: Invalid XML with various errors
            System.out.println("\nTEST 2: Validating XML with multiple errors");
            System.out.println("-".repeat(50));
            testValidation(validator, invalidXml, testSchema);

            // Test 3: Validation with options
            System.out.println("\nTEST 3: Validation with custom options");
            System.out.println("-".repeat(50));
            XmlValidator.ValidationOptions options = new XmlValidator.ValidationOptions();
            options.setMaxErrors(3);
            options.setIncludeWarnings(true);

            ValidationResult optionsResult = validator.validateWithOptions(invalidXml, testSchema, options);
            System.out.println("Result with max 3 errors: " + optionsResult.getErrorCount() + " errors shown");

            // Test 4: Error analysis
            System.out.println("\nTEST 4: Error Analysis");
            System.out.println("-".repeat(50));
            ErrorCollector.ErrorSummary summary = errorCollector.getErrorSummary();
            System.out.println("Error Summary: " + summary);
            System.out.println("\nDetailed Error Report:");
            System.out.println(errorCollector.generateDetailedReport());

            // Clean up
            testSchema.delete();
            validXml.delete();
            invalidXml.delete();

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testValidation(XmlValidator validator, File xmlFile, File schemaFile) {
        try {
            long startTime = System.currentTimeMillis();
            ValidationResult result = validator.validate(xmlFile, schemaFile);
            long endTime = System.currentTimeMillis();

            System.out.println("File: " + xmlFile.getName());
            System.out.println("Valid: " + result.isValid());
            System.out.println("Errors: " + result.getErrorCount());
            System.out.println("Warnings: " + result.getWarningCount());
            System.out.println("Time: " + (endTime - startTime) + " ms");

            if (!result.isValid()) {
                System.out.println("\nErrors found:");
                for (ValidationError error : result.getErrors()) {
                    System.out.println("  - " + error.getFullMessage());
                    if (error.getExpectedValue() != null) {
                        System.out.println("    Expected: " + error.getExpectedValue());
                    }
                    if (error.getActualValue() != null) {
                        System.out.println("    Actual: " + error.getActualValue());
                    }
                }
            }

            if (result.getWarningCount() > 0) {
                System.out.println("\nWarnings:");
                for (ValidationError warning : result.getWarnings()) {
                    System.out.println("  - " + warning.getFullMessage());
                }
            }

            // Print full report
            System.out.println("\nValidation Report:");
            System.out.println(validator.getValidationReport(result));

        } catch (Exception e) {
            System.err.println("Validation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static File createTestSchema() throws IOException {
        File schema = File.createTempFile("test_schema", ".xsd");

        String schemaContent = " <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "            <xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "                       targetNamespace=\"http://example.com/library\"\n" +
                "                       xmlns:tns=\"http://example.com/library\"\n" +
                "                       elementFormDefault=\"qualified\">\n" +
                "\n" +
                "                <xs:element name=\"library\" type=\"tns:libraryType\"/>\n" +
                "\n" +
                "                <xs:complexType name=\"libraryType\">\n" +
                "                    <xs:sequence>\n" +
                "                        <xs:element name=\"name\" type=\"xs:string\"/>\n" +
                "                        <xs:element name=\"books\" type=\"tns:booksType\"/>\n" +
                "                    </xs:sequence>\n" +
                "                    <xs:attribute name=\"version\" type=\"xs:string\" use=\"required\"/>\n" +
                "                </xs:complexType>\n" +
                "\n" +
                "                <xs:complexType name=\"booksType\">\n" +
                "                    <xs:sequence>\n" +
                "                        <xs:element name=\"book\" type=\"tns:bookType\" minOccurs=\"1\" maxOccurs=\"unbounded\"/>\n" +
                "                    </xs:sequence>\n" +
                "                </xs:complexType>\n" +
                "\n" +
                "                <xs:complexType name=\"bookType\">\n" +
                "                    <xs:sequence>\n" +
                "                        <xs:element name=\"title\" type=\"xs:string\"/>\n" +
                "                        <xs:element name=\"author\" type=\"xs:string\" maxOccurs=\"3\"/>\n" +
                "                        <xs:element name=\"isbn\" type=\"tns:isbnType\"/>\n" +
                "                        <xs:element name=\"year\" type=\"tns:yearType\"/>\n" +
                "                        <xs:element name=\"price\" type=\"tns:priceType\" minOccurs=\"0\"/>\n" +
                "                        <xs:element name=\"category\" type=\"tns:categoryType\"/>\n" +
                "                    </xs:sequence>\n" +
                "                    <xs:attribute name=\"id\" type=\"xs:positiveInteger\" use=\"required\"/>\n" +
                "                </xs:complexType>\n" +
                "\n" +
                "                <xs:simpleType name=\"isbnType\">\n" +
                "                    <xs:restriction base=\"xs:string\">\n" +
                "                        <xs:pattern value=\"\\d{3}-\\d{1,5}-\\d{1,7}-\\d{1,7}-\\d{1}\"/>\n" +
                "                    </xs:restriction>\n" +
                "                </xs:simpleType>\n" +
                "\n" +
                "                <xs:simpleType name=\"yearType\">\n" +
                "                    <xs:restriction base=\"xs:integer\">\n" +
                "                        <xs:minInclusive value=\"1900\"/>\n" +
                "                        <xs:maxInclusive value=\"2100\"/>\n" +
                "                    </xs:restriction>\n" +
                "                </xs:simpleType>\n" +
                "\n" +
                "                <xs:simpleType name=\"priceType\">\n" +
                "                    <xs:restriction base=\"xs:decimal\">\n" +
                "                        <xs:minInclusive value=\"0.00\"/>\n" +
                "                        <xs:maxInclusive value=\"999.99\"/>\n" +
                "                        <xs:fractionDigits value=\"2\"/>\n" +
                "                    </xs:restriction>\n" +
                "                </xs:simpleType>\n" +
                "\n" +
                "                <xs:simpleType name=\"categoryType\">\n" +
                "                    <xs:restriction base=\"xs:string\">\n" +
                "                        <xs:enumeration value=\"Fiction\"/>\n" +
                "                        <xs:enumeration value=\"Non-Fiction\"/>\n" +
                "                        <xs:enumeration value=\"Science\"/>\n" +
                "                        <xs:enumeration value=\"Technology\"/>\n" +
                "                        <xs:enumeration value=\"History\"/>\n" +
                "                    </xs:restriction>\n" +
                "                </xs:simpleType>\n" +
                "\n" +
                "            </xs:schema>";

        try (FileWriter writer = new FileWriter(schema)) {
            writer.write(schemaContent);
        }

        return schema;
    }

    private static File createValidXml() throws IOException {
        File xml = File.createTempFile("valid_xml", ".xml");

        String xmlContent = " <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "            <library xmlns=\"http://example.com/library\" version=\"1.0\">\n" +
                "                <name>City Public Library</name>\n" +
                "                <books>\n" +
                "                    <book id=\"1\">\n" +
                "                        <title>The Great Gatsby</title>\n" +
                "                        <author>F. Scott Fitzgerald</author>\n" +
                "                        <isbn>978-0-7432-7356-5</isbn>\n" +
                "                        <year>1925</year>\n" +
                "                        <price>12.99</price>\n" +
                "                        <category>Fiction</category>\n" +
                "                    </book>\n" +
                "                    <book id=\"2\">\n" +
                "                        <title>Clean Code</title>\n" +
                "                        <author>Robert C. Martin</author>\n" +
                "                        <isbn>978-0-13-235088-4</isbn>\n" +
                "                        <year>2008</year>\n" +
                "                        <price>39.99</price>\n" +
                "                        <category>Technology</category>\n" +
                "                    </book>\n" +
                "                </books>\n" +
                "            </library>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }

    private static File createInvalidXml() throws IOException {
        File xml = File.createTempFile("invalid_xml", ".xml");

        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "            <library xmlns=\"http://example.com/library\">\n" +
                "                <!-- Missing required 'version' attribute -->\n" +
                "                <name>City Public Library</name>\n" +
                "                <description>Extra element not in schema</description>\n" +
                "                <books>\n" +
                "                    <!-- Missing required book elements -->\n" +
                "                </books>\n" +
                "                <book id=\"3\">\n" +
                "                    <!-- Book outside of books container -->\n" +
                "                    <title>Misplaced Book</title>\n" +
                "                    <author>John Doe</author>\n" +
                "                    <author>Jane Doe</author>\n" +
                "                    <author>Jim Doe</author>\n" +
                "                    <author>Jack Doe</author>\n" +
                "                    <!-- Too many authors (max 3) -->\n" +
                "                    <isbn>123-456</isbn>\n" +
                "                    <!-- Invalid ISBN format -->\n" +
                "                    <year>1899</year>\n" +
                "                    <!-- Year below minimum (1900) -->\n" +
                "                    <price>1500.00</price>\n" +
                "                    <!-- Price above maximum (999.99) -->\n" +
                "                    <category>Mystery</category>\n" +
                "                    <!-- Invalid category value -->\n" +
                "                </book>\n" +
                "                <books>\n" +
                "                    <book>\n" +
                "                        <!-- Missing required 'id' attribute -->\n" +
                "                        <title>Book Without ID</title>\n" +
                "                        <author>Anonymous</author>\n" +
                "                        <isbn>978-0-1234-5678-9</isbn>\n" +
                "                        <year>2050</year>\n" +
                "                        <!-- Missing required category -->\n" +
                "                    </book>\n" +
                "                    <book id=\"abc\">\n" +
                "                        <!-- Invalid id type (should be positive integer) -->\n" +
                "                        <title>Invalid ID Book</title>\n" +
                "                        <author></author>\n" +
                "                        <!-- Empty required element -->\n" +
                "                        <isbn>978-0-1234-5678-9</isbn>\n" +
                "                        <year>two thousand</year>\n" +
                "                        <!-- Invalid year type -->\n" +
                "                        <price>29.999</price>\n" +
                "                        <!-- Too many decimal places -->\n" +
                "                        <category>Fiction</category>\n" +
                "                    </book>\n" +
                "                </books>\n" +
                "            </library>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }
}
