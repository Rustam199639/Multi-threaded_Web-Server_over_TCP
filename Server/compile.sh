#!/bin/bash
# Compiles Java files to the /bin directory

# Create the bin directory if it doesn't exist
mkdir -p ./bin

# Compile the java files from the src directory
javac -d ./bin ./src/*.java
