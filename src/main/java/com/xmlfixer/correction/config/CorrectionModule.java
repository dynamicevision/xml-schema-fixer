package com.xmlfixer.correction.config;

import com.xmlfixer.correction.CorrectionEngine;
import com.xmlfixer.correction.DomManipulator;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for correction-related dependencies
 */
@Module
public class CorrectionModule {
    
    @Provides
    @Singleton
    public DomManipulator provideDomManipulator() {
        return new DomManipulator();
    }
    
    @Provides
    @Singleton
    public CorrectionEngine provideCorrectionEngine(DomManipulator domManipulator) {
        return new CorrectionEngine(domManipulator);
    }
}

