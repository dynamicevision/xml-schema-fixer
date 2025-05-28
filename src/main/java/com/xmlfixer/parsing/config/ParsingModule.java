package com.xmlfixer.parsing.config;

import com.xmlfixer.parsing.XmlParser;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for parsing-related dependencies
 */
@Module
public class ParsingModule {
    
    @Provides
    @Singleton
    public XmlParser provideXmlParser() {
        return new XmlParser();
    }
}

