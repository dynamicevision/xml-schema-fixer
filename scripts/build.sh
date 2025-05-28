#!/bin/bash
echo "Building XML Schema Fixer..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "✓ Build successful"
else
    echo "✗ Build failed"
    exit 1
fi

