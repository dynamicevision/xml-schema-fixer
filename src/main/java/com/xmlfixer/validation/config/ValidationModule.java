package com.xmlfixer.validation.config;

import com.xmlfixer.parsing.XmlParser;
import com.xmlfixer.schema.SchemaAnalyzer;
import com.xmlfixer.validation.ErrorCollector;
import com.xmlfixer.validation.StreamingValidator;
import com.xmlfixer.validation.XmlValidator;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for validation-related dependencies
 */
@Module
public class ValidationModule {

    @Provides
    @Singleton
    public StreamingValidator provideStreamingValidator(ErrorCollector errorCollector) {
        return new StreamingValidator(errorCollector);
    }

    @Provides
    @Singleton
    public ErrorCollector provideErrorCollector() {
        return new ErrorCollector();
    }

    @Provides
    @Singleton
    public XmlValidator provideXmlValidator(StreamingValidator streamingValidator,
                                            ErrorCollector errorCollector,
                                            XmlParser xmlParser,
                                            SchemaAnalyzer schemaAnalyzer) {
        return new XmlValidator(streamingValidator, errorCollector, xmlParser, schemaAnalyzer);
    }
}
