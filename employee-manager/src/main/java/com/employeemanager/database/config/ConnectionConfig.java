package com.employeemanager.database.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Properties;

/**
 * Adatbázis kapcsolat konfigurációja
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionConfig {
    
    private String name;
    private DatabaseType type;
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;
    private boolean active;
    
    // Firebase specifikus
    private String firebaseProjectId;
    private String firebaseDatabaseUrl;
    private String firebaseServiceAccountPath;
    
    // Connection pool beállítások
    @Builder.Default
    private Integer maxPoolSize = 10;
    @Builder.Default
    private Integer minIdle = 2;
    @Builder.Default
    private Long connectionTimeout = 30000L; // 30 másodperc
    
    /**
     * JDBC URL generálása SQL adatbázisokhoz
     */
    public String getJdbcUrl() {
        switch (type) {
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true",
                        host, port, database);
            case POSTGRESQL:
                return String.format("jdbc:postgresql://%s:%d/%s",
                        host, port, database);
            case H2:
                return String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        database != null ? database : "testdb");
            default:
                return null;
        }
    }
    
    /**
     * JDBC Driver osztály neve
     */
    public String getDriverClassName() {
        switch (type) {
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case H2:
                return "org.h2.Driver";
            default:
                return null;
        }
    }
    
    /**
     * Kapcsolat tulajdonságok
     */
    public Properties getConnectionProperties() {
        Properties props = new Properties();
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        
        // Adatbázis specifikus tulajdonságok
        switch (type) {
            case MYSQL:
                props.setProperty("autoReconnect", "true");
                props.setProperty("useUnicode", "true");
                props.setProperty("characterEncoding", "UTF-8");
                break;
            case POSTGRESQL:
                props.setProperty("ApplicationName", "EmployeeManager");
                props.setProperty("reWriteBatchedInserts", "true");
                props.setProperty("sslmode", "disable"); // DOCKERHEZ
                break;
            default:
                break;
        }
        
        return props;
    }
    
    /**
     * Alapértelmezett port visszaadása típus alapján
     */
    public static Integer getDefaultPort(DatabaseType type) {
        switch (type) {
            case MYSQL:
                return 3306;
            case POSTGRESQL:
                return 5432;
            default:
                return null;
        }
    }
    
    /**
     * Validáció
     */
    public boolean isValid() {
        if (type == null || name == null || name.trim().isEmpty()) {
            return false;
        }
        
        switch (type) {
            case FIREBASE:
                return firebaseProjectId != null && !firebaseProjectId.trim().isEmpty() &&
                       firebaseDatabaseUrl != null && !firebaseDatabaseUrl.trim().isEmpty();
            case MYSQL:
            case POSTGRESQL:
                return host != null && !host.trim().isEmpty() &&
                       port != null && port > 0 &&
                       database != null && !database.trim().isEmpty();
            case H2:
                return true; // H2 mindig érvényes (in-memory)
            default:
                return false;
        }
    }
}