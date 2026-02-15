package com.votingsystem.ui;

import com.votingsystem.model.Block;
import com.votingsystem.service.AdminService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class AdminDashboardScreen {

    private Stage primaryStage;
    private Scene scene;
    private AdminService adminService;

    public AdminDashboardScreen(Stage primaryStage, AdminService adminService) {
        this.primaryStage = primaryStage;
        this.adminService = adminService;
        createUI();
    }

    private void createUI() {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");

        // ================= TOP SECTION =================

        VBox topSection = new VBox(10);
        topSection.setAlignment(Pos.CENTER);
        topSection.setPadding(new Insets(20));

        Label title = new Label("🇮🇳 Admin Dashboard");
        title.getStyleClass().add("title");

        boolean isValid = adminService.validateBlockchain();

        Circle statusLight = new Circle(8);
        statusLight.setFill(isValid ? Color.LIMEGREEN : Color.RED);

        Label statusLabel = new Label(
                isValid ? "Blockchain Secure" : "Blockchain Tampered"
        );
        statusLabel.getStyleClass().add(isValid ? "success-label" : "error-label");

        HBox statusBox = new HBox(10, statusLight, statusLabel);
        statusBox.setAlignment(Pos.CENTER);

        topSection.getChildren().addAll(title, statusBox);

        // ================= CHART SECTION =================

        Map<String, Integer> results = adminService.getElectionResults();

        PieChart pieChart = new PieChart();
        pieChart.setTitle("Vote Distribution");

        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            pieChart.getData().add(
                    new PieChart.Data(entry.getKey(), entry.getValue())
            );
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        BarChart<String, Number> barChart =
                new BarChart<>(xAxis, yAxis);

        barChart.setTitle("Vote Comparison");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Votes");

        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            series.getData().add(
                    new XYChart.Data<>(entry.getKey(), entry.getValue())
            );
        }

        barChart.getData().add(series);

        HBox chartsBox = new HBox(40, pieChart, barChart);
        chartsBox.setAlignment(Pos.CENTER);
        chartsBox.setPadding(new Insets(20));

        // ================= BLOCKCHAIN TIMELINE =================
// ================= BLOCKCHAIN TIMELINE =================

        VBox timelineContainer = new VBox(20);
        timelineContainer.setAlignment(Pos.TOP_CENTER);
        timelineContainer.setPadding(new Insets(20));

        Label blockchainTitle = new Label("🔗 Blockchain Timeline");
        blockchainTitle.getStyleClass().add("subtitle");

        timelineContainer.getChildren().add(blockchainTitle);

        List<Block> blocks = adminService.getAllBlocks();

        if (blocks == null || blocks.size() <= 1) {

            Label emptyLabel = new Label("No votes have been cast yet.");
            emptyLabel.setStyle("-fx-font-size:18px; -fx-text-fill: black;");
            timelineContainer.getChildren().add(emptyLabel);

        } else {

            for (int i = 0; i < blocks.size(); i++) {

                Block block = blocks.get(i);

                VBox blockCard = new VBox(8);
                blockCard.setPadding(new Insets(15));
                blockCard.setMaxWidth(500);
                blockCard.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 15;" +
                                "-fx-border-color: #FF9933;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 15;"
                );

                Label index = new Label("Block #" + block.getIndex());
                index.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

                Label voter = new Label("Voter ID: " + block.getVoterId());
                voter.setStyle("-fx-text-fill: black;");

                Label candidate = new Label("Candidate: " + block.getCandidateName());
                candidate.setStyle("-fx-text-fill: black;");

                Label hash = new Label("Hash: " + shortHash(block.getCurrentHash()));
                hash.setStyle("-fx-text-fill: gray;");

                blockCard.getChildren().addAll(index, voter, candidate, hash);

                timelineContainer.getChildren().add(blockCard);

                if (i != blocks.size() - 1) {
                    Label arrow = new Label("⬇");
                    arrow.setStyle("-fx-font-size: 22px; -fx-text-fill: black;");
                    timelineContainer.getChildren().add(arrow);
                }
            }
        }

        ScrollPane blockchainScroll = new ScrollPane(timelineContainer);
        blockchainScroll.setFitToWidth(true);
        blockchainScroll.setPrefHeight(350);
        blockchainScroll.setStyle("-fx-background: transparent;");


        // ================= CENTER SECTION =================

        VBox centerSection = new VBox(20, chartsBox, blockchainScroll);
        centerSection.setAlignment(Pos.TOP_CENTER);

        // ================= BUTTONS =================

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("primary-button");
        refreshBtn.setOnAction(e ->
                primaryStage.setScene(
                        new AdminDashboardScreen(primaryStage, adminService).getScene()
                )
        );

        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.getStyleClass().add("secondary-button");
        logoutBtn.setOnAction(e ->
                primaryStage.setScene(new HomeScreen(primaryStage).getScene())
        );

        HBox bottomButtons = new HBox(20, refreshBtn, logoutBtn);
        bottomButtons.setAlignment(Pos.CENTER);
        bottomButtons.setPadding(new Insets(20));

        // ================= FINAL LAYOUT =================

        root.setTop(topSection);
        root.setCenter(centerSection);
        root.setBottom(bottomButtons);

        scene = new Scene(root, 1100, 850);
        scene.getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm()
        );
    }

    private String shortHash(String hash) {
        if (hash == null || hash.length() < 10) return hash;
        return hash.substring(0, 10) + "...";
    }

    public Scene getScene() {
        return scene;
    }
}
