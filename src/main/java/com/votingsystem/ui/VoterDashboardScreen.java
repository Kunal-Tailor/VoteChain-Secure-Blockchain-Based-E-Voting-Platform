package com.votingsystem.ui;

import com.votingsystem.service.VoterService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Voter dashboard screen
 * Main screen for voters after login
 */
public class VoterDashboardScreen {
    
    private Stage primaryStage;
    private Scene scene;
    private VoterService voterService;
    private String voterId;
    
    public VoterDashboardScreen(Stage primaryStage, VoterService voterService, String voterId) {
        this.primaryStage = primaryStage;
        this.voterService = voterService;
        this.voterId = voterId;
        createUI();
    }
    
    private void createUI() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.getStyleClass().add("dashboard-root");
        
        Label iconLabel = new Label("🇮🇳");
        iconLabel.getStyleClass().add("icon-label");
        
        Label welcomeLabel = new Label("Welcome, " + voterId);
        welcomeLabel.getStyleClass().add("title");
        
        Label instructionLabel = new Label("Please cast your vote by selecting a candidate");
        instructionLabel.getStyleClass().add("subtitle");
        
        VBox cardContainer = new VBox(20);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(40, 50, 40, 50));
        cardContainer.getStyleClass().add("card-container");
        
        Button castVoteButton = new Button("🗳️ Cast Vote");
        castVoteButton.getStyleClass().add("primary-button");
        castVoteButton.setPrefWidth(320);
        castVoteButton.setPrefHeight(60);
        castVoteButton.setOnAction(e -> {
            VoteCastingScreen voteScreen = new VoteCastingScreen(primaryStage, voterService, voterId);
            primaryStage.setScene(voteScreen.getScene());
        });
        
        Button viewResultsButton = new Button("📊 View Results");
        viewResultsButton.getStyleClass().add("secondary-button");
        viewResultsButton.setPrefWidth(320);
        viewResultsButton.setPrefHeight(60);
        viewResultsButton.setOnAction(e -> {
            VoterResultsScreen resultsScreen = new VoterResultsScreen(primaryStage, voterService);
            primaryStage.setScene(resultsScreen.getScene());
        });
        
        Button logoutButton = new Button("🚪 Logout");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setPrefWidth(320);
        logoutButton.setPrefHeight(50);
        logoutButton.setOnAction(e -> {
            HomeScreen home = new HomeScreen(primaryStage);
            primaryStage.setScene(home.getScene());
        });
        
        cardContainer.getChildren().addAll(
            castVoteButton, viewResultsButton, logoutButton
        );
        
        root.getChildren().addAll(
            iconLabel, welcomeLabel, instructionLabel, cardContainer
        );
        
        scene = new Scene(root, 700, 600);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
    
    public Scene getScene() {
        return scene;
    }
}
