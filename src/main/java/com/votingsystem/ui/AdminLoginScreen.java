package com.votingsystem.ui;

import com.votingsystem.service.AdminService;
import com.votingsystem.service.BlockchainService;
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
 * Admin login screen
 * Authenticates admin users
 */
public class AdminLoginScreen {
    
    private Stage primaryStage;
    private Scene scene;
    private AdminService adminService;
    
    public AdminLoginScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        BlockchainService blockchainService = new BlockchainService();
        this.adminService = new AdminService(blockchainService);
        createUI();
    }
    
    private void createUI() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.getStyleClass().add("login-root");
        
        Label iconLabel = new Label("🇮🇳");
        iconLabel.getStyleClass().add("icon-label");
        
        Label titleLabel = new Label("Admin Login");
        titleLabel.getStyleClass().add("title");
        
        VBox cardContainer = new VBox(20);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(40, 50, 40, 50));
        cardContainer.getStyleClass().add("card-container");
        
        Label usernameLabel = new Label("👤 Username");
        usernameLabel.getStyleClass().add("subtitle");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter admin username");
        usernameField.setPrefWidth(350);
        usernameField.setPrefHeight(45);
        
        Label passwordLabel = new Label("🔒 Password");
        passwordLabel.getStyleClass().add("subtitle");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");
        passwordField.setPrefWidth(350);
        passwordField.setPrefHeight(45);
        
        Button loginButton = new Button("➡️ Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setPrefWidth(350);
        loginButton.setPrefHeight(50);
        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", 
                    "Please enter both Username and Password");
                return;
            }
            
            if (adminService.authenticateAdmin(username, password)) {
                AdminDashboardScreen dashboard = new AdminDashboardScreen(primaryStage, adminService);
                primaryStage.setScene(dashboard.getScene());
            } else {
                showAlert(Alert.AlertType.ERROR, "Authentication Failed", 
                    "Invalid Username or Password. Please try again.");
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
            usernameLabel, usernameField, 
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
