package com.disaster.analysis.ui.navigation;


public enum View {
    PROJECT_LIST("/fxml/project-list.fxml", "Projects"),
    PROJECT_FORM("/fxml/project-form.fxml", "New Project"),
    DATA_COLLECTION("/fxml/data-collection.fxml", "Data Collection"),
    ANALYSIS("/fxml/analysis.fxml", "Analysis");

    private final String fxmlPath;
    private final String title;


    View(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }


    public String getFxmlPath() {
        return fxmlPath;
    }


    public String getTitle() {
        return title;
    }
}
