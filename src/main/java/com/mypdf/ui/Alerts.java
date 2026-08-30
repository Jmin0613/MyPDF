package com.mypdf.ui;

import javafx.scene.control.Alert;
import javafx.stage.Window;

final class Alerts {
    private Alerts() {
    }

    static void info(Window owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, message);
    }

    static void error(Window owner, String message) {
        show(owner, Alert.AlertType.ERROR, "오류", message);
    }

    private static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
