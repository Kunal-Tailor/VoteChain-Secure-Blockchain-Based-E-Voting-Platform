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
 * Vote confirmation screen
 * Shows confirmation after successful vote casting
 */
public class VoteConfirmationScreen {
    
    private Stage primaryStage;
    private Scene scene;
    private VoterService voterService;
    private String voterId;
    private String candidateName;
    
    public VoteConfirmationScreen(Stage primaryStage, VoterService voterService, 
                                 String voterId, String candidateName) {
        this.primaryStage = primaryStage;
        this.voterService = voterService;
        this.voterId = voterId;
        this.candidateName = candidateName;
        createUI();
    }
    
    private void createUI() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.getStyleClass().add("confirmation-root");
        
        Label iconLabel = new Label("🇮🇳");
        iconLabel.getStyleClass().add("icon-label");
        
        Label successLabel = new Label("Vote Cast Successfully!");
        successLabel.getStyleClass().add("success-title");
        
        Label confirmationLabel = new Label("Your vote has been recorded in the blockchain.");
        confirmationLabel.getStyleClass().add("subtitle");
        
        VBox cardContainer = new VBox(20);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(35, 50, 35, 50));
        cardContainer.getStyleClass().add("card-container");
        
        Label candidateLabel = new Label("🎯 You voted for: " + candidateName);
        candidateLabel.getStyleClass().add("info-label");
        
        Label voterLabel = new Label("🆔 Voter ID: " + voterId);
        voterLabel.getStyleClass().add("info-label");
        
        Label noteLabel = new Label("⚠️ Note: Your vote is now permanent and cannot be changed.");
        noteLabel.getStyleClass().add("note-label");
        
        cardContainer.getChildren().addAll(candidateLabel, voterLabel, noteLabel);
        
        Button viewResultsButton = new Button("📊 View Election Results");
        viewResultsButton.getStyleClass().add("primary-button");
        viewResultsButton.setPrefWidth(350);
        viewResultsButton.setPrefHeight(55);
        viewResultsButton.setOnAction(e -> {
            VoterResultsScreen resultsScreen = new VoterResultsScreen(primaryStage, voterService);
            primaryStage.setScene(resultsScreen.getScene());
        });
        
        Button logoutButton = new Button("🚪 Logout");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setPrefWidth(350);
        logoutButton.setPrefHeight(45);
        logoutButton.setOnAction(e -> {
            HomeScreen home = new HomeScreen(primaryStage);
            primaryStage.setScene(home.getScene());
        });
        
        root.getChildren().addAll(
            iconLabel, successLabel, confirmationLabel, cardContainer,
            viewResultsButton, logoutButton
        );
        
        scene = new Scene(root, 700, 650);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }
    
    public Scene getScene() {
        return scene;
    }
}
