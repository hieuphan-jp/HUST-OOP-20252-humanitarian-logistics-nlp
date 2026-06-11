package com.disaster.analysis.ui.navigation;

import com.disaster.analysis.config.ApplicationContext;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;


public class Navigator {

    private static final Logger logger = LoggerFactory.getLogger(Navigator.class);
    private static Navigator instance;

    private BorderPane contentArea;
    private ApplicationContext applicationContext;
    private NavigationHistory history;


    private Navigator() {
        this.history = new NavigationHistory();
    }


    public static synchronized Navigator getInstance() {
        if (instance == null) {
            instance = new Navigator();
        }
        return instance;
    }


    public void initialize(BorderPane contentArea, ApplicationContext context) {
        if (contentArea == null) {
            throw new IllegalArgumentException("Content area cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Application context cannot be null");
        }

        this.contentArea = contentArea;
        this.applicationContext = context;

        logger.info("Navigator initialized with content area and application context");
    }


    public void navigateTo(View view) {
        navigateTo(view, null);
    }


    public void navigateTo(View view, Object data) {
        if (contentArea == null || applicationContext == null) {
            throw new IllegalStateException("Navigator has not been initialized. Call initialize() first.");
        }

        if (view == null) {
            throw new IllegalArgumentException("View cannot be null");
        }

        try {
            logger.info("Navigating to view: {} ({})", view.getTitle(), view.getFxmlPath());

            // Load the view and get its controller
            Object controller = loadView(view.getFxmlPath());

            // Inject dependencies into the controller
            injectDependencies(controller);

            // Pass data to controller if provided
            if (data != null) {
                passDataToController(controller, data);
            }

            // Add current view to history (before navigating)
            history.push(view);

            logger.info("Successfully navigated to view: {}", view.getTitle());

        } catch (IOException e) {
            logger.error("Failed to load view: {} - {}", view.getFxmlPath(), e.getMessage(), e);
            throw new NavigationException("Failed to load view: " + view.getTitle(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during navigation to view: {} - {}", view.getTitle(), e.getMessage(), e);
            throw new NavigationException("Unexpected error during navigation to: " + view.getTitle(), e);
        }
    }


    public void navigateBack() {
        if (contentArea == null || applicationContext == null) {
            throw new IllegalStateException("Navigator has not been initialized. Call initialize() first.");
        }

        // Pop the current view
        View currentView = history.pop();

        if (history.canGoBack()) {
            // Get the previous view without removing it from history
            View previousView = history.peek();

            // Remove it from history since navigateTo will add it back
            history.pop();

            logger.info("Navigating back from {} to {}",
                    currentView != null ? currentView.getTitle() : "unknown",
                    previousView.getTitle());

            navigateTo(previousView);
        } else {
            logger.info("No navigation history available, cannot navigate back");
            // If no history, navigate to default view (PROJECT_LIST)
            navigateTo(View.PROJECT_LIST);
        }
    }


    private Object loadView(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

        // Load the FXML and set it as the center content
        contentArea.setCenter(loader.load());

        // Return the controller
        Object controller = loader.getController();

        if (controller == null) {
            logger.warn("No controller found for FXML: {}", fxmlPath);
        }

        return controller;
    }


    private void injectDependencies(Object controller) {
        if (controller == null) {
            return;
        }

        // Inject Navigator
        try {
            Method setNavigator = controller.getClass().getMethod("setNavigator", Navigator.class);
            setNavigator.invoke(controller, this);
            logger.debug("Injected Navigator into controller: {}", controller.getClass().getSimpleName());
        } catch (NoSuchMethodException e) {
            logger.debug("Controller {} does not have setNavigator method", controller.getClass().getSimpleName());
        } catch (Exception e) {
            logger.warn("Failed to inject Navigator into controller: {}", controller.getClass().getSimpleName(), e);
        }

        // Inject ApplicationContext
        try {
            Method setApplicationContext = controller.getClass().getMethod("setApplicationContext", ApplicationContext.class);
            setApplicationContext.invoke(controller, applicationContext);
            logger.debug("Injected ApplicationContext into controller: {}", controller.getClass().getSimpleName());
        } catch (NoSuchMethodException e) {
            logger.debug("Controller {} does not have setApplicationContext method", controller.getClass().getSimpleName());
        } catch (Exception e) {
            logger.warn("Failed to inject ApplicationContext into controller: {}", controller.getClass().getSimpleName(), e);
        }
    }


    private void passDataToController(Object controller, Object data) {
        if (controller == null || data == null) {
            return;
        }

        try {
            // Try to find a setData method that accepts the data type
            Method setData = controller.getClass().getMethod("setData", data.getClass());
            setData.invoke(controller, data);
            logger.debug("Passed data to controller: {}", controller.getClass().getSimpleName());
        } catch (NoSuchMethodException e) {
            logger.debug("Controller {} does not have setData method for type {}",
                    controller.getClass().getSimpleName(),
                    data.getClass().getSimpleName());
        } catch (Exception e) {
            logger.warn("Failed to pass data to controller: {}", controller.getClass().getSimpleName(), e);
        }
    }


    public NavigationHistory getHistory() {
        return history;
    }


    public static class NavigationException extends RuntimeException {
        public NavigationException(String message) {
            super(message);
        }

        public NavigationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}