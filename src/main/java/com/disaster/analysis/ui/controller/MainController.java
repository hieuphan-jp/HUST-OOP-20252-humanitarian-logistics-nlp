package com.disaster.analysis.ui.controller;

import com.disaster.analysis.config.ApplicationContext;
import com.disaster.analysis.ui.navigation.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.application.Platform;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private BorderPane mainPane;

    @FXML
    private BorderPane contentArea;

    @FXML
    private Label statusLabel;

    private Navigator navigator;
    private ApplicationContext context;

    private boolean darkMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize ApplicationContext
        context = ApplicationContext.getInstance();
        context.initialize();

        // Initialize Navigator
        navigator = Navigator.getInstance();
        navigator.initialize(contentArea, context);

        // Navigate to the default view (project list)
        navigator.navigateTo(View.PROJECT_LIST);

        updateStatus("Ready");
    }


    public void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    // Menu action handlers

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleNewProject() {
        navigator.navigateTo(View.PROJECT_FORM);
        updateStatus("Creating new project");
    }

    @FXML
    private void handleOpenProject() {
        navigator.navigateTo(View.PROJECT_LIST);
        updateStatus("Viewing projects");
    }

    @FXML
    private void handleViewProjects() {
        navigator.navigateTo(View.PROJECT_LIST);
        updateStatus("Viewing projects");
    }

    @FXML
    private void handleCollectData() {
        navigator.navigateTo(View.DATA_COLLECTION);
        updateStatus("Collecting data");
    }

    @FXML
    private void handleSentimentAnalysis() {
        navigator.navigateTo(View.ANALYSIS);
        updateStatus("Running analysis");
    }

    @FXML
    private void handleDamageAnalysis() {
        navigator.navigateTo(View.ANALYSIS);
        updateStatus("Running analysis");
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Disaster Social Media Analysis");
        alert.setContentText("Version 1.0\n\n" +
                "A Java desktop application for collecting and analyzing " +
                "social media data related to natural disasters.\n\n" +
                "Built with JavaFX 24 and Java 25");
        alert.showAndWait();
    }

    @FXML
    private void handleToggleTheme() {
        darkMode = !darkMode;

        if (darkMode) {
            if (!mainPane.getStyleClass().contains("dark")) mainPane.getStyleClass().add("dark");
            updateStatus("Dark mode: ON");
        } else {
            mainPane.getStyleClass().remove("dark");
            updateStatus("Dark mode: OFF");
        }
    }

    public boolean isDarkModeEnabled() {
        return mainPane.getStyleClass().contains("dark");
    }
}
