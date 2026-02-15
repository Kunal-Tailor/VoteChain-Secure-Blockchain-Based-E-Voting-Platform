package com.votingsystem.ui;

import com.votingsystem.service.BlockchainService;
import com.votingsystem.service.VoterService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Voter login screen
 * Authenticates voters before allowing them to vote
 */
public class VoterLoginScreen {
    
    private Stage primaryStage;
    private Scene scene;
    private VoterService voterService;
    
    public VoterLoginScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        BlockchainService blockchainService = new BlockchainService();
        this.voterService = new VoterService(blockchainService);
        createUI();
    }
    
    private void createUI() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.getStyleClass().add("login-root");
        
        // Indian-themed icon
        Label iconLabel = new Label("🇮🇳");
        iconLabel.getStyleClass().add("icon-label");
        
        Label titleLabel = new Label("Voter Login");
        titleLabel.getStyleClass().add("title");
        
        // Card container for form
        VBox cardContainer = new VBox(20);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(40, 50, 40, 50));
        cardContainer.getStyleClass().add("card-container");
        
        Label voterIdLabel = new Label("🆔 Voter ID");
        voterIdLabel.getStyleClass().add("subtitle");
        TextField voterIdField = new TextField();
        voterIdField.setPromptText("Enter your Voter ID (e.g., V001)");
        voterIdField.setPrefWidth(350);
        voterIdField.setPrefHeight(45);
        
        Label passwordLabel = new Label("🔒 Password");
        passwordLabel.getStyleClass().add("subtitle");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefWidth(350);
        passwordField.setPrefHeight(45);
        
        Button loginButton = new Button("➡️ Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setPrefWidth(350);
        loginButton.setPrefHeight(50);
        loginButton.setOnAction(e -> {
            String voterId = voterIdField.getText().trim();
            String password = passwordField.getText();
            
            if (voterId.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", 
                    "Please enter both Voter ID and Password");
                return;
            }
            
            if (voterService.authenticateVoter(voterId, password)) {
                if (voterService.hasVoted(voterId)) {
                    showAlert(Alert.AlertType.INFORMATION, "Already Voted", 
                        "You have already cast your vote. Redirecting to results...");
                    VoterResultsScreen resultsScreen = new VoterResultsScreen(primaryStage, voterService);
                    primaryStage.setScene(resultsScreen.getScene());
                } else {
                    VoterDashboardScreen dashboard = new VoterDashboardScreen(primaryStage, voterService, voterId);
                    primaryStage.setScene(dashboard.getScene());
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Authentication Failed", 
                    "Invalid Voter ID or Password. Please try again.");
            }
        });
        
        Button backButton = new Button("← Back to Home");
        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(350);
        backButton.setPrefHeight(45);
        backButton.setOnAction(e -> {
            HomeScreen home = new HomeScreen(primaryStage);
            primaryStage.setScene(home.getScene());
        });
        
        cardContainer.getChildren().addAll(
            voterIdLabel, voterIdField, 
            passwordLabel, passwordField, loginButton, backButton
        );
        
        root.getChildren().addAll(iconLabel, titleLabel, cardContainer);
        
        scene = new Scene(root, 700, 650);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public Scene getScene() {
        return scene;
    }
}
