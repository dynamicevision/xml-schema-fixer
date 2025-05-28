package com.xmlfixer.app.core.config;

import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.correction.CorrectionEngine;
import com.xmlfixer.parsing.XmlParser;
import com.xmlfixer.reporting.ReportGenerator;
import com.xmlfixer.schema.SchemaAnalyzer;
import com.xmlfixer.validation.XmlValidator;

import dagger.Module;
import dagger.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Core module providing application-wide dependencies
 */
@Module
public class CoreModule {
    
    private static final Logger logger = LoggerFactory.getLogger(CoreModule.class);
    
    @Provides
    @Singleton
    public Properties provideApplicationProperties() {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
                logger.info("Loaded application properties");
            } else {
                logger.warn("application.properties not found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load application properties", e);
        }
        return properties;
    }
    
    @Provides
    @Singleton
    public ApplicationOrchestrator provideApplicationOrchestrator(
            SchemaAnalyzer schemaAnalyzer,
            XmlValidator xmlValidator,
            CorrectionEngine correctionEngine,
            XmlParser xmlParser,
            ReportGenerator reportGenerator,
            Properties properties) {
        
        return new ApplicationOrchestrator(
            schemaAnalyzer,
            xmlValidator,
            correctionEngine,
            xmlParser,
            reportGenerator,
            properties
        );
    }
}

