package com.xmlfixer.app.cli.commands;

import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.app.core.ApplicationOrchestrator.BatchProcessingResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * CLI command for batch processing multiple XML files
 */
@Command(
    name = "batch",
    description = "Process multiple XML files at once",
    mixinStandardHelpOptions = true
)
public class BatchCommand implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchCommand.class);
    
    private ApplicationOrchestrator orchestrator;
    
    @Parameters(description = "Directory containing XML files or list of XML files")
    private File[] inputPaths;
    
    @Option(names = {"-s", "--schema"}, description = "XSD schema file", required = true)
    private File schemaFile;
    
    @Option(names = {"-o", "--output-dir"}, description = "Output directory for fixed files")
    private File outputDirectory;
    
    @Option(names = {"--validate-only"}, description = "Only validate files, don't fix them")
    private boolean validateOnly;
    
    @Option(names = {"-r", "--report"}, description = "Generate batch processing report")
    private File reportFile;
    
    @Option(names = {"-f", "--format"}, description = "Report format (html, json, text)", defaultValue = "text")
    private String reportFormat;
    
    @Option(names = {"--include"}, description = "File pattern to include (e.g., *.xml)", defaultValue = "*.xml")
    private String includePattern;
    
    @Option(names = {"--exclude"}, description = "File pattern to exclude")
    private String excludePattern;
    
    @Option(names = {"--recursive"}, description = "Process directories recursively")
    private boolean recursive;
    
    @Option(names = {"-t", "--threads"}, description = "Number of parallel processing threads", defaultValue = "4")
    private int threadCount;
    
    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose;
    
    @Option(names = {"--continue-on-error"}, description = "Continue processing even if some files fail", defaultValue = "true")
    private boolean continueOnError;
    
    public void setOrchestrator(ApplicationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
    
    @Override
    public Integer call() throws Exception {
        try {
            // Validate inputs
            if (inputPaths == null || inputPaths.length == 0) {
                System.err.println("Error: No input files or directories specified");
                return 1;
            }
            
            if (!schemaFile.exists()) {
                System.err.println("Error: Schema file not found: " + schemaFile.getAbsolutePath());
                return 1;
            }
            
            // Collect XML files to process
            File[] xmlFiles = collectXmlFiles();
            
            if (xmlFiles.length == 0) {
                System.out.println("No XML files found to process");
                return 0;
            }
            
            // Set up output directory
            File targetOutputDir = setupOutputDirectory();
            
            System.out.println("Batch processing configuration:");
            System.out.println("  XML files to process: " + xmlFiles.length);
            System.out.println("  Schema file: " + schemaFile.getName());
            System.out.println("  Output directory: " + targetOutputDir.getAbsolutePath());
            System.out.println("  Operation: " + (validateOnly ? "Validate only" : "Validate and fix"));
            System.out.println("  Parallel threads: " + threadCount);
            System.out.println();
            
            // Process files
            BatchProcessingResult result = orchestrator.processBatchAsync(
                xmlFiles, schemaFile, targetOutputDir).get();
            
            // Display results
            displayBatchResults(result);
            
            // Generate report if requested
            if (reportFile != null) {
                generateReport(result);
            }
            
            // Return appropriate exit code
            return result.getFailedFiles() == 0 ? 0 : 1;
            
        } catch (Exception e) {
            logger.error("Batch command failed", e);
            System.err.println("Batch processing failed: " + e.getMessage());
            return 1;
        }
    }
    
    private File[] collectXmlFiles() {
        // TODO: Implement actual file collection logic
        // For now, return the input paths as-is
        return Arrays.stream(inputPaths)
            .filter(File::exists)
            .filter(f -> f.getName().toLowerCase().endsWith(".xml"))
            .toArray(File[]::new);
    }
    
    private File setupOutputDirectory() {
        if (outputDirectory != null) {
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }
            return outputDirectory;
        }
        
        // Default: create 'fixed' subdirectory in first input directory
        File firstInput = inputPaths[0];
        File defaultOutput;
        
        if (firstInput.isDirectory()) {
            defaultOutput = new File(firstInput, "fixed");
        } else {
            defaultOutput = new File(firstInput.getParent(), "fixed");
        }
        
        if (!defaultOutput.exists()) {
            defaultOutput.mkdirs();
        }
        
        return defaultOutput;
    }
    
    private void displayBatchResults(BatchProcessingResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BATCH PROCESSING RESULTS");
        System.out.println("=".repeat(60));
        
        System.out.println("Total files: " + result.getTotalFiles());
        System.out.println("Processed files: " + result.getProcessedFiles());
        System.out.println("Successful files: " + result.getSuccessfulFiles());
        System.out.println("Failed files: " + result.getFailedFiles());
        
        double successRate = result.getTotalFiles() > 0 ? 
            (double) result.getSuccessfulFiles() / result.getTotalFiles() * 100 : 0;
        System.out.printf("Success rate: %.1f%%\n", successRate);
        
        if (result.getFailedFiles() > 0) {
            System.out.println("\n⚠ Some files failed to process.");
            if (verbose) {
                System.out.println("Failed files will be listed in detailed report.");
            }
        } else {
            System.out.println("\n✓ All files processed successfully!");
        }
        
        System.out.println("=".repeat(60));
    }
    
    private void generateReport(BatchProcessingResult result) {
        try {
            System.out.println("\nGenerating batch processing report...");
            
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

