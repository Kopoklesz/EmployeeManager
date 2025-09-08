package com.employeemanager.database.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adatbázis kapcsolatok központi kezelője
 */
@Slf4j
@Component
public class DatabaseConnectionManager {

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();
    private final Map<String, Firestore> firestoreCache = new ConcurrentHashMap<>();
    private ConnectionConfig activeConnection;

    /**
     * Kapcsolat tesztelése
     */
    public boolean testConnection(ConnectionConfig config) {
        if (!config.isValid()) {
            log.error("Invalid connection configuration");
            return false;
        }

        switch (config.getType()) {
            case FIREBASE:
                return testFirebaseConnection(config);
            case MYSQL:
            case POSTGRESQL:
            case H2:
                return testJdbcConnection(config);
            default:
                return false;
        }
    }

    /**
     * JDBC kapcsolat tesztelése - JAVÍTOTT VERZIÓ HikariCP-vel
     */
    @SuppressWarnings("null")
    private boolean testJdbcConnection(ConnectionConfig config) {
        log.info("=== JDBC CONNECTION TEST WITH HIKARICP ===");
        log.info("Testing connection to: {}", config.getJdbcUrl());
        log.info("Database Type: {}", config.getType());
        log.info("Host: '{}'", config.getHost());
        log.info("Port: {}", config.getPort());
        log.info("Database: '{}'", config.getDatabase());
        log.info("Username: '{}'", config.getUsername());
        log.info("Password length: {}", config.getPassword() != null ? config.getPassword().length() : 0);
        log.info("=== END DEBUG INFO ===");

        DataSource testDataSource = null;
        try {
            // Ideiglenes DataSource létrehozása teszteléshez
            log.info("Creating temporary HikariCP DataSource for testing...");
            testDataSource = createDataSource(config);

            // Kapcsolat tesztelése a DataSource-on keresztül
            log.info("Attempting connection via HikariCP DataSource...");
            try (Connection conn = testDataSource.getConnection()) {
                boolean isValid = conn != null && !conn.isClosed();
                if (isValid) {
                    log.info("JDBC connection test successful via HikariCP");
                    log.info("Connection class: {}", conn.getClass().getName());
                    log.info("Connection URL: {}", conn.getMetaData().getURL());
                    log.info("Database product: {}", conn.getMetaData().getDatabaseProductName());
                    log.info("Database version: {}", conn.getMetaData().getDatabaseProductVersion());
                } else {
                    log.error("Connection is null or closed");
                }
                return isValid;
            }
        } catch (SQLException e) {
            log.error("Failed to test connection via HikariCP: {}", config.getJdbcUrl(), e);
            log.error("SQL Error Code: {}, SQL State: {}", e.getErrorCode(), e.getSQLState());
            log.error("SQL Exception message: {}", e.getMessage());

            // PostgreSQL specifikus debug információk
            if (config.getType() == DatabaseType.POSTGRESQL) {
                log.error("=== POSTGRESQL HIKARICP DEBUG ===");
                log.error("Username: '{}'", config.getUsername());
                log.error("Password length: {}", config.getPassword() != null ? config.getPassword().length() : 0);
                log.error("JDBC URL: {}", config.getJdbcUrl());
                log.error("=== END POSTGRESQL DEBUG ===");
            }

            return false;
        } catch (Exception e) {
            log.error("Unexpected error during connection test", e);
            return false;
        } finally {
            // Cleanup - DataSource bezárása
            if (testDataSource instanceof HikariDataSource) {
                log.info("Closing temporary HikariCP DataSource...");
                ((HikariDataSource) testDataSource).close();
            }
        }
    }

    /**
     * Firebase kapcsolat tesztelése
     */
    private boolean testFirebaseConnection(ConnectionConfig config) {
        try {
            // Service account betöltése
            Resource serviceAccount = loadFirebaseServiceAccount(config);
            if (!serviceAccount.exists()) {
                log.error("Firebase service account not found: {}",
                        config.getFirebaseServiceAccountPath());
                return false;
            }

            // Kapcsolat tesztelése
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            serviceAccount.getInputStream()))
                    .setProjectId(config.getFirebaseProjectId())
                    .setDatabaseUrl(config.getFirebaseDatabaseUrl())
                    .build();

            // Ideiglenes app létrehozása teszteléshez
            String appName = "test-" + System.currentTimeMillis();
            FirebaseApp testApp = FirebaseApp.initializeApp(options, appName);

