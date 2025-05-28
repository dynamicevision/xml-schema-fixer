package com.xmlfixer.reporting.config;

import com.xmlfixer.reporting.ReportGenerator;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for reporting-related dependencies
 */
@Module
public class ReportingModule {
    
    @Provides
    @Singleton
    public ReportGenerator provideReportGenerator() {
        return new ReportGenerator();
    }
}

