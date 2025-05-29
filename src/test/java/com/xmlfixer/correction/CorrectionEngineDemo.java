package com.xmlfixer.correction;

import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.correction.model.CorrectionAction;
import com.xmlfixer.correction.model.CorrectionResult;
import com.xmlfixer.parsing.XmlParser;
import com.xmlfixer.reporting.ReportGenerator;
import com.xmlfixer.schema.SchemaAnalyzer;
import com.xmlfixer.schema.SchemaParser;
import com.xmlfixer.validation.ErrorCollector;
import com.xmlfixer.validation.StreamingValidator;
import com.xmlfixer.validation.XmlValidator;
import com.xmlfixer.validation.model.ValidationResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Comprehensive demo class to test the complete correction engine implementation
 * Tests all correction strategies: Missing Elements, Ordering, Cardinality, and Data Types
 */
public class CorrectionEngineDemo {

    public static void main(String[] args) {
        try {
            System.out.println("=== XML CORRECTION ENGINE COMPREHENSIVE DEMO ===\n");

            // Create test files
            File testSchema = createComprehensiveSchema();
            File validXml = createValidXml();
            File invalidXml = createInvalidXmlWithAllErrorTypes();

            // Initialize all components
            SchemaParser schemaParser = new SchemaParser();
            SchemaAnalyzer schemaAnalyzer = new SchemaAnalyzer(schemaParser);
            ErrorCollector errorCollector = new ErrorCollector();
            StreamingValidator streamingValidator = new StreamingValidator(errorCollector);
            XmlParser xmlParser = new XmlParser();
            XmlValidator xmlValidator = new XmlValidator(streamingValidator, errorCollector, xmlParser, schemaAnalyzer);

            DomManipulator domManipulator = new DomManipulator();
            CorrectionEngine correctionEngine = new CorrectionEngine(domManipulator);
            ReportGenerator reportGenerator = new ReportGenerator();
            Properties properties = new Properties();

            ApplicationOrchestrator orchestrator = new ApplicationOrchestrator(
                    schemaAnalyzer, xmlValidator, correctionEngine, xmlParser, reportGenerator, properties);

            // Test 1: Validate the valid XML (should pass)
           /* System.out.println("TEST 1: Validating correct XML");
            System.out.println("-".repeat(50));
            testValidation(orchestrator, validXml, testSchema, "Valid XML");
*/
            // Test 2: Validate and correct the invalid XML
            System.out.println("\nTEST 2: Correcting XML with comprehensive errors");
            System.out.println("-".repeat(50));
            testCorrection(orchestrator, invalidXml, testSchema, "Invalid XML with all error types");

            // Test 3: Test specific correction strategies
           /* System.out.println("\nTEST 3: Testing individual correction strategies");
            System.out.println("-".repeat(50));
            testIndividualStrategies(orchestrator, testSchema);

            // Test 4: Test correction capabilities
            System.out.println("\nTEST 4: Correction capabilities analysis");
            System.out.println("-".repeat(50));
            testCorrectionCapabilities(orchestrator);

            // Test 5: Complete processing workflow
            System.out.println("\nTEST 5: Complete processing workflow");
            System.out.println("-".repeat(50));
            testCompleteProcessingWorkflow(orchestrator, invalidXml, testSchema);*/

            // Clean up
            /*testSchema.delete();
            validXml.delete();
            invalidXml.delete();*/

            System.out.println("\n=== DEMO COMPLETED SUCCESSFULLY ===");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testValidation(ApplicationOrchestrator orchestrator, File xmlFile,
                                       File schemaFile, String testName) {
        try {
            System.out.println("Testing: " + testName);

            ValidationResult result = orchestrator.validateXml(xmlFile, schemaFile);

            System.out.println("  Valid: " + result.isValid());
            System.out.println("  Errors: " + result.getErrorCount());
            System.out.println("  Warnings: " + result.getWarningCount());
            System.out.println("  Time: " + result.getValidationTimeMs() + " ms");

            if (!result.isValid()) {
                System.out.println("  Error types found:");
                result.getErrors().forEach(error ->
                        System.out.println("    - " + error.getErrorType() + ": " + error.getMessage()));
            }

        } catch (Exception e) {
            System.err.println("  Test failed: " + e.getMessage());
        }
    }

    private static void testCorrection(ApplicationOrchestrator orchestrator, File xmlFile,
                                       File schemaFile, String testName) {
        try {
            System.out.println("Testing: " + testName);

            // Create output file
            File outputFile = new File(xmlFile.getParent(),
                    xmlFile.getName().replace(".xml", ".corrected.xml"));

            // Perform correction
            CorrectionResult result = orchestrator.fixXml(xmlFile, schemaFile, outputFile);

            System.out.println("  Success: " + result.isSuccess());
            System.out.println("  No changes required: " + result.isNoChangesRequired());
            System.out.println("  Actions applied: " + result.getAppliedActionCount());
            System.out.println("  Actions failed: " + result.getFailedActionCount());
            System.out.println("  Time: " + result.getCorrectionTimeMs() + " ms");

            if (result.getActionsApplied() != null && !result.getActionsApplied().isEmpty()) {
                System.out.println("  Corrections applied:");
                for (CorrectionAction action : result.getActionsApplied()) {
                    System.out.println("    ✓ " + action.getFullDescription());
                }
            }

            if (result.getFailedActions() != null && !result.getFailedActions().isEmpty()) {
                System.out.println("  Failed corrections:");
                for (CorrectionAction action : result.getFailedActions()) {
                    System.out.println("    ✗ " + action.getFullDescription() +
                            " (Reason: " + action.getFailureReason() + ")");
                }
            }

            // Test the corrected file
            if (result.isSuccess() && outputFile.exists()) {
                System.out.println("  Post-correction validation:");
                ValidationResult postValidation = orchestrator.validateXml(outputFile, schemaFile);
                System.out.println("    Valid: " + postValidation.isValid());
                System.out.println("    Remaining errors: " + postValidation.getErrorCount());

                if (result.hasValidationImprovement()) {
                    System.out.println("    Error reduction: " + result.getErrorReduction() + " errors fixed");
                }
            }

            // Clean up output file
            if (outputFile.exists()) {
                outputFile.delete();
            }

        } catch (Exception e) {
            System.err.println("  Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testIndividualStrategies(ApplicationOrchestrator orchestrator, File schemaFile) {
        try {
            // Test Missing Element Strategy
            System.out.println("Testing Missing Element Strategy:");
            File missingElementXml = createXmlWithMissingElements();
            testSingleStrategy(orchestrator, missingElementXml, schemaFile, "Missing Elements");
            missingElementXml.delete();

            // Test Cardinality Strategy
            System.out.println("\nTesting Cardinality Strategy:");
            File cardinalityXml = createXmlWithCardinalityErrors();
            testSingleStrategy(orchestrator, cardinalityXml, schemaFile, "Cardinality Errors");
            cardinalityXml.delete();

            // Test Data Type Strategy
            System.out.println("\nTesting Data Type Strategy:");
            File dataTypeXml = createXmlWithDataTypeErrors();
            testSingleStrategy(orchestrator, dataTypeXml, schemaFile, "Data Type Errors");
            dataTypeXml.delete();

            // Test Ordering Strategy
            System.out.println("\nTesting Element Ordering Strategy:");
            File orderingXml = createXmlWithOrderingErrors();
            testSingleStrategy(orchestrator, orderingXml, schemaFile, "Ordering Errors");
            orderingXml.delete();

        } catch (Exception e) {
            System.err.println("Individual strategy testing failed: " + e.getMessage());
        }
    }

    private static void testSingleStrategy(ApplicationOrchestrator orchestrator, File xmlFile,
                                           File schemaFile, String strategyName) {
        try {
            File outputFile = new File(xmlFile.getParent(),
                    xmlFile.getName().replace(".xml", ".strategy_test.xml"));

            ValidationResult beforeValidation = orchestrator.validateXml(xmlFile, schemaFile);
            System.out.println("  " + strategyName + " - Errors before: " + beforeValidation.getErrorCount());

            CorrectionResult correction = orchestrator.fixXml(xmlFile, schemaFile, outputFile);
            System.out.println("  " + strategyName + " - Corrections applied: " +
                    correction.getAppliedActionCount());

            if (outputFile.exists()) {
                ValidationResult afterValidation = orchestrator.validateXml(outputFile, schemaFile);
                System.out.println("  " + strategyName + " - Errors after: " + afterValidation.getErrorCount());
                System.out.println("  " + strategyName + " - Success rate: " +
                        String.format("%.1f%%",
                                (double)(beforeValidation.getErrorCount() - afterValidation.getErrorCount()) /
                                        beforeValidation.getErrorCount() * 100));
                outputFile.delete();
            }

        } catch (Exception e) {
            System.err.println("  Strategy test failed: " + e.getMessage());
        }
    }

    private static void testCorrectionCapabilities(ApplicationOrchestrator orchestrator) {
        try {
            ApplicationOrchestrator.CorrectionCapabilities capabilities =
                    orchestrator.getCorrectionCapabilities();

            System.out.println("Supported Error Types: " +
                    capabilities.getSupportedErrorTypes().size());
            System.out.println("Available Strategies: " +
                    capabilities.getStrategiesAvailable());

            System.out.println("Correction Capabilities:");
            capabilities.getCapabilities().forEach((name, description) ->
                    System.out.println("  • " + name + ": " + description));

        } catch (Exception e) {
            System.err.println("Capabilities test failed: " + e.getMessage());
        }
    }

    private static void testCompleteProcessingWorkflow(ApplicationOrchestrator orchestrator,
                                                       File xmlFile, File schemaFile) {
        try {
            File outputFile = new File(xmlFile.getParent(),
                    xmlFile.getName().replace(".xml", ".workflow_test.xml"));

            ApplicationOrchestrator.ProcessingOptions options =
                    new ApplicationOrchestrator.ProcessingOptions();
            options.setAnalyzeSchema(true);
            options.setApplyCorrections(true);
            options.setGenerateReport(true);
            options.setValidateAfterCorrection(true);

            ApplicationOrchestrator.ProcessingResult result =
                    orchestrator.processXmlComplete(xmlFile, schemaFile, outputFile, options).get();

            System.out.println("Complete Processing Results:");
            System.out.println("  Total time: " + result.getTotalProcessingTimeMs() + " ms");
            System.out.println("  Was successful: " + result.wasSuccessful());
            System.out.println("  Error reduction: " + result.getErrorReduction() + " errors");

            if (result.getInitialValidation() != null) {
                System.out.println("  Initial errors: " + result.getInitialValidation().getErrorCount());
            }

            if (result.getFinalValidation() != null) {
                System.out.println("  Final errors: " + result.getFinalValidation().getErrorCount());
            }

            if (result.getCorrectionResult() != null) {
                System.out.println("  Corrections applied: " +
                        result.getCorrectionResult().getAppliedActionCount());
            }

            // Clean up
            if (outputFile.exists()) {
                outputFile.delete();
            }

        } catch (Exception e) {
            System.err.println("Complete workflow test failed: " + e.getMessage());
        }
    }

    // Helper methods to create test files

    private static File createComprehensiveSchema() throws IOException {
        File schema = File.createTempFile("comprehensive_schema", ".xsd");

        String schemaContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "           targetNamespace=\"http://example.com/comprehensive\"\n" +
                "           xmlns:tns=\"http://example.com/comprehensive\"\n" +
                "           elementFormDefault=\"qualified\">\n" +
                "\n" +
                "    <xs:element name=\"company\" type=\"tns:companyType\"/>\n" +
                "\n" +
                "    <xs:complexType name=\"companyType\">\n" +
                "        <xs:sequence>\n" +
                "            <xs:element name=\"name\" type=\"xs:string\"/>\n" +
                "            <xs:element name=\"founded\" type=\"xs:int\"/>\n" +
                "            <xs:element name=\"employees\" type=\"tns:employeesType\"/>\n" +
                "            <xs:element name=\"departments\" type=\"tns:departmentsType\"/>\n" +
                "        </xs:sequence>\n" +
                "        <xs:attribute name=\"id\" type=\"xs:positiveInteger\" use=\"required\"/>\n" +
                "        <xs:attribute name=\"active\" type=\"xs:boolean\" default=\"true\"/>\n" +
                "    </xs:complexType>\n" +
                "\n" +
                "    <xs:complexType name=\"employeesType\">\n" +
                "        <xs:sequence>\n" +
                "            <xs:element name=\"employee\" type=\"tns:employeeType\" minOccurs=\"1\" maxOccurs=\"unbounded\"/>\n" +
                "        </xs:sequence>\n" +
                "    </xs:complexType>\n" +
                "\n" +
                "    <xs:complexType name=\"employeeType\">\n" +
                "        <xs:sequence>\n" +
                "            <xs:element name=\"firstName\" type=\"xs:string\"/>\n" +
                "            <xs:element name=\"lastName\" type=\"xs:string\"/>\n" +
                "            <xs:element name=\"age\" type=\"tns:ageType\"/>\n" +
                "            <xs:element name=\"email\" type=\"tns:emailType\"/>\n" +
                "            <xs:element name=\"salary\" type=\"tns:salaryType\" minOccurs=\"0\"/>\n" +
                "        </xs:sequence>\n" +
                "        <xs:attribute name=\"empId\" type=\"xs:positiveInteger\" use=\"required\"/>\n" +
                "    </xs:complexType>\n" +
                "\n" +
                "    <xs:complexType name=\"departmentsType\">\n" +
                "        <xs:sequence>\n" +
                "            <xs:element name=\"department\" type=\"tns:departmentType\" minOccurs=\"1\" maxOccurs=\"5\"/>\n" +
                "        </xs:sequence>\n" +
                "    </xs:complexType>\n" +
                "\n" +
                "    <xs:complexType name=\"departmentType\">\n" +
                "        <xs:sequence>\n" +
                "            <xs:element name=\"name\" type=\"tns:departmentNameType\"/>\n" +
                "            <xs:element name=\"budget\" type=\"tns:budgetType\"/>\n" +
                "        </xs:sequence>\n" +
                "        <xs:attribute name=\"deptId\" type=\"xs:positiveInteger\" use=\"required\"/>\n" +
                "    </xs:complexType>\n" +
                "\n" +
                "    <!-- Simple types with constraints -->\n" +
                "    <xs:simpleType name=\"ageType\">\n" +
                "        <xs:restriction base=\"xs:int\">\n" +
                "            <xs:minInclusive value=\"18\"/>\n" +
                "            <xs:maxInclusive value=\"65\"/>\n" +
                "        </xs:restriction>\n" +
                "    </xs:simpleType>\n" +
                "\n" +
                "    <xs:simpleType name=\"emailType\">\n" +
                "        <xs:restriction base=\"xs:string\">\n" +
                "            <xs:pattern value=\"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\"/>\n" +
                "        </xs:restriction>\n" +
                "    </xs:simpleType>\n" +
                "\n" +
                "    <xs:simpleType name=\"salaryType\">\n" +
                "        <xs:restriction base=\"xs:decimal\">\n" +
                "            <xs:minInclusive value=\"30000.00\"/>\n" +
                "            <xs:maxInclusive value=\"200000.00\"/>\n" +
                "            <xs:fractionDigits value=\"2\"/>\n" +
                "        </xs:restriction>\n" +
                "    </xs:simpleType>\n" +
                "\n" +
                "    <xs:simpleType name=\"departmentNameType\">\n" +
                "        <xs:restriction base=\"xs:string\">\n" +
                "            <xs:enumeration value=\"Engineering\"/>\n" +
                "            <xs:enumeration value=\"Marketing\"/>\n" +
                "            <xs:enumeration value=\"Sales\"/>\n" +
                "            <xs:enumeration value=\"HR\"/>\n" +
                "            <xs:enumeration value=\"Finance\"/>\n" +
                "        </xs:restriction>\n" +
                "    </xs:simpleType>\n" +
                "\n" +
                "    <xs:simpleType name=\"budgetType\">\n" +
                "        <xs:restriction base=\"xs:decimal\">\n" +
                "            <xs:minInclusive value=\"10000.00\"/>\n" +
                "            <xs:maxInclusive value=\"1000000.00\"/>\n" +
                "            <xs:fractionDigits value=\"2\"/>\n" +
                "        </xs:restriction>\n" +
                "    </xs:simpleType>\n" +
                "\n" +
                "</xs:schema>";

        try (FileWriter writer = new FileWriter(schema)) {
            writer.write(schemaContent);
        }

        return schema;
    }

    private static File createValidXml() throws IOException {
        File xml = File.createTempFile("valid_company", ".xml");

        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<company xmlns=\"http://example.com/comprehensive\" id=\"1\" active=\"true\">\n" +
                "    <name>Tech Innovations Inc.</name>\n" +
                "    <founded>2010</founded>\n" +
                "    <employees>\n" +
                "        <employee empId=\"101\">\n" +
                "            <firstName>John</firstName>\n" +
                "            <lastName>Doe</lastName>\n" +
                "            <age>30</age>\n" +
                "            <email>john.doe@example.com</email>\n" +
                "            <salary>75000.00</salary>\n" +
                "        </employee>\n" +
                "        <employee empId=\"102\">\n" +
                "            <firstName>Jane</firstName>\n" +
                "            <lastName>Smith</lastName>\n" +
                "            <age>28</age>\n" +
                "            <email>jane.smith@example.com</email>\n" +
                "            <salary>80000.00</salary>\n" +
                "        </employee>\n" +
                "    </employees>\n" +
                "    <departments>\n" +
                "        <department deptId=\"1\">\n" +
                "            <name>Engineering</name>\n" +
                "            <budget>500000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"2\">\n" +
                "            <name>Marketing</name>\n" +
                "            <budget>200000.00</budget>\n" +
                "        </department>\n" +
                "    </departments>\n" +
                "</company>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }

    private static File createInvalidXmlWithAllErrorTypes() throws IOException {
        File xml = File.createTempFile("invalid_company_comprehensive", ".xml");

        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<company xmlns=\"http://example.com/comprehensive\">\n" +
                "    <!-- Missing required 'id' attribute -->\n" +
                "    <!-- Wrong element order: founded should come after name -->\n" +
                "    <founded>not_a_number</founded>\n" +
                "    <!-- Invalid data type -->\n" +
                "    <name></name>\n" +
                "    <!-- Empty required content -->\n" +
                "    <departments>\n" +
                "        <!-- Missing required employees element -->\n" +
                "        <department>\n" +
                "            <!-- Missing required deptId attribute -->\n" +
                "            <name>InvalidDepartment</name>\n" +
                "            <!-- Invalid enumeration value -->\n" +
                "            <budget>-5000.00</budget>\n" +
                "            <!-- Invalid range (below minimum) -->\n" +
                "        </department>\n" +
                "        <department deptId=\"2\">\n" +
                "            <name>Engineering</name>\n" +
                "            <budget>100000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"3\">\n" +
                "            <name>Marketing</name>\n" +
                "            <budget>200000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"4\">\n" +
                "            <name>Sales</name>\n" +
                "            <budget>150000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"5\">\n" +
                "            <name>HR</name>\n" +
                "            <budget>120000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"6\">\n" +
                "            <!-- Too many departments (max 5) -->\n" +
                "            <name>Finance</name>\n" +
                "            <budget>180000.00</budget>\n" +
                "        </department>\n" +
                "    </departments>\n" +
                "</company>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }

    private static File createXmlWithMissingElements() throws IOException {
        File xml = File.createTempFile("missing_elements", ".xml");

        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<company xmlns=\"http://example.com/comprehensive\" id=\"1\">\n" +
                "    <name>Incomplete Company</name>\n" +
                "    <!-- Missing required 'founded' element -->\n" +
                "    <!-- Missing required 'employees' element -->\n" +
                "    <!-- Missing required 'departments' element -->\n" +
                "</company>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }

    private static File createXmlWithCardinalityErrors() throws IOException {
        File xml = File.createTempFile("cardinality_errors", ".xml");

        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<company xmlns=\"http://example.com/comprehensive\" id=\"1\">\n" +
                "    <name>Cardinality Test Company</name>\n" +
                "    <founded>2020</founded>\n" +
                "    <employees>\n" +
                "        <!-- Missing required employee (minOccurs=1) -->\n" +
                "    </employees>\n" +
                "    <departments>\n" +
                "        <department deptId=\"1\">\n" +
                "            <name>Engineering</name>\n" +
                "            <budget>100000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"2\">\n" +
                "            <name>Marketing</name>\n" +
                "            <budget>200000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"3\">\n" +
                "            <name>Sales</name>\n" +
                "            <budget>150000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"4\">\n" +
                "            <name>HR</name>\n" +
                "            <budget>120000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"5\">\n" +
                "            <name>Finance</name>\n" +
                "            <budget>180000.00</budget>\n" +
                "        </department>\n" +
                "        <department deptId=\"6\">\n" +
                "            <!-- Exceeds maxOccurs=5 -->\n" +
                "            <name>Operations</name>\n" +
                "            <budget>160000.00</budget>\n" +
                "        </department>\n" +
                "    </departments>\n" +
                "</company>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }

    private static File createXmlWithDataTypeErrors() throws IOException {
        File xml = File.createTempFile("data_type_errors", ".xml");

        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<company xmlns=\"http://example.com/comprehensive\" id=\"1\">\n" +
                "    <name>Data Type Test Company</name>\n" +
                "    <founded>not_a_year</founded>\n" +
                "    <!-- Invalid integer -->\n" +
                "    <employees>\n" +
                "        <employee empId=\"not_a_number\">\n" +
                "            <!-- Invalid positive integer -->\n" +
                "            <firstName>John</firstName>\n" +
                "            <lastName>Doe</lastName>\n" +
                "            <age>15</age>\n" +
                "            <!-- Below minimum age -->\n" +
                "            <email>invalid-email-format</email>\n" +
                "            <!-- Invalid email pattern -->\n" +
                "            <salary>25000.00</salary>\n" +
                "            <!-- Below minimum salary -->\n" +
                "        </employee>\n" +
                "        <employee empId=\"102\">\n" +
                "            <firstName>Jane</firstName>\n" +
                "            <lastName>Smith</lastName>\n" +
                "            <age>70</age>\n" +
                "            <!-- Above maximum age -->\n" +
                "            <email>jane.smith@example.com</email>\n" +
                "            <salary>250000.00</salary>\n" +
                "            <!-- Above maximum salary -->\n" +
                "        </employee>\n" +
                "    </employees>\n" +
                "    <departments>\n" +
                "        <department deptId=\"1\">\n" +
                "            <name>InvalidDepartment</name>\n" +
                "            <!-- Invalid enumeration -->\n" +
                "            <budget>not_a_number</budget>\n" +
                "            <!-- Invalid decimal -->\n" +
                "        </department>\n" +
                "    </departments>\n" +
                "</company>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }

    private static File createXmlWithOrderingErrors() throws IOException {
        File xml = File.createTempFile("ordering_errors", ".xml");

        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<company xmlns=\"http://example.com/comprehensive\" id=\"1\">\n" +
                "    <!-- Wrong order: should be name, founded, employees, departments -->\n" +
                "    <founded>2020</founded>\n" +
                "    <departments>\n" +
                "        <department deptId=\"1\">\n" +
                "            <!-- Wrong order: should be name, budget -->\n" +
                "            <budget>100000.00</budget>\n" +
                "            <name>Engineering</name>\n" +
                "        </department>\n" +
                "    </departments>\n" +
                "    <name>Ordering Test Company</name>\n" +
                "    <employees>\n" +
                "        <employee empId=\"101\">\n" +
                "            <!-- Wrong order: should be firstName, lastName, age, email, salary -->\n" +
                "            <age>30</age>\n" +
                "            <email>john.doe@example.com</email>\n" +
                "            <firstName>John</firstName>\n" +
                "            <salary>75000.00</salary>\n" +
                "            <lastName>Doe</lastName>\n" +
                "        </employee>\n" +
                "    </employees>\n" +
                "</company>";

        try (FileWriter writer = new FileWriter(xml)) {
            writer.write(xmlContent);
        }

        return xml;
    }
}
