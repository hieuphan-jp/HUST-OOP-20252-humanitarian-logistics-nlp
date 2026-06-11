package com.disaster.analysis.ui.controller;

import com.disaster.analysis.config.ApplicationContext;
import com.disaster.analysis.ui.navigation.Navigator;
import com.disaster.analysis.ui.navigation.View;
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
    private BorderPane contentArea;

    @FXML
    private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize ApplicationContext
        ApplicationContext context = ApplicationContext.getInstance();
        context.initialize();

        // Initialize Navigator
        Navigator navigator = Navigator.getInstance();
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

    @FXML
    private void handleExit() {
        Platform.exit();
    }
}
