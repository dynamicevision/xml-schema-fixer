#!/bin/bash
echo "Packaging XML Schema Fixer..."
mvn clean package

if [ $? -eq 0 ]; then
    echo "✓ Packaging successful"
    echo "CLI JAR: target/xml-fixer-cli.jar"
else
    echo "✗ Packaging failed"
    exit 1
fi
