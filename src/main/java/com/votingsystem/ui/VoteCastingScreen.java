package com.votingsystem.ui;

import com.votingsystem.service.VoterService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

/**
 * Vote casting screen
 * Allows voters to select a candidate and cast their vote
 */
public class VoteCastingScreen {
    
    private Stage primaryStage;
    private Scene scene;
    private VoterService voterService;
    private String voterId;
    private ToggleGroup candidateGroup;
    
    // List of candidates
    private String[] candidates = {
        "Candidate A - Ram Kumar Sharma",
        "Candidate B - Priya Singh",
        "Candidate C - Amit Patel",
        "Candidate D - Anushka Reddy"
    };
    
    public VoteCastingScreen(Stage primaryStage, VoterService voterService, String voterId) {
        this.primaryStage = primaryStage;
        this.voterService = voterService;
        this.voterId = voterId;
        createUI();
    }
    
    private void createUI() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("vote-root");
        
        Label iconLabel = new Label("🇮🇳");
        iconLabel.getStyleClass().add("icon-label");
        
        Label titleLabel = new Label("Cast Your Vote");
        titleLabel.getStyleClass().add("title");
        
        Label instructionLabel = new Label("Select a candidate from the list below:");
        instructionLabel.getStyleClass().add("subtitle");
        
        VBox cardContainer = new VBox(20);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(30, 40, 30, 40));
        cardContainer.getStyleClass().add("card-container");
        
        candidateGroup = new ToggleGroup();
        VBox candidatesBox = new VBox(18);
        candidatesBox.setAlignment(Pos.CENTER);
        candidatesBox.setPadding(new Insets(20));
        
        String[] candidateIcons = {"👤", "👩", "👨", "👩‍💼"};
        for (int i = 0; i < candidates.length; i++) {
            RadioButton radioButton = new RadioButton(candidateIcons[i] + " " + candidates[i]);
            radioButton.setToggleGroup(candidateGroup);
            radioButton.getStyleClass().add("radio-button");
            candidatesBox.getChildren().add(radioButton);
        }
        
        cardContainer.getChildren().add(candidatesBox);
        
        Button submitButton = new Button("✅ Submit Vote");
        submitButton.getStyleClass().add("primary-button");
        submitButton.setPrefWidth(350);
        submitButton.setPrefHeight(55);
        submitButton.setOnAction(e -> submitVote());
        
        Button backButton = new Button("← Back to Dashboard");
        backButton.getStyleClass().add("secondary-button");
        backButton.setPrefWidth(350);
        backButton.setPrefHeight(45);
        backButton.setOnAction(e -> {
            VoterDashboardScreen dashboard = new VoterDashboardScreen(primaryStage, voterService, voterId);
            primaryStage.setScene(dashboard.getScene());
        });
        
        root.getChildren().addAll(
            iconLabel, titleLabel, instructionLabel, cardContainer, 
            submitButton, backButton
        );
        
        scene = new Scene(root, 750, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }

    private void submitVote() {

        RadioButton selected =
                (RadioButton) candidateGroup.getSelectedToggle();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING,
                    "Selection Required",
                    "Please select a candidate.");
            return;
        }

        String candidateName = selected.getText();
        final String candidate = candidateName.split(" - ")[0].trim();

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(120,120);

        StackPane overlay = new StackPane(progress);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

        ((VBox) scene.getRoot()).getChildren().add(overlay);

        javafx.concurrent.Task<Boolean> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Boolean call() {
                        return voterService.castVote(voterId, candidate);
                    }
                };

        task.setOnSucceeded(e -> {

            ((VBox) scene.getRoot()).getChildren().remove(overlay);

            if(task.getValue()){
                VoteConfirmationScreen confirm =
                        new VoteConfirmationScreen(
                                primaryStage,
                                voterService,
                                voterId,
                                candidateName);
                primaryStage.setScene(confirm.getScene());
            }
        });

        new Thread(task).start();
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
