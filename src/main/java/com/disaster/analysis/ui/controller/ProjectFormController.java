package com.disaster.analysis.ui.controller;

import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.config.ApplicationContext;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.ui.navigation.Navigator;
import com.disaster.analysis.application.services.ProjectService;
import com.disaster.analysis.ui.util.DialogUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


public class ProjectFormController implements Initializable {

    // Form fields
    @FXML
    private Label formTitleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField disasterNameField;

    @FXML
    private TextArea keywordsArea;

    @FXML
    private TextArea hashtagsArea;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private VBox platformsContainer;  // Container for dynamically generated checkboxes

    // Map to store dynamically created checkboxes
    private final Map<Platform, CheckBox> platformCheckBoxes = new HashMap<>();

    // Error labels
    @FXML
    private Label nameErrorLabel;

    @FXML
    private Label disasterNameErrorLabel;

    @FXML
    private Label keywordsErrorLabel;

    @FXML
    private Label dateErrorLabel;

    @FXML
    private Label platformsErrorLabel;

    // Buttons
    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private ProjectService projectService;
    private ProjectDTO editingProject; // null for new project, set for editing
    private Navigator navigator;
    private ApplicationContext applicationContext;


    public ProjectFormController() {
    }


    public ProjectFormController(ProjectService projectService) {
        this.projectService = projectService;
    }


    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }


    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // Get ProjectService from context if not already set (for testing)
        if (this.projectService == null) {
            this.projectService = applicationContext.getProjectService();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Generate platform checkboxes dynamically
        generatePlatformCheckboxes();

        // Set up form validation listeners
        setupValidationListeners();

        // If editing an existing project, populate the form
        if (editingProject != null) {
            populateForm(editingProject);
            formTitleLabel.setText("Edit Project");
        }
    }


    private void generatePlatformCheckboxes() {
        // Clear existing checkboxes (if any)
        platformsContainer.getChildren().clear();
        platformCheckBoxes.clear();

        // Create checkbox for each platform
        for (Platform platform : Platform.values()) {
            CheckBox checkBox = new CheckBox(getPlatformDisplayName(platform));
            checkBox.setId(platform.name().toLowerCase() + "CheckBox");

            // Add listener to clear error when any checkbox is selected
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal || isAnyPlatformSelected()) {
                    hideError(platformsErrorLabel);
                }
            });

            // Store checkbox in map
            platformCheckBoxes.put(platform, checkBox);

            // Add to container
            platformsContainer.getChildren().add(checkBox);
        }
    }


    private String getPlatformDisplayName(Platform platform) {
        return platform.getDisplayName();
    }


    private boolean isAnyPlatformSelected() {
        return platformCheckBoxes.values().stream()
                .anyMatch(CheckBox::isSelected);
    }


    public void setEditingProject(ProjectDTO project) {
        this.editingProject = project;
        if (project != null && formTitleLabel != null) {
            populateForm(project);
            formTitleLabel.setText("Edit Project");
        }
    }


    private void setupValidationListeners() {
        // Clear error when user starts typing
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                hideError(nameErrorLabel);
            }
        });

        disasterNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                hideError(disasterNameErrorLabel);
            }
        });

        keywordsArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().isEmpty()) {
                hideError(keywordsErrorLabel);
            }
        });

        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                hideError(dateErrorLabel);
            }
        });

        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                hideError(dateErrorLabel);
            }
        });

        // Platform checkbox listeners are set up in generatePlatformCheckboxes()
    }


    private void populateForm(ProjectDTO project) {
        nameField.setText(project.getName());
        disasterNameField.setText(project.getDisasterName());

        // Join keywords with commas
        keywordsArea.setText(String.join(", ", project.getKeywords()));

        // Join hashtags with commas
        if (project.getHashtags() != null && !project.getHashtags().isEmpty()) {
            hashtagsArea.setText(String.join(", ", project.getHashtags()));
        }

        // Set dates
        if (project.getStartDate() != null) {
            startDatePicker.setValue(project.getStartDate().toLocalDate());
        }

        if (project.getEndDate() != null) {
            endDatePicker.setValue(project.getEndDate().toLocalDate());
        }

        // Set platform checkboxes dynamically
        Set<String> platformNames = project.getPlatforms().stream()
                .map(Platform::name)
                .collect(Collectors.toSet());
        for (Map.Entry<Platform, CheckBox> entry : platformCheckBoxes.entrySet()) {
            Platform platform = entry.getKey();
            CheckBox checkBox = entry.getValue();
            checkBox.setSelected(platformNames.contains(platform.name()));
        }
    }


    @FXML
    private void handleSave() {
        // Clear all previous errors
        clearAllErrors();

        // Validate form
        if (!validateForm()) {
            return; // Validation failed, errors are displayed
        }

        try {
            // Parse form data
            String name = nameField.getText().trim();
            String disasterName = disasterNameField.getText().trim();
            List<String> keywords = parseCommaSeparated(keywordsArea.getText());
            List<String> hashtags = parseCommaSeparated(hashtagsArea.getText());

            // Dates are now optional - use null if not provided
            LocalDateTime startDate = startDatePicker.getValue() != null
                    ? startDatePicker.getValue().atStartOfDay()
                    : null;
            LocalDateTime endDate = endDatePicker.getValue() != null
                    ? endDatePicker.getValue().atTime(LocalTime.MAX)
                    : null;

            Set<Platform> platforms = getSelectedPlatforms();

            if (editingProject == null) {
                // Create new project
                projectService.createProject(new ProjectDTO(name, disasterName, keywords, hashtags,
                        startDate, endDate, platforms));
                DialogUtil.showInformation("Project Created",
                        "Project '" + name + "' has been created successfully.");
            } else {
                // Update existing project DTO
                editingProject.setName(name);
                editingProject.setDisasterName(disasterName);
                editingProject.setKeywords(keywords);
                editingProject.setHashtags(hashtags);
                editingProject.setStartDate(startDate);
                editingProject.setEndDate(endDate);
                // Convert Platform enums to strings for DTO
                editingProject.setPlatforms(platforms);
                editingProject.setLastModified(LocalDateTime.now());

                projectService.updateProject(editingProject);
                DialogUtil.showInformation("Project Updated",
                        "Project '" + name + "' has been updated successfully.");
            }

            // Navigate back to project list
            if (navigator != null) {
                navigator.navigateBack();
            } else {
                DialogUtil.showError("Navigation Error", "Navigator not set. Cannot navigate back.");
            }

        } catch (IllegalArgumentException e) {
            // Validation errors are safe to show directly
            DialogUtil.showError("Validation Error", e.getMessage());
        } catch (Exception e) {
            DialogUtil.showError("Save Error",
                    "An error occurred while saving the project: " + e.getMessage());
        }
    }


    @FXML
    private void handleCancel() {
        // Check if form has unsaved changes
        if (hasUnsavedChanges()) {
            String message = "You have unsaved changes. Are you sure you want to cancel? All changes will be lost.";

            if (!DialogUtil.showConfirmation("Unsaved Changes", message)) {
                return; // User chose not to cancel
            }
        }

        // Navigate back to project list
        if (navigator != null) {
            navigator.navigateBack();
        } else {
            DialogUtil.showError("Navigation Error", "Navigator not set. Cannot navigate back.");
        }
    }


    private boolean validateForm() {
        boolean isValid = true;

        // Validate project name
        if (nameField.getText().trim().isEmpty()) {
            showError(nameErrorLabel, "Project name is required");
            isValid = false;
        }

        // Validate disaster name
        if (disasterNameField.getText().trim().isEmpty()) {
            showError(disasterNameErrorLabel, "Disaster name is required");
            isValid = false;
        }

        // Validate keywords
        List<String> keywords = parseCommaSeparated(keywordsArea.getText());
        if (keywords.isEmpty()) {
            showError(keywordsErrorLabel, "At least one keyword is required");
            isValid = false;
        }

        // Validate dates (now optional, but if provided, must be valid)
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        // If both dates are provided, validate the range
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            showError(dateErrorLabel, "End date must be after start date");
            isValid = false;
        }

        // If only one date is provided, show warning
        // if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
        //     showError(dateErrorLabel, "Please provide both dates or leave both empty");
        //     isValid = false;
        // }

        // Validate platforms
        if (!isAnyPlatformSelected()) {
            showError(platformsErrorLabel, "At least one platform must be selected");
            isValid = false;
        }

        return isValid;
    }


    private List<String> parseCommaSeparated(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }


    private Set<Platform> getSelectedPlatforms() {
        Set<Platform> platforms = new HashSet<>();

        for (Map.Entry<Platform, CheckBox> entry : platformCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                platforms.add(entry.getKey());
            }
        }

        return platforms;
    }


    private boolean hasUnsavedChanges() {
        // If any field has content, consider it as having changes
        return !nameField.getText().trim().isEmpty() ||
                !disasterNameField.getText().trim().isEmpty() ||
                !keywordsArea.getText().trim().isEmpty() ||
                !hashtagsArea.getText().trim().isEmpty() ||
                startDatePicker.getValue() != null ||
                endDatePicker.getValue() != null ||
                isAnyPlatformSelected();
    }


    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }


    private void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }


    private void clearAllErrors() {
        hideError(nameErrorLabel);
        hideError(disasterNameErrorLabel);
        hideError(keywordsErrorLabel);
        hideError(dateErrorLabel);
        hideError(platformsErrorLabel);
    }


}
