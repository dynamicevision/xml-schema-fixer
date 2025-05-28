package com.xmlfixer.schema.config;

import com.xmlfixer.schema.SchemaAnalyzer;
import com.xmlfixer.schema.SchemaConstraintExtractor;
import com.xmlfixer.schema.SchemaParser;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for schema-related dependencies
 */
@Module
public class SchemaModule {

    @Provides
    @Singleton
    public SchemaParser provideSchemaParser() {
        return new SchemaParser();
    }

    @Provides
    @Singleton
    public SchemaConstraintExtractor provideSchemaConstraintExtractor() {
        return new SchemaConstraintExtractor();
    }

    @Provides
    @Singleton
    public SchemaAnalyzer provideSchemaAnalyzer(SchemaParser schemaParser) {
        return new SchemaAnalyzer(schemaParser);
    }
}