            try {
                Firestore firestore = FirestoreClient.getFirestore(testApp);
                // Egyszerű teszt lekérdezés
                firestore.collection("test").limit(1).get().get();
                return true;
            } finally {
                testApp.delete();
            }

        } catch (Exception e) {
            log.error("Failed to connect to Firebase", e);
            return false;
        }
    }

    /**
     * DataSource létrehozása vagy cache-ből visszaadása
     */
    public DataSource getDataSource(ConnectionConfig config) {
        if (config.getType() == DatabaseType.FIREBASE) {
            throw new IllegalArgumentException("Use getFirestore() for Firebase connections");
        }

        String cacheKey = getCacheKey(config);
        return dataSourceCache.computeIfAbsent(cacheKey, key -> createDataSource(config));
    }

    /**
     * HikariCP DataSource létrehozása
     */
    private DataSource createDataSource(ConnectionConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        log.info("=== HIKARI DATASOURCE CREATION ===");
        log.info("Creating DataSource for: {}", config.getName());
        log.info("JDBC URL: {}", config.getJdbcUrl());
        log.info("Username: '{}'", config.getUsername());
        log.info("Password length: {}", config.getPassword() != null ? config.getPassword().length() : 0);
        log.info("Driver: {}", config.getDriverClassName());

        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(config.getDriverClassName());

        // Connection pool beállítások
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setPoolName("EmployeeManager-" + config.getName());

        // Adatbázis specifikus beállítások
        if (config.getType() == DatabaseType.MYSQL) {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            log.info("Added MySQL specific properties to HikariCP");
        }

        // PostgreSQL specifikus beállítások HikariCP-hez
        if (config.getType() == DatabaseType.POSTGRESQL) {
            hikariConfig.addDataSourceProperty("ApplicationName", "EmployeeManager");
            hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");
            hikariConfig.addDataSourceProperty("sslmode", "disable");
            log.info("Added PostgreSQL specific properties to HikariCP:");
            log.info("  - ApplicationName: EmployeeManager");
            log.info("  - reWriteBatchedInserts: true");
            log.info("  - sslmode: disable");
        }

        log.info("=== END HIKARI CREATION ===");

        try {
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            log.info("HikariDataSource created successfully for: {}", config.getName());
            return dataSource;
        } catch (Exception e) {
            log.error("Failed to create HikariDataSource for: {}", config.getName(), e);
            throw e;
        }
    }

    /**
     * Firestore instance létrehozása vagy cache-ből visszaadása
     */
    public Firestore getFirestore(ConnectionConfig config) throws IOException {
        if (config.getType() != DatabaseType.FIREBASE) {
            throw new IllegalArgumentException("Use getDataSource() for SQL connections");
        }

        String cacheKey = getCacheKey(config);
        return firestoreCache.computeIfAbsent(cacheKey, key -> {
            try {
                return createFirestore(config);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create Firestore", e);
            }
        });
    }

    /**
     * Firestore létrehozása
     */
    private Firestore createFirestore(ConnectionConfig config) throws IOException {
        Resource serviceAccount = loadFirebaseServiceAccount(config);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                .setProjectId(config.getFirebaseProjectId())
                .setDatabaseUrl(config.getFirebaseDatabaseUrl())
                .build();

        String appName = "app-" + config.getName();

        // Ha már létezik ilyen nevű app, töröljük
        try {
            FirebaseApp existingApp = FirebaseApp.getInstance(appName);
            existingApp.delete();
        } catch (IllegalStateException e) {
            // Nincs ilyen app, OK
        }

        FirebaseApp app = FirebaseApp.initializeApp(options, appName);
        log.info("Created Firebase app: {}", appName);

        return FirestoreClient.getFirestore(app);
    }

    /**
     * Firebase service account betöltése
     */
    private Resource loadFirebaseServiceAccount(ConnectionConfig config) {
        String path = config.getFirebaseServiceAccountPath();

        if (path.startsWith("classpath:")) {
            return new ClassPathResource(path.substring("classpath:".length()));
        } else {
            return new FileSystemResource(path);
        }
    }

    /**
     * Cache kulcs generálása
     */
    private String getCacheKey(ConnectionConfig config) {
        return config.getType() + ":" + config.getName();
    }

    /**
     * Aktív kapcsolat beállítása
     */
    public void setActiveConnection(ConnectionConfig config) {
        this.activeConnection = config;
        log.info("Active database connection set to: {} ({})",
                config.getName(), config.getType());
    }

    /**
     * Aktív kapcsolat lekérése
     */
    public ConnectionConfig getActiveConnection() {
        return activeConnection;
    }

    /**
     * Összes kapcsolat bezárása és cache tisztítása
     */
    public void closeAll() {
        // DataSource-ok bezárása
        dataSourceCache.values().forEach(ds -> {
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
        });
        dataSourceCache.clear();

        // Firebase app-ok törlése
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
        firestoreCache.clear();

        log.info("All database connections closed");
    }
}