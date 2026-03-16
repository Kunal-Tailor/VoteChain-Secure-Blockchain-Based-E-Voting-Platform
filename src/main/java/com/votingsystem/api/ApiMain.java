package com.votingsystem.api;

import com.votingsystem.ui.HomeScreen;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * ApiMain — starts BOTH the REST API server AND the JavaFX desktop app.
 *
 * Run this instead of Main.java when you want:
 *   ✅ JavaFX desktop window  (same as before)
 *   ✅ REST API on port 4567  (so index.html website can connect)
 *
 * They share the same blockchain.dat and voters.dat files.
 *
 * NOTE: If you only want the web interface (no JavaFX window),
 *       just call VotingApiServer.startServer() in a plain main() instead.
 */
public class ApiMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Start JavaFX UI (same as Main.java)
        primaryStage.setTitle("🇮🇳 Online Voting System - Blockchain Based");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        primaryStage.centerOnScreen();

        HomeScreen homeScreen = new HomeScreen(primaryStage);
        primaryStage.setScene(homeScreen.getScene());
        primaryStage.show();
    }

    public static void main(String[] args) {
        // ── Step 1: Start REST API server on a background thread ──────────
        Thread apiThread = new Thread(() -> {
            try {
                VotingApiServer.startServer();
            } catch (Exception e) {
                System.err.println("API Server error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        apiThread.setDaemon(true); // dies when JavaFX window closes
        apiThread.start();

        // ── Step 2: Start JavaFX desktop app ──────────────────────────────
        launch(args);
    }
}
