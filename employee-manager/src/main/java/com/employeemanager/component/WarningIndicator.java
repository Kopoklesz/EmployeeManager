package com.employeemanager.component;

import com.employeemanager.model.WarningLevel;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Figyelmeztetési indikátor komponens
 * Megjeleníti a figyelmeztetési szintet vizuálisan (felkiáltójel) és tooltip-pel
 */
public class WarningIndicator extends StackPane {
    
    private final Label warningLabel;
    private final Tooltip tooltip;
    private WarningLevel currentLevel = WarningLevel.NONE;
    
    public WarningIndicator() {
        // Felkiáltójel label
        warningLabel = new Label("!");
        warningLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        warningLabel.setVisible(false);
        
        // Tooltip beállítása
        tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.millis(100));
        tooltip.setHideDelay(Duration.millis(100));
        tooltip.setShowDuration(Duration.INDEFINITE);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        tooltip.getStyleClass().add("warning-tooltip");
        
        // Container beállítása
        setAlignment(Pos.CENTER);
        setPrefSize(30, 30);
        getChildren().add(warningLabel);
    }
    
    /**
     * Beállítja a figyelmeztetési szintet és üzenetet
     */
    public void setWarning(WarningLevel level, String message) {
        // Előző stílus eltávolítása
        if (currentLevel != WarningLevel.NONE) {
            warningLabel.getStyleClass().remove(currentLevel.getStyleClass());
            getStyleClass().remove(currentLevel.getStyleClass() + "-bg");
        }
        
        currentLevel = level;
        
        if (level == WarningLevel.NONE) {
            warningLabel.setVisible(false);
            Tooltip.uninstall(this, tooltip);
        } else {
            warningLabel.setVisible(true);
            warningLabel.getStyleClass().add(level.getStyleClass());
            getStyleClass().add(level.getStyleClass() + "-bg");
            
            // Tooltip beállítása
            if (message != null && !message.isEmpty()) {
                tooltip.setText(message);
                Tooltip.install(this, tooltip);
            }
        }
    }
    
    /**
     * Visszaadja az aktuális figyelmeztetési szintet
     */
    public WarningLevel getWarningLevel() {
        return currentLevel;
    }
    
    /**
     * Törli a figyelmeztetést
     */
    public void clearWarning() {
        setWarning(WarningLevel.NONE, null);
    }
}