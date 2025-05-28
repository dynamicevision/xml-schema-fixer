@echo off
echo XML Schema Fixer CLI
echo Usage: run-cli.bat command [options]
echo.

if "%~1"=="" (
    echo Available commands:
    echo   validate - Validate XML against schema
    echo   fix      - Fix XML to match schema
    echo   batch    - Process multiple files
    echo.
    echo Example: run-cli.bat validate --xml sample.xml --schema schema.xsd
    exit /b 0
)

mvn exec:java -Pcli -Dexec.args="%*"
