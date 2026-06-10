package com.disaster.analysis.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;


public class DialogUtil {

    private DialogUtil() {
        // Private constructor to prevent instantiation
    }


    public static void showError(String title, String message) {
        LogUtil.error(title + ": " + message);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        boolean confirmed = result.isPresent() && result.get() == ButtonType.OK;

        LogUtil.info(title + ": " + message + " - User response: " + (confirmed ? "Confirmed" : "Cancelled"));
        return confirmed;
    }


    public static boolean showConfirmation(String title, String message,
                                           String confirmButtonText, String cancelButtonText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType confirmButton = new ButtonType(confirmButtonText);
        ButtonType cancelButton = new ButtonType(cancelButtonText);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        boolean confirmed = result.isPresent() && result.get() == confirmButton;

        LogUtil.info(title + ": " + message + " - User response: " + (confirmed ? "Confirmed" : "Cancelled"));
        return confirmed;
    }


    public static void showInformation(String title, String message) {
        LogUtil.info(title + ": " + message);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static void showWarning(String title, String message) {
        LogUtil.warn(title + ": " + message);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}