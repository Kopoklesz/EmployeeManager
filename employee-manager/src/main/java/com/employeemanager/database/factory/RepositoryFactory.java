package com.employeemanager.database.factory;

import com.employeemanager.database.config.ConnectionConfig;
import com.employeemanager.database.config.DatabaseConnectionManager;
import com.employeemanager.database.schema.SchemaInitializerFactory;
import com.employeemanager.repository.impl.FirebaseEmployeeRepository;
import com.employeemanager.repository.impl.FirebaseWorkRecordRepository;
import com.employeemanager.repository.impl.JdbcEmployeeRepository;
import com.employeemanager.repository.impl.JdbcWorkRecordRepository;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Repository példányok létrehozása az aktív adatbázis kapcsolat alapján
 *
 * Thread-safe implementáció: synchronized metódusok és volatile mezők biztosítják
 * a megfelelő szálbiztonságot többszálú környezetben.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepositoryFactory {

    private final DatabaseConnectionManager connectionManager;

    // Volatile mezők a láthatóság biztosítására több szál között
    private volatile EmployeeRepository currentEmployeeRepository;
    private volatile WorkRecordRepository currentWorkRecordRepository;
    private volatile String currentConnectionKey;
    
    /**
     * EmployeeRepository létrehozása vagy visszaadása
     */
    public synchronized EmployeeRepository getEmployeeRepository() {
        ConnectionConfig activeConfig = connectionManager.getActiveConnection();
        
        if (activeConfig == null) {
            throw new IllegalStateException("No active database connection configured");
        }
        
        String connectionKey = getConnectionKey(activeConfig);
        
        // Ha nincs még repository vagy változott a kapcsolat, újat hozunk létre
        if (currentEmployeeRepository == null || !connectionKey.equals(currentConnectionKey)) {
            currentEmployeeRepository = createEmployeeRepository(activeConfig);
            currentConnectionKey = connectionKey;
        }
        
        return currentEmployeeRepository;
    }
    
    /**
     * WorkRecordRepository létrehozása vagy visszaadása
     */
    public synchronized WorkRecordRepository getWorkRecordRepository() {
        ConnectionConfig activeConfig = connectionManager.getActiveConnection();
        
        if (activeConfig == null) {
            throw new IllegalStateException("No active database connection configured");
        }
        
        String connectionKey = getConnectionKey(activeConfig);
        
        // Ha nincs még repository vagy változott a kapcsolat, újat hozunk létre
        if (currentWorkRecordRepository == null || !connectionKey.equals(currentConnectionKey)) {
            currentWorkRecordRepository = createWorkRecordRepository(activeConfig);
            currentConnectionKey = connectionKey;
        }
        
        return currentWorkRecordRepository;
    }
    
    /**
     * EmployeeRepository létrehozása a konfiguráció alapján
     */
    private EmployeeRepository createEmployeeRepository(ConnectionConfig config) {
        log.info("Creating EmployeeRepository for database type: {}", config.getType());
        
        switch (config.getType()) {
            case FIREBASE:
                try {
                    Firestore firestore = connectionManager.getFirestore(config);
                    return new FirebaseEmployeeRepository(firestore);
                } catch (IOException e) {
                    log.error("Failed to create Firebase EmployeeRepository", e);
                    throw new RuntimeException("Failed to create Firebase repository", e);
                }
                
            case MYSQL:
            case POSTGRESQL:
            case H2:
                DataSource dataSource = connectionManager.getDataSource(config);
                
                // Séma inicializálása ha szükséges
                try {
                    SchemaInitializerFactory.initializeDatabase(dataSource, config.getType());
                } catch (Exception e) {
                    log.error("Failed to initialize database schema", e);
                    throw new RuntimeException("Failed to initialize database schema", e);
                }
                
                return new JdbcEmployeeRepository(dataSource);
                
            default:
                throw new IllegalArgumentException("Unsupported database type: " + config.getType());
        }
    }
    
    /**
     * WorkRecordRepository létrehozása a konfiguráció alapján
     */
    private WorkRecordRepository createWorkRecordRepository(ConnectionConfig config) {
        log.info("Creating WorkRecordRepository for database type: {}", config.getType());
        
        // Először szükségünk van az EmployeeRepository-ra
        EmployeeRepository employeeRepository = getEmployeeRepository();
        
        switch (config.getType()) {
            case FIREBASE:
                try {
                    Firestore firestore = connectionManager.getFirestore(config);
                    return new FirebaseWorkRecordRepository(firestore, employeeRepository);
                } catch (IOException e) {
                    log.error("Failed to create Firebase WorkRecordRepository", e);
                    throw new RuntimeException("Failed to create Firebase repository", e);
                }
                
            case MYSQL:
            case POSTGRESQL:
            case H2:
                DataSource dataSource = connectionManager.getDataSource(config);
                
                // Séma inicializálása ha szükséges
                try {
                    SchemaInitializerFactory.initializeDatabase(dataSource, config.getType());
                } catch (Exception e) {
                    log.error("Failed to initialize database schema", e);
                    throw new RuntimeException("Failed to initialize database schema", e);
                }
                
                return new JdbcWorkRecordRepository(dataSource, employeeRepository);
                
            default:
                throw new IllegalArgumentException("Unsupported database type: " + config.getType());
        }
    }
    
    /**
     * Kapcsolat kulcs generálása az egyedi azonosításhoz
     */
    private String getConnectionKey(ConnectionConfig config) {
        return config.getType() + ":" + config.getName() + ":" + config.hashCode();
    }
    
    /**
     * Repository cache tisztítása
     */
    public synchronized void clearCache() {
        currentEmployeeRepository = null;
        currentWorkRecordRepository = null;
        currentConnectionKey = null;
        log.info("Repository cache cleared");
    }

    /**
     * Aktív kapcsolat váltása
     */
    public synchronized void switchConnection(ConnectionConfig config) {
        // Beállítjuk az új aktív kapcsolatot
        connectionManager.setActiveConnection(config);

        // Töröljük a cache-t
        clearCache();

        // AZONNAL létrehozzuk az új repository-kat
        log.info("Creating new repositories for: {}", config.getType());
        currentEmployeeRepository = createEmployeeRepository(config);
        currentWorkRecordRepository = createWorkRecordRepository(config);
        currentConnectionKey = getConnectionKey(config);

        log.info("Switched to database connection: {} ({})",
                config.getName(), config.getType());
        log.info("New repositories created: EmployeeRepository={}, WorkRecordRepository={}",
                currentEmployeeRepository.getClass().getSimpleName(),
                currentWorkRecordRepository.getClass().getSimpleName());
    }
    
    /**
     * Ellenőrzi, hogy van-e aktív kapcsolat
     */
    public boolean hasActiveConnection() {
        return connectionManager.getActiveConnection() != null;
    }
    
    /**
     * Visszaadja az aktív kapcsolat konfigurációját
     */
    public ConnectionConfig getActiveConnectionConfig() {
        return connectionManager.getActiveConnection();
    }
    
    /**
     * Újrainicializálja a repository-kat az aktuális kapcsolattal
     */
    public synchronized void reinitialize() {
        clearCache();
        
        ConnectionConfig activeConfig = connectionManager.getActiveConnection();
        if (activeConfig != null) {
            log.info("Reinitializing repositories with connection: {}", activeConfig.getName());
            
            // Létrehozzuk az új repository-kat
            currentEmployeeRepository = createEmployeeRepository(activeConfig);
            currentWorkRecordRepository = createWorkRecordRepository(activeConfig);
            currentConnectionKey = getConnectionKey(activeConfig);
            
            log.info("Repositories reinitialized successfully");
        } else {
            log.warn("No active connection to reinitialize with");
        }
    }
}