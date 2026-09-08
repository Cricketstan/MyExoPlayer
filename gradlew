#!/bin/bash

# Get the directory where this script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Use the jar file in the same directory
exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
