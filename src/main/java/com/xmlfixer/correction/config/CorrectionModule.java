package com.xmlfixer.correction.config;

import com.xmlfixer.correction.CorrectionEngine;
import com.xmlfixer.correction.DomManipulator;
import com.xmlfixer.correction.strategies.*;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Enhanced Dagger module for correction-related dependencies
 * Provides all correction strategies and supporting components
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

    @Provides
    @Singleton
    public CorrectionPlanner provideCorrectionPlanner() {
        return new CorrectionPlanner();
    }

    @Provides
    @Singleton
    public MissingElementStrategy provideMissingElementStrategy(DomManipulator domManipulator) {
        return new MissingElementStrategy(domManipulator);
    }

    @Provides
    @Singleton
    public OrderingStrategy provideElementOrderingStrategy(DomManipulator domManipulator) {
        return new OrderingStrategy(domManipulator);
    }

    @Provides
    @Singleton
    public CardinalityStrategy provideCardinalityStrategy(DomManipulator domManipulator) {
        return new CardinalityStrategy(domManipulator);
    }

    @Provides
    @Singleton
    public DataTypeStrategy provideDataTypeStrategy(DomManipulator domManipulator) {
        return new DataTypeStrategy(domManipulator);
    }

    @Provides
    @Singleton
    public AttributeStrategy provideAttributeStrategy(DomManipulator domManipulator) {
        return new AttributeStrategy(domManipulator);
    }

    @Provides
    @Singleton
    public ContentStrategy provideContentStrategy(DomManipulator domManipulator) {
        return new ContentStrategy(domManipulator);
    }
}
