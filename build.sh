#!/bin/bash

# Build script for Online Voting System
# Compiles all Java files and creates output directory

echo "Building Online Voting System..."

# Create output directory
mkdir -p out

# Find JavaFX path (adjust if needed)
JAVAFX_PATH=""
if [ -d "/usr/lib/jvm/openjfx" ]; then
    JAVAFX_PATH="/usr/lib/jvm/openjfx/lib"
elif [ -d "/Library/Java/JavaVirtualMachines" ]; then
    # macOS - JavaFX might be in JDK
    JAVAFX_PATH=""
else
    echo "Note: JavaFX path not found. If you have JavaFX installed, update JAVAFX_PATH in this script."
fi

# Compile with JavaFX modules if path is set
if [ -n "$JAVAFX_PATH" ]; then
    javac -d out \
        --module-path "$JAVAFX_PATH" \
        --add-modules javafx.controls,javafx.fxml \
        -cp "src/main/resources" \
        src/main/java/com/votingsystem/**/*.java
else
    # Try compiling without explicit JavaFX path (if JavaFX is in JDK)
    javac -d out \
        --add-modules javafx.controls,javafx.fxml \
        -cp "src/main/resources" \
        src/main/java/com/votingsystem/**/*.java 2>/dev/null || \
    javac -d out \
        -cp "src/main/resources" \
        src/main/java/com/votingsystem/**/*.java
fi

if [ $? -eq 0 ]; then
    echo "Build successful! Output in 'out' directory."
    echo ""
    echo "To run the application:"
    if [ -n "$JAVAFX_PATH" ]; then
        echo "  java --module-path \"$JAVAFX_PATH\" --add-modules javafx.controls,javafx.fxml -cp out:src/main/resources com.votingsystem.Main"
    else
        echo "  java --add-modules javafx.controls,javafx.fxml -cp out:src/main/resources com.votingsystem.Main"
        echo "  OR"
        echo "  java -cp out:src/main/resources com.votingsystem.Main"
    fi
else
    echo "Build failed. Please check errors above."
    exit 1
fi
