package com.xmlfixer.app.core.config;

import com.xmlfixer.app.cli.config.CliModule;
import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.app.gui.config.GuiModule;
import com.xmlfixer.common.config.CommonModule;
import com.xmlfixer.correction.config.CorrectionModule;
import com.xmlfixer.parsing.config.ParsingModule;
import com.xmlfixer.reporting.config.ReportingModule;
import com.xmlfixer.schema.config.SchemaModule;
import com.xmlfixer.validation.config.ValidationModule;

import dagger.Component;
import javax.inject.Singleton;

/**
 * Main Dagger component that wires together all application modules.
 * This component provides dependency injection for both GUI and CLI applications.
 */
@Singleton
@Component(modules = {
    CoreModule.class,
    CommonModule.class,
    SchemaModule.class,
    ValidationModule.class,
    CorrectionModule.class,
    ParsingModule.class,
    ReportingModule.class,
    GuiModule.class,
    CliModule.class
})
public interface ApplicationComponent {
    
    /**
     * Provides the main application orchestrator
     */
    ApplicationOrchestrator orchestrator();
    
    /**
     * CLI specific injection point
     */
    void inject(com.xmlfixer.app.cli.CliApplication cliApplication);
    
    /**
     * GUI specific injection point  
     */
    void inject(com.xmlfixer.app.gui.GuiApplication guiApplication);
    
    /**
     * Factory for creating the component
     */
    @Component.Factory
    interface Factory {
        ApplicationComponent create();
    }
}

