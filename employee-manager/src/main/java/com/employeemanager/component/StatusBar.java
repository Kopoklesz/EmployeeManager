package com.employeemanager.component;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public class StatusBar extends HBox {
    private final Label label;

    public StatusBar() {
        this.label = new Label();
        this.label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);

        getChildren().add(label);

        setStyle("-fx-padding: 5; -fx-background-color: #f4f4f4; -fx-border-width: 1 0 0 0; -fx-border-color: #c8c8c8;");
    }

    public void setText(String text) {
        label.setText(text);
        label.setStyle("");  // Reset style
    }

    public String getText() {
        return label.getText();
    }

    /**
     * Sikeres művelet jelzése zöld háttérrel
     */
    public void showSuccess(String message) {
        label.setText("✓ " + message);
        label.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        
        // 3 másodperc után visszaállítjuk a normál stílust
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> label.setStyle(""));
        pause.play();
    }
    
    /**
     * Hiba jelzése piros háttérrel
     */
    public void showError(String message) {
        label.setText("✗ " + message);
        label.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        
        // 3 másodperc után visszaállítjuk a normál stílust
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> label.setStyle(""));
        pause.play();
    }
}