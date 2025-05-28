#!/bin/bash
echo "XML Schema Fixer CLI"
echo "Usage: ./run-cli.sh <command> [options]"
echo ""

if [ $# -eq 0 ]; then
    echo "Available commands:"
    echo "  validate - Validate XML against schema"
    echo "  fix      - Fix XML to match schema"
    echo "  batch    - Process multiple files"
    echo ""
    echo "Example: ./run-cli.sh validate --xml sample.xml --schema schema.xsd"
    exit 0
fi

mvn exec:java -Pcli -Dexec.args="$*"
