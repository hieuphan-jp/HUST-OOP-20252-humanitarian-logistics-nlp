package com.disaster.analysis.ui.controller;

import com.disaster.analysis.application.dto.AISummaryDTO;
import com.disaster.analysis.application.dto.CommentDTO;
import com.disaster.analysis.application.dto.PostDTO;
import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.application.mapper.ProjectMapper;
import com.disaster.analysis.application.services.AISummaryService;
import com.disaster.analysis.application.services.DamageClassificationService;
import com.disaster.analysis.application.services.ExportService;
import com.disaster.analysis.application.services.SentimentAnalysisService;
import com.disaster.analysis.config.ApplicationContext;
import com.disaster.analysis.domain.model.enums.DamageCategory;
import com.disaster.analysis.domain.model.enums.Sentiment;
import com.disaster.analysis.domain.model.enums.TimeGranularity;
import com.disaster.analysis.ui.navigation.Navigator;
import com.disaster.analysis.ui.util.DialogUtil;
import com.disaster.analysis.util.LogUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javafx.collections.ObservableList;

public class AnalysisController implements Initializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Project Info Labels
    @FXML
    private Label projectNameLabel;

    @FXML
    private Label projectDetailsLabel;

    // Sentiment Analysis Tab Components
    @FXML
    private ComboBox<String> timeGranularityComboBox;

    @FXML
    private Button runSentimentButton;

    @FXML
    private LineChart<String, Number> sentimentChart;

    @FXML
    private Label positiveCountLabel;

    @FXML
    private Label neutralCountLabel;

    @FXML
    private Label negativeCountLabel;

    // Damage Classification Tab Components
    @FXML
    private ComboBox<String> categoryFilterComboBox;

    @FXML
    private ComboBox<String> contentTypeFilterComboBox;

    @FXML
    private Button runDamageButton;

    @FXML
    private BarChart<String, Number> damageChart;

    @FXML
    private ListView<String> samplePostsListView;

    @FXML
    private Label samplePostsCountLabel;

    // Action Buttons
    @FXML
    private Button backButton;

    @FXML
    private Button exportButton;

    // AI Summary Tab Components
    @FXML
    private Button generateSummaryButton;

    @FXML
    private TextArea summaryTextArea;

    @FXML
    private ProgressIndicator summaryProgressIndicator;

    @FXML
    private Button copySummaryButton;

    @FXML
    private Label summaryMetadataLabel;

    // Services
    private SentimentAnalysisService sentimentService;
    private DamageClassificationService damageService;
    private AISummaryService aiSummaryService;
    private ExportService exportService;

    // Current state
    private ProjectDTO currentProject;
    private AISummaryDTO currentSummary;
    private TimeGranularity currentGranularity = TimeGranularity.DAILY;
    private DamageCategory currentCategoryFilter = null;
    private String currentContentTypeFilter = "All"; // "All", "Posts", "Comments"
    private Navigator navigator;
    private ApplicationContext applicationContext;


    public AnalysisController() {
    }


    public AnalysisController(SentimentAnalysisService sentimentService,
                              DamageClassificationService damageService) {
        this.sentimentService = sentimentService;
        this.damageService = damageService;
    }


    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }


    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // Get services from context if not already set (for testing)
        if (this.sentimentService == null) {
            this.sentimentService = applicationContext.getSentimentAnalysisService();
        }
        if (this.damageService == null) {
            this.damageService = applicationContext.getDamageClassificationService();
        }
        if (this.aiSummaryService == null) {
            this.aiSummaryService = applicationContext.getAISummaryService();
        }
        if (this.exportService == null) {
            this.exportService = applicationContext.getExportService();
        }
        // Get current project from context
        this.currentProject = applicationContext.getCurrentProject();

        // Initialize services after dependencies are injected
        // try {
        //     if (sentimentService != null) {
        //         sentimentService.initialize();
        //     }
        //     if (damageService != null) {
        //         damageService.initialize();
        //     }
        // } catch (Exception e) {
        //     LogUtil.error("Failed to initialize analysis services", e);
        //     DialogUtil.showErrorWithDetails("Initialization Error",
        //         "Failed to initialize analysis services", e);
        // }

        // Update UI after dependencies are injected
        updateProjectInfo();

        // Load existing AI summary if available
        loadExistingSummary();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize time granularity combo box
        timeGranularityComboBox.setItems(FXCollections.observableArrayList(
                "Hourly", "Daily", "Weekly", "Monthly"
        ));
        timeGranularityComboBox.setValue("Daily");

        // Initialize category filter combo box with "All Categories" option
        List<String> categoryOptions = new ArrayList<>();
        categoryOptions.add("All Categories");
        for (DamageCategory category : DamageCategory.values()) {
            categoryOptions.add(category.getDisplayName());
        }
        categoryFilterComboBox.setItems(FXCollections.observableArrayList(categoryOptions));
        categoryFilterComboBox.setValue("All Categories");

        // Initialize content type filter combo box
        if (contentTypeFilterComboBox != null) {
            contentTypeFilterComboBox.setItems(FXCollections.observableArrayList(
                    "All", "Posts", "Comments"
            ));
            contentTypeFilterComboBox.setValue("All");
        }

        // Initialize AI Summary UI components
        if (summaryProgressIndicator != null) {
            summaryProgressIndicator.setVisible(false);
        }
        if (summaryTextArea != null) {
            summaryTextArea.setEditable(false);
            summaryTextArea.setWrapText(true);
        }

        // Note: Service initialization and updateProjectInfo() are called in setApplicationContext()
        // after dependencies are injected
        // loadExistingSummary() is called after dependencies are set in setApplicationContext()
    }


    private void updateProjectInfo() {
        if (currentProject == null) {
            projectNameLabel.setText("Project: No project selected");
            projectDetailsLabel.setText("Please select a project from the project list.");
            runSentimentButton.setDisable(true);
            runDamageButton.setDisable(true);
            return;
        }

        projectNameLabel.setText("Project: " + currentProject.getName());

        String platforms = String.join(", ", currentProject.getPlatforms());

        String details = String.format(
                "Disaster: %s | Platforms: %s | Period: %s to %s",
                currentProject.getDisasterName(),
                platforms,
                currentProject.getStartDate().format(DATE_FORMATTER),
                currentProject.getEndDate().format(DATE_FORMATTER)
        );

        projectDetailsLabel.setText(details);
        runSentimentButton.setDisable(false);
        runDamageButton.setDisable(false);
    }


    @FXML
    private void handleRunSentimentAnalysis() {
        if (currentProject == null || currentProject.getId() == null) {
            DialogUtil.showError("No Project", "Please select a project before running analysis.");
            return;
        }

        // Disable button during analysis
        runSentimentButton.setDisable(true);
        runSentimentButton.setText("Analyzing...");

        // Create background task
        Task<String> analysisTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                // Run sentiment analysis on all project posts
                sentimentService.analyzeProjectPosts(currentProject.getId());

                // Run sentiment analysis on all project comments
                sentimentService.analyzeProjectComments(currentProject.getId());

                return "Analysis completed for both posts and comments";
            }
        };

        // Handle task completion
        analysisTask.setOnSucceeded(event -> {
            runSentimentButton.setDisable(false);
            runSentimentButton.setText("Run Sentiment Analysis");
            updateSentimentChart();
            DialogUtil.showInformation("Analysis Complete", analysisTask.getValue());
        });

        // Handle task failure
        analysisTask.setOnFailed(event -> {
            Throwable exception = analysisTask.getException();
            LogUtil.error("Sentiment analysis failed", exception);

            runSentimentButton.setDisable(false);
            runSentimentButton.setText("Run Sentiment Analysis");

            DialogUtil.showError("Sentiment Analysis Failed", exception.getMessage());
        });

        // Start the task in a background thread
        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }


    @FXML
    private void handleRunDamageAnalysis() {
        if (currentProject == null || currentProject.getId() == null) {
            DialogUtil.showError("No Project", "Please select a project before running analysis.");
            return;
        }


        // Disable button during analysis
        runDamageButton.setDisable(true);
        runDamageButton.setText("Analyzing...");

        // Create background task
        Task<String> analysisTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                // Run damage classification on all project posts
                int postCount = damageService.classifyProjectPosts(currentProject.getId());

                // Run damage classification on all project comments
                int commentCount = damageService.classifyProjectComments(currentProject.getId());

                return String.format("Classified %d posts and %d comments", postCount, commentCount);
            }
        };

        // Handle task completion
        analysisTask.setOnSucceeded(event -> {
            runDamageButton.setDisable(false);
            runDamageButton.setText("Run Damage Analysis");
            String result = analysisTask.getValue();
            updateDamageChart();
            updateSamplePosts();
            DialogUtil.showInformation("Analysis Complete",
                    "Damage classification completed! " + result);
        });

        // Handle task failure
        analysisTask.setOnFailed(event -> {
            Throwable exception = analysisTask.getException();
            LogUtil.error("Damage analysis failed", exception);

            runDamageButton.setDisable(false);
            runDamageButton.setText("Run Damage Analysis");

            DialogUtil.showError("Damage Analysis Failed", exception.getMessage());
        });

        // Start the task in a background thread
        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }


    private void updateSentimentChart() {
        if (currentProject == null || currentProject.getId() == null) {
            return;
        }

        try {
            // Get sentiment time series data
            Map<LocalDateTime, Map<Sentiment, Long>> timeSeries =
                    sentimentService.getSentimentTimeSeries(currentProject.getId(), currentGranularity);

            // Clear existing data
            sentimentChart.getData().clear();

            // Create series for each sentiment type
            XYChart.Series<String, Number> positiveSeries = new XYChart.Series<>();
            positiveSeries.setName("Positive");

            XYChart.Series<String, Number> neutralSeries = new XYChart.Series<>();
            neutralSeries.setName("Neutral");

            XYChart.Series<String, Number> negativeSeries = new XYChart.Series<>();
            negativeSeries.setName("Negative");

            // Track totals for statistics
            long totalPositive = 0;
            long totalNeutral = 0;
            long totalNegative = 0;

            // Populate series with data
            DateTimeFormatter formatter = getFormatterForGranularity(currentGranularity);

            for (Map.Entry<LocalDateTime, Map<Sentiment, Long>> entry : timeSeries.entrySet()) {
                String timeLabel = entry.getKey().format(formatter);
                Map<Sentiment, Long> sentimentCounts = entry.getValue();

                long positiveCount = sentimentCounts.getOrDefault(Sentiment.POSITIVE, 0L);
                long neutralCount = sentimentCounts.getOrDefault(Sentiment.NEUTRAL, 0L);
                long negativeCount = sentimentCounts.getOrDefault(Sentiment.NEGATIVE, 0L);

                positiveSeries.getData().add(new XYChart.Data<>(timeLabel, positiveCount));
                neutralSeries.getData().add(new XYChart.Data<>(timeLabel, neutralCount));
                negativeSeries.getData().add(new XYChart.Data<>(timeLabel, negativeCount));

                totalPositive += positiveCount;
                totalNeutral += neutralCount;
                totalNegative += negativeCount;
            }

            // Add series to chart
            sentimentChart.getData().add(positiveSeries);
            sentimentChart.getData().add(neutralSeries);
            sentimentChart.getData().add(negativeSeries);

            // Update statistics labels
            positiveCountLabel.setText(String.valueOf(totalPositive));
            neutralCountLabel.setText(String.valueOf(totalNeutral));
            negativeCountLabel.setText(String.valueOf(totalNegative));

        } catch (Exception e) {
            LogUtil.error("Failed to update sentiment chart", e);
            DialogUtil.showError("Chart Update Failed", "Failed to update sentiment chart");
        }
    }


    private void updateDamageChart() {
        if (currentProject == null || currentProject.getId() == null) {
            return;
        }

        try {
            // Get damage category distribution
            Map<DamageCategory, Long> distribution =
                    damageService.getDamageCategoryDistribution(currentProject.getId());

            // Clear existing data
            damageChart.getData().clear();

            // Create series for damage categories
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Damage Categories");

            // Sort categories by count (descending)
            List<Map.Entry<DamageCategory, Long>> sortedEntries = distribution.entrySet().stream()
                    .sorted(Map.Entry.<DamageCategory, Long>comparingByValue().reversed())
                    .toList();

            if (damageChart.getXAxis() instanceof CategoryAxis xAxis) {
                xAxis.getCategories().clear();

                // TẠO DANH SÁCH NHÃN ĐỘC LẬP
                ObservableList<String> categories = FXCollections.observableArrayList();
                for (Map.Entry<DamageCategory, Long> entry : sortedEntries) {
                    categories.add(entry.getKey().getDisplayName());
                }
                // Ép trục X phải chia đều không gian cho các nhãn này
                xAxis.setCategories(categories);
            }

            // Populate series with data
            for (Map.Entry<DamageCategory, Long> entry : sortedEntries) {
                String categoryName = entry.getKey().getDisplayName();
                long count = entry.getValue();
                series.getData().add(new XYChart.Data<>(categoryName, count));
            }

            // Add series to chart
            damageChart.getData().add(series);

        } catch (Exception e) {
            LogUtil.error("Failed to update damage chart", e);
            DialogUtil.showError("Chart Update Failed", "Failed to update damage chart");
        }
    }


    @FXML
    private void handleTimeGranularityChange() {
        String selected = timeGranularityComboBox.getValue();

        if (selected == null) {
            return;
        }

        // Map selection to TimeGranularity enum
        currentGranularity = switch (selected) {
            case "Hourly" -> TimeGranularity.HOURLY;
            case "Weekly" -> TimeGranularity.WEEKLY;
            case "Monthly" -> TimeGranularity.MONTHLY;
            default -> TimeGranularity.DAILY;
        };

        // Refresh the sentiment chart with new granularity
        updateSentimentChart();
    }


    @FXML
    private void handleCategoryFilterChange() {
        String selected = categoryFilterComboBox.getValue();

        if (selected == null) {
            return;
        }

        // Map selection to DamageCategory enum
        if (selected.equals("All Categories")) {
            currentCategoryFilter = null;
        } else {
            for (DamageCategory category : DamageCategory.values()) {
                if (category.getDisplayName().equals(selected)) {
                    currentCategoryFilter = category;
                    break;
                }
            }
        }

        // Update sample posts list
        updateSamplePosts();
    }


    @FXML
    private void handleContentTypeFilterChange() {
        String selected = contentTypeFilterComboBox.getValue();

        if (selected == null) {
            return;
        }

        currentContentTypeFilter = selected;

        // Update sample posts list
        updateSamplePosts();
    }


    private void updateSamplePosts() {
        if (currentProject == null || currentProject.getId() == null) {
            return;
        }

        try {
            List<String> formattedItems = new ArrayList<>();
            int postCount = 0;
            int commentCount = 0;

            // Fetch and format posts if needed
            if ("All".equals(currentContentTypeFilter) || "Posts".equals(currentContentTypeFilter)) {
                List<PostDTO> posts;

                if (currentCategoryFilter == null) {
                    // Show all posts - use ProjectService to get DTOs
                    posts = applicationContext.getProjectService().getPostsByProjectId(currentProject.getId());
                } else {
                    // Show posts for selected category
                    posts = damageService.getPostsByCategory(currentProject.getId(), currentCategoryFilter);
                }

                postCount = posts.size();

                // Format posts for display
                formattedItems.addAll(posts.stream()
                        .map(this::formatPostForDisplay)
                        .toList());
            }

            // Fetch and format comments if needed
            if ("All".equals(currentContentTypeFilter) || "Comments".equals(currentContentTypeFilter)) {
                List<CommentDTO> comments;

                if (currentCategoryFilter == null) {
                    // Show all comments - use ProjectService to get DTOs
                    comments = applicationContext.getProjectService().getCommentsByProjectId(currentProject.getId());
                } else {
                    // Show comments for selected category
                    comments = damageService.getCommentsByCategory(currentProject.getId(), currentCategoryFilter);
                }

                commentCount = comments.size();

                // Format comments for display
                formattedItems.addAll(comments.stream()
                        .map(this::formatCommentForDisplay)
                        .toList());
            }

            // Update ListView
            // samplePostsListView.setItems(FXCollections.observableArrayList(formattedItems));

            // Update count label
            String filterText = currentCategoryFilter != null
                    ? " in " + currentCategoryFilter.getDisplayName()
                    : "";

            String countText;
            if ("All".equals(currentContentTypeFilter)) {
                countText = String.format("(%d posts, %d comments%s)", postCount, commentCount, filterText);
            } else if ("Posts".equals(currentContentTypeFilter)) {
                countText = String.format("(%d posts%s)", postCount, filterText);
            } else {
                countText = String.format("(%d comments%s)", commentCount, filterText);
            }

            //samplePostsCountLabel.setText(countText);

        } catch (Exception e) {
            LogUtil.error("Failed to update sample posts", e);
            DialogUtil.showError("Update Failed", "Failed to update sample posts list");
        }
    }


    private String formatPostForDisplay(PostDTO post) {
        String timestamp = post.getPublishedAt() != null
                ? post.getPublishedAt().format(DATE_FORMATTER)
                : "Unknown";

        String sentiment = post.getSentiment() != null
                ? post.getSentiment()
                : "N/A";

        String categories = "";
        if (post.getDamageCategories() != null && !post.getDamageCategories().isEmpty()) {
            categories = post.getDamageCategories().stream()
                    .map(cat -> {
                        try {
                            return DamageCategory.valueOf(cat).getDisplayName();
                        } catch (IllegalArgumentException e) {
                            return cat;
                        }
                    })
                    .collect(Collectors.joining(", "));
        }

        String content = post.getContent();
        if (content.length() > 80) {
            content = content.substring(0, 77) + "...";
        }

        return String.format("[POST] [%s] %s | Sentiment: %s | Categories: %s\n%s",
                timestamp,
                post.getPlatform(),
                sentiment,
                categories.isEmpty() ? "None" : categories,
                content
        );
    }


    private String formatCommentForDisplay(CommentDTO comment) {
        String timestamp = comment.getPublishedAt() != null
                ? comment.getPublishedAt().format(DATE_FORMATTER)
                : "Unknown";

        String sentiment = comment.getSentiment() != null
                ? comment.getSentiment()
                : "N/A";

        String categories = "";
        if (comment.getDamageCategories() != null && !comment.getDamageCategories().isEmpty()) {
            categories = comment.getDamageCategories().stream()
                    .map(cat -> {
                        try {
                            return DamageCategory.valueOf(cat).getDisplayName();
                        } catch (IllegalArgumentException e) {
                            return cat;
                        }
                    })
                    .collect(Collectors.joining(", "));
        }

        String content = comment.getContent();
        if (content.length() > 80) {
            content = content.substring(0, 77) + "...";
        }

        return String.format("[COMMENT] [%s] %s | Sentiment: %s | Categories: %s\n%s",
                timestamp,
                comment.getPlatform(),
                sentiment,
                categories.isEmpty() ? "None" : categories,
                content
        );
    }


    private DateTimeFormatter getFormatterForGranularity(TimeGranularity granularity) {
        return switch (granularity) {
            case HOURLY -> DateTimeFormatter.ofPattern("MM-dd HH:00");
            case DAILY -> DateTimeFormatter.ofPattern("MM-dd");
            case WEEKLY -> DateTimeFormatter.ofPattern("MM-dd");
            case MONTHLY -> DateTimeFormatter.ofPattern("yyyy-MM");
        };
    }


    @FXML
    private void handleBack() {
        if (navigator != null) {
            navigator.navigateBack();
        } else {
            LogUtil.warn("Navigator not set. Cannot navigate back.");
        }
    }


    @FXML
    private void handleExport() {
        // Validate that currentProject exists and has ID
        if (currentProject == null || currentProject.getId() == null) {
            DialogUtil.showError("No Project", "Please select a project before exporting.");
            return;
        }

        // Create FileChooser with title "Export Project Data"
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Project Data");

        // Set initial filename to project name with .xlsx extension
        String initialFilename = sanitizeFilename(currentProject.getName()) + ".xlsx";
        fileChooser.setInitialFileName(initialFilename);

        // Add extension filter for "Excel Files (*.xlsx)"
        FileChooser.ExtensionFilter excelFilter =
                new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx");
        fileChooser.getExtensionFilters().add(excelFilter);

        // Set initial directory to user's documents folder or last used location
        String userHome = System.getProperty("user.home");
        File documentsDir = new File(userHome, "Documents");
        if (documentsDir.exists() && documentsDir.isDirectory()) {
            fileChooser.setInitialDirectory(documentsDir);
        } else {
            fileChooser.setInitialDirectory(new File(userHome));
        }

        // Show FileChooser dialog
        File selectedFile = fileChooser.showSaveDialog(exportButton.getScene().getWindow());

        // Handle cancellation (return without error)
        if (selectedFile == null) {
            return;
        }

        // Ensure .xlsx extension
        File finalFile = new File(ensureXlsxExtension(selectedFile.getAbsolutePath()));

        // Check if file exists and prompt for overwrite confirmation
        if (finalFile.exists()) {
            boolean overwrite = DialogUtil.showConfirmation(
                    "File Exists",
                    "The file already exists. Do you want to overwrite it?"
            );
            if (!overwrite) {
                return;
            }
        }

        // Start background export task
        startExportTask(finalFile.toPath());
    }


    private void startExportTask(Path outputPath) {
        // Disable Export button before starting export
        exportButton.setDisable(true);
        exportButton.setText("Exporting...");

        // Create JavaFX Task for background export
        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Convert DTO to domain model for service call
                var project = ProjectMapper.toDomain(currentProject);
                // Call exportService.exportProject in Task.call()
                exportService.exportProject(project, outputPath);
                return null;
            }
        };

        // On success: show success dialog with file location, re-enable button
        exportTask.setOnSucceeded(event -> {
            exportButton.setDisable(false);
            exportButton.setText("Export Results");

            DialogUtil.showInformation(
                    "Export Successful",
                    "Project data has been exported successfully to:\n" + outputPath.toString()
            );

            LogUtil.info("Export completed successfully: " + outputPath);
        });

        // On failure: log error, show error dialog, re-enable button
        exportTask.setOnFailed(event -> {
            Throwable exception = exportTask.getException();
            LogUtil.error("Export failed", exception);

            exportButton.setDisable(false);
            exportButton.setText("Export Results");

            // Show specific error message based on exception type and message
            DialogUtil.showError("Export Failed", exception.getMessage());
        });

        // Start the task in a background thread
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }


    private void loadExistingSummary() {
        // Check if we have the necessary dependencies and current project
        if (aiSummaryService == null || currentProject == null || currentProject.getId() == null) {
            return;
        }

        try {
            // Try to get existing summary from database
            Optional<AISummaryDTO> existingSummary = aiSummaryService.getExistingSummary(currentProject.getId());

            if (existingSummary.isPresent()) {
                // Summary exists - display it
                currentSummary = existingSummary.get();

                // Display summary text
                if (summaryTextArea != null) {
                    summaryTextArea.setText(currentSummary.getSummaryText());
                }

                // Update metadata label
                if (summaryMetadataLabel != null) {
                    String metadata = formatSummaryMetadata(currentSummary);
                    summaryMetadataLabel.setText(metadata);
                }

                // Change button text to "Regenerate Summary"
                if (generateSummaryButton != null) {
                    generateSummaryButton.setText("Regenerate Summary");
                }

                LogUtil.info("Loaded existing AI summary for project: " + currentProject.getName());
            } else {
                // No summary exists - leave UI in default state
                currentSummary = null;

                if (summaryTextArea != null) {
                    summaryTextArea.setText("");
                }

                if (summaryMetadataLabel != null) {
                    summaryMetadataLabel.setText("");
                }

                if (generateSummaryButton != null) {
                    generateSummaryButton.setText("Generate AI Summary");
                }
            }
        } catch (Exception e) {
            LogUtil.error("Failed to load existing AI summary", e);
            // Don't show error dialog - just log it and leave UI in default state
            // This is not critical enough to interrupt the user
        }
    }


    private String formatSummaryMetadata(AISummaryDTO summary) {
        if (summary == null) {
            return "";
        }

        // Format the timestamp
        String timestamp = "Unknown";
        if (summary.getGeneratedAt() != null) {
            LocalDateTime generatedAt = summary.getGeneratedAt();
            LocalDateTime now = LocalDateTime.now();

            // Calculate time difference
            long minutesAgo = java.time.Duration.between(generatedAt, now).toMinutes();

            if (minutesAgo < 1) {
                timestamp = "just now";
            } else if (minutesAgo < 60) {
                timestamp = minutesAgo + " minute" + (minutesAgo == 1 ? "" : "s") + " ago";
            } else if (minutesAgo < 1440) { // Less than 24 hours
                long hoursAgo = minutesAgo / 60;
                timestamp = hoursAgo + " hour" + (hoursAgo == 1 ? "" : "s") + " ago";
            } else {
                long daysAgo = minutesAgo / 1440;
                timestamp = daysAgo + " day" + (daysAgo == 1 ? "" : "s") + " ago";
            }
        }

        // Format the model name
        String model = summary.getModel() != null ? summary.getModel() : "Unknown";

        // Format the data counts
        String dataCounts = String.format("Analyzed: %d posts, %d comments",
                summary.getPostsAnalyzed(),
                summary.getCommentsAnalyzed());

        return String.format("Model: %s | Generated: %s | %s", model, timestamp, dataCounts);
    }


    @FXML
    private void handleGenerateSummary() {
        if (currentProject == null || currentProject.getId() == null) {
            DialogUtil.showError("No Project", "Please select a project before generating a summary.");
            return;
        }

        if (aiSummaryService == null) {
            DialogUtil.showError("Service Unavailable", "AI Summary service is not available.");
            return;
        }

        // Disable button and show progress indicator
        generateSummaryButton.setDisable(true);
        summaryProgressIndicator.setVisible(true);

        // Create background task for summary generation
        Task<AISummaryDTO> summaryTask = new Task<AISummaryDTO>() {
            @Override
            protected AISummaryDTO call() throws Exception {
                // Call the AI summary service to generate the summary
                return aiSummaryService.generateProjectSummary(currentProject.getId());
            }
        };

        // Handle task success
        summaryTask.setOnSucceeded(event -> {
            try {
                // Get the generated summary
                AISummaryDTO generatedSummary = summaryTask.getValue();
                currentSummary = generatedSummary;

                // Update summary text area
                if (summaryTextArea != null && generatedSummary != null) {
                    summaryTextArea.setText(generatedSummary.getSummaryText());
                }

                // Update metadata label
                if (summaryMetadataLabel != null) {
                    String metadata = formatSummaryMetadata(generatedSummary);
                    summaryMetadataLabel.setText(metadata);
                }

                // Change button text to "Regenerate Summary"
                generateSummaryButton.setText("Regenerate Summary");

                LogUtil.info("Successfully generated AI summary for project: " + currentProject.getName());

            } finally {
                // Re-enable button and hide progress indicator
                generateSummaryButton.setDisable(false);
                summaryProgressIndicator.setVisible(false);
            }
        });

        // Handle task failure
        summaryTask.setOnFailed(event -> {
            try {
                Throwable exception = summaryTask.getException();
                LogUtil.error("AI summary generation failed", exception);

                DialogUtil.showError("AI Summary Generation Error", exception.getMessage());

            } finally {
                // Re-enable button and hide progress indicator
                generateSummaryButton.setDisable(false);
                summaryProgressIndicator.setVisible(false);
            }
        });

        // Start the task in a background thread
        Thread summaryThread = new Thread(summaryTask);
        summaryThread.setDaemon(true);
        summaryThread.start();
    }


    @FXML
    private void handleCopySummary() {
        // Check if there is text to copy
        if (summaryTextArea == null || summaryTextArea.getText() == null || summaryTextArea.getText().trim().isEmpty()) {
            DialogUtil.showWarning("No Summary", "There is no summary text to copy. Please generate a summary first.");
            return;
        }

        try {
            // Get the summary text
            String summaryText = summaryTextArea.getText();

            // Create clipboard content
            ClipboardContent content = new ClipboardContent();
            content.putString(summaryText);

            // Copy to system clipboard
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(content);

            // Show confirmation message
            DialogUtil.showInformation("Copied", "Summary text has been copied to clipboard.");

            LogUtil.info("Summary text copied to clipboard");

        } catch (Exception e) {
            LogUtil.error("Failed to copy summary to clipboard", e);
            DialogUtil.showError("Copy Failed", "Failed to copy summary text to clipboard.");
        }
    }


    private String ensureXlsxExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "export.xlsx";
        }

        if (!filename.toLowerCase().endsWith(".xlsx")) {
            return filename + ".xlsx";
        }

        return filename;
    }


    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "export";
        }

        // Remove or replace invalid characters for Windows, macOS, and Linux
        // Invalid characters: < > : " / \ | ? *
        String sanitized = filename.replaceAll("[<>:\"/\\\\|?*]", "_");

        // Remove leading/trailing whitespace and dots
        sanitized = sanitized.trim().replaceAll("^\\.+|\\.+$", "");

        // If the result is empty, use a default name
        if (sanitized.isEmpty()) {
            return "export";
        }

        return sanitized;
    }

}
