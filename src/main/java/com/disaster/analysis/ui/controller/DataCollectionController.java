package com.disaster.analysis.ui.controller;

import com.disaster.analysis.application.dto.CommentDTO;
import com.disaster.analysis.application.dto.PostDTO;
import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.application.mapper.ProjectMapper;
import com.disaster.analysis.config.ApplicationContext;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.ui.navigation.Navigator;
import com.disaster.analysis.ui.navigation.View;
import com.disaster.analysis.application.services.DataCollectionService;
import com.disaster.analysis.application.services.ProjectService;
import com.disaster.analysis.ui.util.DialogUtil;
import com.disaster.analysis.util.LogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


public class DataCollectionController implements Initializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Label projectNameLabel;



    @FXML
    private Label projectDisasterNameLabel;

    @FXML
    private Label projectPlatformLabel;

    @FXML
    private Label projectPeriodLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressDetailsLabel;

    @FXML
    private ListView<String> postsListView;

    @FXML
    private ListView<String> collectionLogListView;

    @FXML
    private Button startButton;

    @FXML
    private Button stopButton;

    @FXML
    private Button backButton;

    @FXML
    private Button analysisButton;

    private DataCollectionService dataCollectionService;
    private ProjectService projectService;
    private final ObservableList<String> collectedPosts;
    private final ObservableList<String> collectionLogs;
    private ProjectDTO currentProject;
    private Task<Void> collectionTask;
    private Navigator navigator;
    private ApplicationContext applicationContext;


    public DataCollectionController() {
        // Initialize observable list for posts
        this.collectedPosts = FXCollections.observableArrayList();
        this.collectionLogs = FXCollections.observableArrayList();
    }


    public DataCollectionController(DataCollectionService dataCollectionService) {
        this.dataCollectionService = dataCollectionService;
        this.collectedPosts = FXCollections.observableArrayList();
        this.collectionLogs = FXCollections.observableArrayList();
    }


    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }


    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // Get DataCollectionService from context if not already set (for testing)
        if (this.dataCollectionService == null) {
            this.dataCollectionService = applicationContext.getDataCollectionService();
        }
        if (this.projectService == null) {
            this.projectService = applicationContext.getProjectService();
        }
        // Get current project from context
        this.currentProject = applicationContext.getCurrentProject();

        // Update UI after dependencies are injected
        updateProjectInfo();
        loadCollectedPosts();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind the posts list to the ListView
        postsListView.setItems(collectedPosts);
        collectionLogListView.setItems(collectionLogs);
        addCollectionLog("Ready to collect data.");

        // Set initial UI state
        updateUIState(false);

        LogUtil.info("DataCollectionController initialize ...");

        // Note: updateProjectInfo() and loadCollectedPosts() are called in setApplicationContext()
        // after dependencies are injected
    }


    private void updateProjectInfo() {
        if (currentProject == null) {
            projectNameLabel.setText("Project: No project selected");
            // projectDetailsLabel.setText("Please select a project from the project list.");
            startButton.setDisable(true);
            return;
        }

        projectNameLabel.setText("Project: " + currentProject.getName());

        String platforms = currentProject.getPlatforms().stream()
                .map(Platform::name)
                .collect(java.util.stream.Collectors.joining(", "));

        // String details = String.format(
        //     "Disaster: %s | Platforms: %s | Period: %s to %s",
        //     currentProject.getDisasterName(),
        //     platforms,
        //     currentProject.getStartDate().format(DATE_FORMATTER),
        //     currentProject.getEndDate().format(DATE_FORMATTER)
        // );

        // projectDetailsLabel.setText(details);

        projectDisasterNameLabel.setText("Disaster: " + currentProject.getDisasterName());
        projectPlatformLabel.setText("Platform: " + platforms);
        projectPeriodLabel.setText("Period: " + currentProject.getStartDate().format(DATE_FORMATTER) + " to " + currentProject.getEndDate().format(DATE_FORMATTER));
        startButton.setDisable(false);
    }


    @FXML
    private void handleStart() {
        if (currentProject == null) {
            DialogUtil.showError("No Project", "Please select a project before starting data collection.");
            return;
        }

        // Clear previous results
        collectedPosts.clear();
        collectionLogs.clear();
        addCollectionLog("Starting data collection for project: " + currentProject.getName());
        progressBar.setProgress(0);
        progressDetailsLabel.setText("");

        // Update UI state
        updateUIState(true);
        updateStatus("Starting data collection...");

        // Create and configure the collection task
        collectionTask = createCollectionTask();

        // Bind progress to UI
        progressBar.progressProperty().bind(collectionTask.progressProperty());

        // Handle task completion
        collectionTask.setOnSucceeded(event -> {
            updateStatus("Data collection completed successfully!");
            updateUIState(false);
            addCollectionLog("Data collection completed.");
            DialogUtil.showInformation("Collection Complete",
                    "Data collection finished. Check the collected posts below.");
        });

        // Handle task failure
        collectionTask.setOnFailed(event -> {
            Throwable exception = collectionTask.getException();
            LogUtil.error("Data collection failed", exception);

            updateStatus("Data collection failed");
            updateUIState(false);
            addCollectionLog("Data collection failed: " + exception.getMessage());

            DialogUtil.showError("Collection Failed",
                    "Data collection failed: " +
                            exception.getMessage());
        });

        // Handle task cancellation
        collectionTask.setOnCancelled(event -> {
            updateStatus("Data collection cancelled by user");
            updateUIState(false);
            addCollectionLog("Data collection cancelled by user.");
            DialogUtil.showInformation("Collection Cancelled", "Data collection was stopped by user.");
        });

        // Start the task in a background thread
        Thread collectionThread = new Thread(collectionTask);
        collectionThread.setDaemon(true);
        collectionThread.start();
    }


    @FXML
    private void handleStop() {
        if (collectionTask != null && collectionTask.isRunning()) {
            updateStatus("Stopping data collection...");
            addCollectionLog("Stopping data collection...");
            collectionTask.cancel();
        }
    }


    @FXML
    private void handleBack() {
        // If collection is running, confirm before navigating away
        if (collectionTask != null && collectionTask.isRunning()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Collection in Progress");
            alert.setHeaderText("Data collection is currently running");
            alert.setContentText("Are you sure you want to go back? This will stop the collection.");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                collectionTask.cancel();
                if (navigator != null) {
                    navigator.navigateBack();
                } else {
                    LogUtil.warn("Navigator not set. Cannot navigate back.");
                }
            }
        } else {
            if (navigator != null) {
                navigator.navigateBack();
            } else {
                LogUtil.warn("Navigator not set. Cannot navigate back.");
            }
        }
    }


    @FXML
    private void handleAnalysis() {
        if (collectionTask != null && collectionTask.isRunning()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Collection in Progress");
            alert.setHeaderText("Data collection is currently running");
            alert.setContentText("Are you sure you want to open Analysis? This will stop the collection.");

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            collectionTask.cancel();
        }

        if (navigator != null) {
            navigator.navigateTo(View.ANALYSIS);
        } else {
            LogUtil.warn("Navigator not set. Cannot navigate to analysis.");
        }
    }


    private Task<Void> createCollectionTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Convert DTO to domain model for service call
                    var project = ProjectMapper.toEntity(currentProject);

                    // Update progress callback
                    dataCollectionService.collectData(project, progress -> {
                        // Update progress on JavaFX Application Thread
                        javafx.application.Platform.runLater(() -> {
                            updateProgress(progress, 100);
                            updateProgressDetails(progress);
                            addCollectionLog(progressDetailsLabel.getText());
                        });
                    });

                    // Load and display collected posts after collection completes
                    javafx.application.Platform.runLater(() -> {
                        loadCollectedPosts();
                    });

                } catch (Exception e) {
                    LogUtil.error("Error during data collection", e);
                    throw e;
                }

                return null;
            }
        };
    }


    private void updateProgressDetails(int progress) {
        String details;

        if (progress < 20) {
            details = "Initializing data collection...";
        } else if (progress < 50) {
            details = "Collecting posts from platforms...";
        } else if (progress < 60) {
            details = "Preprocessing collected posts...";
        } else if (progress < 70) {
            details = "Saving posts to database...";
        } else if (progress < 90) {
            details = "Collecting comments for posts...";
        } else if (progress < 100) {
            details = "Preprocessing and saving comments...";
        } else {
            details = "Collection complete!";
        }

        progressDetailsLabel.setText(details + " (" + progress + "%)");
    }


    private void loadCollectedPosts() {
        if (currentProject == null || currentProject.getId() == null) {
            return;
        }

        LogUtil.info("Loading collected posts and comments for project ID: " + currentProject.getId());

        try {
            var posts = projectService.getPostsByProjectId(currentProject.getId());
            var comments = projectService.getCommentsByProjectId(currentProject.getId());

            collectedPosts.clear();

            // Format posts for display
            for (PostDTO post : posts) {
                String displayText = formatPostForDisplay(post);
                collectedPosts.add(displayText);
            }

            // Format comments for display
            for (CommentDTO comment : comments) {
                String displayText = formatCommentForDisplay(comment);
                collectedPosts.add(displayText);
            }

            updateStatus("Loaded " + posts.size() + " posts and " + comments.size() + " comments");
            addCollectionLog("Loaded " + posts.size() + " posts and " + comments.size() + " comments.");

        } catch (Exception e) {
            LogUtil.warn("Failed to load collected posts and comments", e);
            updateStatus("Failed to load collected data: " + e.getMessage());
            addCollectionLog("Failed to load collected data: " + e.getMessage());
        }
    }


    private String formatPostForDisplay(PostDTO post) {
        String timestamp = post.getPublishedAt() != null
                ? post.getPublishedAt().format(DATE_FORMATTER)
                : "Unknown";

        String content = post.getContent();
        if (content.length() > 100) {
            content = content.substring(0, 97) + "...";
        }

        return String.format("[%s] [POST] %s - %s: %s",
                timestamp,
                post.getPlatform(),
                post.getAuthor() != null ? post.getAuthor() : "Unknown",
                content
        );
    }


    private String formatCommentForDisplay(CommentDTO comment) {
        String timestamp = comment.getPublishedAt() != null
                ? comment.getPublishedAt().format(DATE_FORMATTER)
                : "Unknown";

        String content = comment.getContent();
        if (content.length() > 100) {
            content = content.substring(0, 97) + "...";
        }

        return String.format("[%s] [COMMENT] %s - %s: %s",
                timestamp,
                comment.getPlatform(),
                comment.getAuthor() != null ? comment.getAuthor() : "Unknown",
                content
        );
    }


    private void updateUIState(boolean isCollecting) {
        startButton.setDisable(isCollecting);
        stopButton.setDisable(!isCollecting);
        backButton.setDisable(isCollecting);
        analysisButton.setDisable(isCollecting);
    }


    private void updateStatus(String message) {
        statusLabel.setText(message);
    }


    private void addCollectionLog(String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        collectionLogs.add("[" + timestamp + "] " + message);
        collectionLogListView.scrollTo(collectionLogs.size() - 1);
    }


}
