#!/bin/bash

# Run script for Online Voting System

echo "Starting Online Voting System..."

# Load environment variables from .env if present
if [ -f ".env" ]; then
    set -a
    source .env
    set +a
fi

# Find JavaFX path (adjust if needed)
JAVAFX_PATH=""
if [ -d "/usr/lib/jvm/openjfx" ]; then
    JAVAFX_PATH="/usr/lib/jvm/openjfx/lib"
fi

# Run the application
if [ -n "$JAVAFX_PATH" ]; then
    java --module-path "$JAVAFX_PATH" \
        --add-modules javafx.controls,javafx.fxml \
        -cp out:src/main/resources \
        com.votingsystem.Main
else
    # Try running without explicit JavaFX path
    java --add-modules javafx.controls,javafx.fxml \
        -cp out:src/main/resources \
        com.votingsystem.Main 2>/dev/null || \
    java -cp out:src/main/resources \
        com.votingsystem.Main
fi
