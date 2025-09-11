package com.employeemanager.model;

/**
 * Figyelmeztetési szintek a munkavállalók munkaidő mintáihoz
 */
public enum WarningLevel {
    NONE("Nincs figyelmeztetés", ""),
    YELLOW("Figyelmeztető", "warning-yellow"),
    RED("Kritikus", "warning-red");
    
    private final String description;
    private final String styleClass;
    
    WarningLevel(String description, String styleClass) {
        this.description = description;
        this.styleClass = styleClass;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getStyleClass() {
        return styleClass;
    }
    
    /**
     * Visszaadja a magasabb prioritású figyelmeztetési szintet
     */
    public static WarningLevel getHigherPriority(WarningLevel level1, WarningLevel level2) {
        if (level1 == RED || level2 == RED) {
            return RED;
        }
        if (level1 == YELLOW || level2 == YELLOW) {
            return YELLOW;
        }
        return NONE;
    }
}