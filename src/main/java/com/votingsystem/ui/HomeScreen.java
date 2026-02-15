package com.votingsystem.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Home screen - Entry point of the application
 * Allows user to choose between Voter and Admin login
 */
public class HomeScreen {
    
    private Stage primaryStage;
    private Scene scene;
    
    public HomeScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        createUI();
    }
    
    private void createUI() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.getStyleClass().add("home-root");
        
        // Indian-themed icon
        Label iconLabel = new Label("🇮🇳");
        iconLabel.getStyleClass().add("icon-label");
        
        Label titleLabel = new Label("Online Voting System");
        titleLabel.getStyleClass().add("title");
        
        Label subtitleLabel = new Label("Blockchain-Based Secure Voting Platform");
        subtitleLabel.getStyleClass().add("subtitle");
        
        VBox buttonContainer = new VBox(15);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(20, 0, 0, 0));
        
        Button voterButton = new Button("👤 Voter Login");
        voterButton.getStyleClass().add("primary-button");
        voterButton.setPrefWidth(280);
        voterButton.setPrefHeight(55);
        voterButton.setOnAction(e -> {
            VoterLoginScreen voterLogin = new VoterLoginScreen(primaryStage);
            primaryStage.setScene(voterLogin.getScene());
        });
        
        Button adminButton = new Button("🔐 Admin Login");
        adminButton.getStyleClass().add("secondary-button");
        adminButton.setPrefWidth(280);
        adminButton.setPrefHeight(55);
        adminButton.setOnAction(e -> {
            AdminLoginScreen adminLogin = new AdminLoginScreen(primaryStage);
            primaryStage.setScene(adminLogin.getScene());
        });
        
        buttonContainer.getChildren().addAll(voterButton, adminButton);
        root.getChildren().addAll(iconLabel, titleLabel, subtitleLabel, buttonContainer);
        
        scene = new Scene(root, 700, 550);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
    
    public Scene getScene() {
        return scene;
    }
}
