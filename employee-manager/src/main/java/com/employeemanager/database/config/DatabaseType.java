package com.employeemanager.database.config;

/**
 * Támogatott adatbázis típusok
 */
public enum DatabaseType {
    FIREBASE("Firebase", "Firebase Realtime/Firestore Database"),
    MYSQL("MySQL", "MySQL Database"),
    POSTGRESQL("PostgreSQL", "PostgreSQL Database"),
    H2("H2", "H2 In-Memory Database (Testing)");

    private final String displayName;
    private final String description;

    DatabaseType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static DatabaseType fromDisplayName(String displayName) {
        for (DatabaseType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        return FIREBASE; // default
    }
}