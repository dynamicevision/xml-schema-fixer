@echo off
echo Building XML Schema Fixer...
mvn clean compile

if %errorlevel% equ 0 (
    echo ✓ Build successful
) else (
    echo ✗ Build failed
    exit /b 1
)
