#!/bin/bash
kotlinc -nowarn -d /dev/null -cp $(find app/src/main/java -name "*.kt" | xargs echo) || echo "Syntax error found"
