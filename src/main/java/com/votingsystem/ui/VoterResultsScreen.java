package com.votingsystem.ui;

import com.votingsystem.service.VoterService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;

public class VoterResultsScreen {

    private Stage primaryStage;
    private Scene scene;
    private VoterService voterService;

    public VoterResultsScreen(Stage primaryStage, VoterService voterService) {
        this.primaryStage = primaryStage;
        this.voterService = voterService;
        createUI();
    }

    private void createUI() {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("results-root");

        VBox top = new VBox(10);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(20));

        javafx.scene.control.Label title =
                new javafx.scene.control.Label("🇮🇳 Election Results");
        title.getStyleClass().add("title");

        top.getChildren().add(title);

        Map<String, Integer> results = voterService.getElectionResults();

        // Pie Chart
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Vote Distribution");

        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            pieChart.getData().add(
                    new PieChart.Data(entry.getKey(), entry.getValue())
            );
        }

        // Bar Chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart =
                new BarChart<>(xAxis, yAxis);

        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            series.getData().add(
                    new XYChart.Data<>(entry.getKey(), entry.getValue())
            );
        }

        barChart.getData().add(series);

        HBox charts = new HBox(30, pieChart, barChart);
        charts.setAlignment(Pos.CENTER);
        charts.setPadding(new Insets(20));

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("primary-button");
        backBtn.setOnAction(e ->
                primaryStage.setScene(
                        new HomeScreen(primaryStage).getScene()
                )
        );

        VBox bottom = new VBox(backBtn);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(20));

        root.setTop(top);
        root.setCenter(charts);
        root.setBottom(bottom);

        scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(
                getClass().getResource("/css/styles.css").toExternalForm()
        );
    }

    public Scene getScene() {
        return scene;
    }
}
