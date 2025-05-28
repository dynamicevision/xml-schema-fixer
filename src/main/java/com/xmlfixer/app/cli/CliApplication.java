package com.xmlfixer.app.cli;

import com.xmlfixer.app.cli.commands.BatchCommand;
import com.xmlfixer.app.cli.commands.FixCommand;
import com.xmlfixer.app.cli.commands.ValidateCommand;
import com.xmlfixer.app.core.ApplicationOrchestrator;
import com.xmlfixer.app.core.config.ApplicationComponent;
import com.xmlfixer.app.core.config.DaggerApplicationComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.inject.Inject;

/**
 * Main CLI application entry point using PicoCLI for command line parsing
 */
@Command(
    name = "xml-fixer",
    description = "XML Schema Fixer - Automatic XML validation and correction tool",
    version = "1.0.0",
    mixinStandardHelpOptions = true,
    subcommands = {
        ValidateCommand.class,
        FixCommand.class,
        BatchCommand.class
    }
)
public class CliApplication implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(CliApplication.class);
    
    @Inject
    ApplicationOrchestrator orchestrator;
    
    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;
    
    @Option(names = {"-q", "--quiet"}, description = "Suppress output except errors")
    private boolean quiet;
    
    public static void main(String[] args) {
        try {
            // Initialize DI container
            ApplicationComponent component = DaggerApplicationComponent.factory().create();
            
            // Create CLI application instance
            CliApplication app = new CliApplication();
            component.inject(app);
            
            // Configure PicoCLI command line
            CommandLine commandLine = new CommandLine(app);
            
            // Inject dependencies into subcommands
            commandLine.getSubcommands().values().forEach(subCmd -> {
                Object command = subCmd.getCommand();
                if (command instanceof ValidateCommand) {
                    ((ValidateCommand) command).setOrchestrator(app.orchestrator);
                } else if (command instanceof FixCommand) {
                    ((FixCommand) command).setOrchestrator(app.orchestrator);
                } else if (command instanceof BatchCommand) {
                    ((BatchCommand) command).setOrchestrator(app.orchestrator);
                }
            });
            
            // Execute command
            int exitCode = commandLine.execute(args);
            System.exit(exitCode);
            
        } catch (Exception e) {
            logger.error("Application failed to start", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    @Override
    public void run() {
        // Default behavior when no subcommand is specified
        System.out.println("XML Schema Fixer - Use --help to see available commands");
        
        System.out.println("\nAvailable commands:");
        System.out.println("  validate  - Validate XML files against schema");
        System.out.println("  fix       - Fix XML files to match schema");
        System.out.println("  batch     - Process multiple files at once");
        System.out.println("\nUse 'xml-fixer <command> --help' for more information about a command.");
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public boolean isQuiet() {
        return quiet;
    }
}

