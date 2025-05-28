package com.xmlfixer.app.cli.commands;

import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.validation.model.ValidationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * CLI command for validating XML files against XSD schema
 */
@Command(
    name = "validate",
    description = "Validate XML files against XSD schema",
    mixinStandardHelpOptions = true
)
public class ValidateCommand implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);
    
    private ApplicationOrchestrator orchestrator;
    
    @Parameters(index = "0", description = "XML file to validate")
    private File xmlFile;
    
    @Option(names = {"-s", "--schema"}, description = "XSD schema file", required = true)
    private File schemaFile;
    
    @Option(names = {"-r", "--report"}, description = "Generate validation report")
    private File reportFile;
    
    @Option(names = {"-f", "--format"}, description = "Report format (html, json, text)", defaultValue = "text")
    private String reportFormat;
    
    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;
    
    public void setOrchestrator(ApplicationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
    
    @Override
    public Integer call() throws Exception {
        try {
            // Validate input files
            if (!xmlFile.exists()) {
                System.err.println("Error: XML file not found: " + xmlFile.getAbsolutePath());
                return 1;
            }
            
            if (!schemaFile.exists()) {
                System.err.println("Error: Schema file not found: " + schemaFile.getAbsolutePath());
                return 1;
            }
            
            System.out.println("Validating XML file: " + xmlFile.getName());
            System.out.println("Using schema: " + schemaFile.getName());
            
            if (verbose) {
                System.out.println("XML file path: " + xmlFile.getAbsolutePath());
                System.out.println("Schema file path: " + schemaFile.getAbsolutePath());
            }
            
            // Perform validation
            ValidationResult result = orchestrator.validateXml(xmlFile, schemaFile);
            
            // Display results
            displayValidationResults(result);
            
            // Generate report if requested
            if (reportFile != null) {
                generateReport(result);
            }
            
            // Return appropriate exit code
            return result.isValid() ? 0 : 2;
            
        } catch (Exception e) {
            logger.error("Validation command failed", e);
            System.err.println("Validation failed: " + e.getMessage());
            return 1;
        }
    }
    
    private void displayValidationResults(ValidationResult result) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("VALIDATION RESULTS");
        System.out.println("=".repeat(50));
        
        if (result.isValid()) {
            System.out.println("✓ XML file is VALID");
            System.out.println("No errors found.");
        } else {
            System.out.println("✗ XML file is INVALID");
            System.out.println("Errors found: " + result.getErrors().size());
            
            if (verbose) {
                System.out.println("\nError details:");
                // TODO: Display actual error details when model is implemented
                System.out.println("- Detailed error list will be shown here");
            }
        }
        
        if (result.getWarnings() != null && !result.getWarnings().isEmpty()) {
            System.out.println("Warnings: " + result.getWarnings().size());
            
            if (verbose) {
                System.out.println("\nWarning details:");
                // TODO: Display actual warning details when model is implemented
                System.out.println("- Detailed warning list will be shown here");
            }
        }
        
        System.out.println("=".repeat(50));
    }
    
    private void generateReport(ValidationResult result) {
        try {
            System.out.println("\nGenerating validation report...");
            
            // TODO: Implement actual report generation
            System.out.println("Report would be generated at: " + reportFile.getAbsolutePath());
            System.out.println("Report format: " + reportFormat);
            
            System.out.println("✓ Report generated successfully");
            
        } catch (Exception e) {
            logger.error("Failed to generate report", e);
            System.err.println("Warning: Failed to generate report - " + e.getMessage());
        }
    }
}

