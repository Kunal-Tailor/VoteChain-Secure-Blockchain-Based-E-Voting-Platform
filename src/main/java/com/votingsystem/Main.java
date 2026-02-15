package com.votingsystem;

import com.votingsystem.ui.HomeScreen;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main entry point for the Online Voting System
 * Blockchain-based secure voting application
 */
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setTitle("🇮🇳 Online Voting System - Blockchain Based");
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            
            // Center window on screen
            primaryStage.centerOnScreen();
            
            // Start with home screen
            HomeScreen homeScreen = new HomeScreen(primaryStage);
            primaryStage.setScene(homeScreen.getScene());
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
