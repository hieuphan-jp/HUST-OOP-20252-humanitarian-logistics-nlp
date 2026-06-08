package com.disaster.analysis;

import com.disaster.analysis.infrastructure.persistence.DatabaseManager;
import com.disaster.analysis.util.LogUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;

public class Main extends Application {

    private static final String APP_TITLE = "Disaster Social Media Analysis";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 700;
    private static final String MAIN_FXML = "/fxml/main.fxml";
    private static final String STYLESHEET = "/css/styles.css";


    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            LogUtil.info("Starting application...");

            // Load main.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Parent root = loader.load();

            // Create scene
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
            //Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            // Configure primary stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Handle window close event to ensure proper shutdown
            primaryStage.setOnCloseRequest(event -> {
                shutdown();
            });

            // Set up global exception handler for uncaught exceptions
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                LogUtil.error("Uncaught exception in thread " + thread.getName(), throwable);
            });

            // Show the stage
            primaryStage.show();

            LogUtil.info("Application started successfully");
            System.out.println("Application started successfully");
            System.out.println("Log file location: " + LogUtil.getLogFilePath());

        } catch (Exception e) {
            LogUtil.error("Failed to start application", e);
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    @Override
    public void init() throws Exception {
        super.init();

        try {
            LogUtil.info("Initializing application...");

            // Initialize DatabaseManager
            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.initialize();

            LogUtil.info("Database initialized successfully");
            System.out.println("Database initialized successfully");

        } catch (SQLException e) {
            LogUtil.error("Failed to initialize database", e);
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Database initialization failed", e);
        }
    }


    @Override
    public void stop() throws Exception {
        shutdown();
        super.stop();
    }


    private void shutdown() {
        try {
            LogUtil.info("Shutting down application...");

            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.close();

            LogUtil.info("Application shutdown complete");
            System.out.println("Application shutdown complete");
        } catch (Exception e) {
            LogUtil.error("Error during shutdown", e);
            System.err.println("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        try {
            LogUtil.info("=== Disaster Social Media Analysis Application ===");
            LogUtil.info("Starting application with Java version: " + System.getProperty("java.version"));
            launch(args);
        } catch (Exception e) {
            LogUtil.error("Fatal error during application launch", e);
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
