package com.disaster.analysis.ui.controller;

import com.disaster.analysis.application.dto.ProjectDTO;
import com.disaster.analysis.config.ApplicationContext;
import com.disaster.analysis.domain.model.enums.Platform;
import com.disaster.analysis.ui.navigation.Navigator;
import com.disaster.analysis.ui.navigation.View;
import com.disaster.analysis.application.service.ProjectService;
import com.disaster.analysis.util.DialogUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


public class ProjectListController implements Initializable {

    @FXML
    private TableView<ProjectDTO> projectTable;

    @FXML
    private TableColumn<ProjectDTO, String> nameColumn;

    @FXML
    private TableColumn<ProjectDTO, String> disasterColumn;

    @FXML
    private TableColumn<ProjectDTO, String> dateColumn;

    @FXML
    private TableColumn<ProjectDTO, String> platformsColumn;

    @FXML
    private TableColumn<ProjectDTO, String> createdColumn;

    @FXML
    private TableColumn<ProjectDTO, String> modifiedColumn;

    @FXML
    private Button openButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Label projectCountLabel;

    private ProjectService projectService;
    private final ObservableList<ProjectDTO> projects;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private Navigator navigator;
    private ApplicationContext applicationContext;


    public ProjectListController() {
        this.projects = FXCollections.observableArrayList();
    }


    public ProjectListController(ProjectService projectService) {
        this.projectService = projectService;
        this.projects = FXCollections.observableArrayList();
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
        // Load projects after dependencies are injected
        loadProjects();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configure table columns with cell value factories
        setupTableColumns();

        // Bind the projects list to the table
        projectTable.setItems(projects);

        // Add selection listener to enable/disable buttons
        projectTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> updateButtonStates(newValue != null)
        );

        setupRowDoubleClick();

        // Note: loadProjects() is called in setApplicationContext() after dependencies are injected
    }

    private void setupRowDoubleClick() {
        projectTable.setRowFactory(tv -> {
            TableRow<ProjectDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    projectTable.getSelectionModel().select(row.getItem());
                    handleOpenProject();
                }
            });
            return row;
        });
    }


    private void setupTableColumns() {
        // Name column
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName())
        );

        // Disaster column
        disasterColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDisasterName())
        );

        // Date range column
        dateColumn.setCellValueFactory(cellData -> {
            ProjectDTO project = cellData.getValue();
            String dateRange;

            if (project.getStartDate() == null && project.getEndDate() == null) {
                dateRange = "Not set (will auto-update)";
            } else if (project.getStartDate() == null) {
                dateRange = "? to " + project.getEndDate().format(DATE_FORMATTER);
            } else if (project.getEndDate() == null) {
                dateRange = project.getStartDate().format(DATE_FORMATTER) + " to ?";
            } else {
                dateRange = String.format("%s to %s",
                        project.getStartDate().format(DATE_FORMATTER),
                        project.getEndDate().format(DATE_FORMATTER)
                );
            }

            return new SimpleStringProperty(dateRange);
        });

        // Platforms column
        platformsColumn.setCellValueFactory(cellData -> {
            String platformsStr = cellData.getValue().getPlatforms().stream()
                    .map(Platform::name)
                    .collect(java.util.stream.Collectors.joining(", "));
            return new SimpleStringProperty(platformsStr);
        });

        // Created column
        createdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getCreatedAt().format(DATE_FORMATTER)
                )
        );

        // Last modified column
        modifiedColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getLastModified().format(DATE_FORMATTER)
                )
        );
    }


    private void loadProjects() {
        try {
            projects.clear();
            projects.addAll(projectService.getAllProjects());
            updateProjectCount();
        } catch (Exception e) {
            DialogUtil.showError("Load Error",
                    "An error occurred while loading projects: " + e.getMessage());
        }
    }


    private void updateProjectCount() {
        int count = projects.size();
        projectCountLabel.setText(count + (count == 1 ? " project" : " projects"));
    }


    private void updateButtonStates(boolean hasSelection) {
        // openButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
    }


    @FXML
    private void handleNewProject() {
        if (navigator != null) {
            navigator.navigateTo(View.PROJECT_FORM);
        } else {
            DialogUtil.showError("Navigation Error", "Navigator not set. Cannot navigate to project form.");
        }
    }


    @FXML
    private void handleOpenProject() {
        ProjectDTO selectedProject = projectTable.getSelectionModel().getSelectedItem();

        if (selectedProject == null) {
            DialogUtil.showWarning("No Selection", "Please select a project to open.");
            return;
        }

        if (navigator == null || applicationContext == null) {
            DialogUtil.showError("Navigation Error", "Navigator or ApplicationContext not set. Cannot navigate.");
            return;
        }

        try {
            // Show dialog to choose between data collection and analysis
            Alert choiceDialog = new Alert(Alert.AlertType.CONFIRMATION);
            choiceDialog.setTitle("Open Project");
            choiceDialog.setHeaderText("Choose Action for: " + selectedProject.getName());
            choiceDialog.setContentText("What would you like to do with this project?");

            ButtonType dataCollectionButton = new ButtonType("Data Collection");
            ButtonType analysisButton = new ButtonType("Analysis");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            choiceDialog.getButtonTypes().setAll(dataCollectionButton, analysisButton, cancelButton);

            java.util.Optional<ButtonType> result = choiceDialog.showAndWait();

            if (result.isPresent()) {
                // Set the selected project in ApplicationContext
                applicationContext.setCurrentProject(selectedProject);

                if (result.get() == dataCollectionButton) {
                    // Navigate to data collection view
                    navigator.navigateTo(View.DATA_COLLECTION);
                } else if (result.get() == analysisButton) {
                    // Navigate to analysis view
                    navigator.navigateTo(View.ANALYSIS);
                }
            }

        } catch (Exception e) {
            DialogUtil.showError("Open Error",
                    "An error occurred while opening the project: " + e.getMessage());
        }
    }


    @FXML
    private void handleDeleteProject() {
        ProjectDTO selectedProject = projectTable.getSelectionModel().getSelectedItem();

        if (selectedProject == null) {
            DialogUtil.showWarning("No Selection", "Please select a project to delete.");
            return;
        }

        // Show confirmation dialog
        String confirmMessage = "Project: " + selectedProject.getName() + "\n" +
                "Disaster: " + selectedProject.getDisasterName() + "\n\n" +
                "This action cannot be undone. All collected data will be permanently deleted.";

        if (DialogUtil.showConfirmation("Delete Project", confirmMessage)) {
            try {
                projectService.deleteProject(selectedProject.getId());
                projects.remove(selectedProject);
                updateProjectCount();
                DialogUtil.showInformation("Project Deleted",
                        "Project '" + selectedProject.getName() + "' has been deleted successfully.");
            } catch (Exception e) {
                DialogUtil.showError("Delete Error",
                        "An error occurred while deleting the project: " + e.getMessage());
            }
        }
    }


    @FXML
    private void handleRefresh() {
        loadProjects();
    }
}