# XML Schema Fixer

A comprehensive desktop application for automatically validating and correcting XML files against XSD schemas. Built with JavaFX for GUI interface and supporting CLI operations.

## Features

- **XML Validation**: Validate XML files against XSD schemas with detailed error reporting
- **Automatic Correction**: Intelligently fix XML files to match schema requirements
- **Batch Processing**: Process multiple XML files simultaneously
- **Dual Interface**: Both GUI (JavaFX) and CLI interfaces available
- **Detailed Reporting**: Generate comprehensive reports in multiple formats
- **Memory Efficient**: Streaming processing for large XML files
- **Modular Architecture**: Clean separation of concerns with dependency injection

## Technology Stack

- **Java 11+**: Core programming language
- **JavaFX 17**: GUI framework
- **Maven**: Build tool and dependency management
- **Dagger 2**: Dependency injection framework
- **PicoCLI**: Command-line interface framework
- **SLF4J + Logback**: Logging framework

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- JavaFX runtime (included in dependencies)

### Build the Project

```bash
# Clone the repository
git clone <repository-url>
cd xml-schema-fixer

# Build the project
mvn clean compile

# Or use the build script
./scripts/build.sh
```

### Run the GUI Application

```bash
# Using Maven
mvn javafx:run

# Or using the script
./scripts/run-gui.sh
```

### Run the CLI Application

```bash
# Validate an XML file
mvn exec:java -Pcli -Dexec.args="validate --xml sample.xml --schema schema.xsd"

# Fix an XML file
mvn exec:java -Pcli -Dexec.args="fix --xml invalid.xml --schema schema.xsd --output fixed.xml"

# Batch process files
mvn exec:java -Pcli -Dexec.args="batch /path/to/xml/files --schema schema.xsd --output-dir /path/to/output"

# Or use the script
./scripts/run-cli.sh validate --xml sample.xml --schema schema.xsd
```

### Create Standalone JAR

```bash
# Package the application
mvn clean package

# Run the standalone CLI
java -jar target/xml-fixer-cli.jar validate --xml sample.xml --schema schema.xsd
```

## Project Structure

```
xml-schema-fixer/
├── src/main/java/com/xmlfixer/
│   ├── app/                    # Application entry points
│   │   ├── cli/               # CLI interface
│   │   ├── gui/               # JavaFX GUI interface
│   │   └── core/              # Core application logic
│   ├── schema/                # Schema analysis components
│   ├── validation/            # XML validation engine
│   ├── correction/            # XML correction/fixing engine
│   ├── parsing/               # XML parsing utilities
│   ├── reporting/             # Report generation
│   └── common/                # Common utilities and exceptions
├── src/main/resources/
│   ├── fxml/                  # JavaFX FXML files
│   ├── logback.xml           # Logging configuration
│   └── application.properties # Application settings
└── scripts/                   # Build and run scripts
```

## Architecture Overview

The application follows a modular architecture with clean separation of concerns:

### Core Components

1. **Application Orchestrator**: Coordinates all operations and provides high-level API
2. **Schema Analyzer**: Parses XSD schemas and extracts validation rules
3. **XML Validator**: Validates XML files against schemas with detailed error reporting
4. **Correction Engine**: Automatically fixes XML files to match schema requirements
5. **Report Generator**: Creates comprehensive reports of validation and correction results

### Dependency Injection

Uses Dagger 2 for compile-time dependency injection, ensuring:
- Loose coupling between components
- Easy testing and mocking
- Clear dependency graphs
- Performance optimization

### Interface Design

- **GUI**: JavaFX-based desktop interface with intuitive file selection and progress tracking
- **CLI**: PicoCLI-based command-line interface for automation and scripting

## Usage Examples

### GUI Interface

1. Launch the GUI application
2. Select XML file and XSD schema
3. Choose validation or correction operation
4. View results and generate reports

### CLI Interface

#### Validate XML File
```bash
xml-fixer validate \
  --xml document.xml \
  --schema schema.xsd \
  --report validation-report.html \
  --verbose
```

#### Fix XML File
```bash
xml-fixer fix \
  --xml invalid.xml \
  --schema schema.xsd \
  --output corrected.xml \
  --backup \
  --report correction-report.html
```

#### Batch Processing
```bash
xml-fixer batch \
  /path/to/xml/directory \
  --schema schema.xsd \
  --output-dir /path/to/corrected \
  --recursive \
  --threads 4 \
  --report batch-report.html
```

## Configuration

### Application Properties

Configure the application through `src/main/resources/application.properties`:

```properties
# Processing limits
xml.max.file.size.mb=500
xml.buffer.size.kb=64
validation.concurrent.threads=4

# Correction settings
correction.backup.enabled=true
correction.max.attempts=3

# GUI settings
gui.window.width=1200
gui.window.height=800
```

### Logging Configuration

Adjust logging levels in `src/main/resources/logback.xml`:

```xml
<!-- Set log level for application -->
<logger name="com.xmlfixer" level="DEBUG" />
```

## Development

### Running Tests

```bash
mvn test
```

### Code Style

The project follows standard Java conventions with:
- Clear naming conventions
- Comprehensive documentation
- Modular design patterns
- Dependency injection best practices

### Adding New Features

1. Create service classes in appropriate packages
2. Add Dagger modules for dependency injection
3. Update the ApplicationOrchestrator for coordination
4. Add corresponding CLI commands or GUI controls
5. Write comprehensive tests

## Current Status

This is the initial framework with:
- ✅ Complete project structure
- ✅ Dependency injection setup
- ✅ GUI and CLI interfaces
- ✅ Basic service architecture
- ⏳ Core business logic (validation/correction algorithms) - **Next Phase**

## Next Steps

1. Implement core validation algorithms
2. Add schema parsing and analysis logic
3. Develop XML correction strategies
4. Create comprehensive test suite
5. Add advanced reporting features
6. Performance optimization for large files

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes following the established architecture
4. Add tests for new functionality
5. Submit a pull request

## License


