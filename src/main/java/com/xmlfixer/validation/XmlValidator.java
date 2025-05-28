package com.xmlfixer.validation;

import com.xmlfixer.parsing.XmlParser;
import com.xmlfixer.validation.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

/**
 * Service for validating XML files against XSD schemas
 */
@Singleton
public class XmlValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(XmlValidator.class);
    
    private final StreamingValidator streamingValidator;
    private final ErrorCollector errorCollector;
    private final XmlParser xmlParser;
    
    @Inject
    public XmlValidator(StreamingValidator streamingValidator, 
                       ErrorCollector errorCollector,
                       XmlParser xmlParser) {
        this.streamingValidator = streamingValidator;
        this.errorCollector = errorCollector;
        this.xmlParser = xmlParser;
        logger.info("XmlValidator initialized");
    }
    
    /**
     * Validates an XML file against a schema
     */
    public ValidationResult validate(File xmlFile, File schemaFile) {
        logger.info("Validating XML file: {} against schema: {}", 
            xmlFile.getName(), schemaFile.getName());
        
        long startTime = System.currentTimeMillis();
        ValidationResult result = new ValidationResult();
        result.setXmlFile(xmlFile);
        result.setSchemaFile(schemaFile);
        
        try {
            // TODO: Implement actual validation logic
            // For now, create a placeholder result
            result.setValid(false); // Assume invalid for testing
            
            long endTime = System.currentTimeMillis();
            result.setValidationTimeMs(endTime - startTime);
            
            logger.info("Validation completed for: {} ({}ms)", 
                xmlFile.getName(), result.getValidationTimeMs());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Validation failed for: {}", xmlFile.getName(), e);
            result.setValid(false);
            result.setValidationTimeMs(System.currentTimeMillis() - startTime);
            // TODO: Add error to result
            return result;
        }
    }
    
    /**
     * Quick validation check without detailed results
     */
    public boolean isValid(File xmlFile, File schemaFile) {
        try {
            ValidationResult result = validate(xmlFile, schemaFile);
            return result.isValid();
        } catch (Exception e) {
            logger.warn("Quick validation check failed for: {}", xmlFile.getName(), e);
            return false;
        }
    }
}

