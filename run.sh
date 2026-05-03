#!/bin/bash
# Run Script for SeqEdit - Biological Sequence Editor

cd "$(dirname "$0")"

# Set Java classpath (include BioJava libs)
CP="classes:lib/*"

# Run the application
java -cp "$CP" SeqEdit "$@"
