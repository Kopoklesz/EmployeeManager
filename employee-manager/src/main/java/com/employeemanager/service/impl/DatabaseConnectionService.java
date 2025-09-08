package com.employeemanager.service.impl;

import com.employeemanager.database.config.ConnectionConfig;
import com.employeemanager.database.config.DatabaseConnectionManager;
import com.employeemanager.database.config.DatabaseType;
import com.employeemanager.database.factory.RepositoryFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Adatbázis kapcsolatok kezelése és perzisztálása
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConnectionService {
    
    private static final String CONNECTIONS_FILE = "database-connections.json";
    private static final String ENCRYPTED_PREFIX = "ENC:";
    
    private final DatabaseConnectionManager connectionManager;
    private final RepositoryFactory repositoryFactory;
    private final ObjectMapper objectMapper;
    
    private final Map<String, ConnectionConfig> savedConnections = new LinkedHashMap<>();
    private String activeConnectionName;

    @PostConstruct
    public void init() {
        loadConnections();

        // Ha nincs mentett kapcsolat, létrehozunk egy alapértelmezett Firebase kapcsolatot
        if (savedConnections.isEmpty()) {
            createDefaultConnections();
        }

        // Ellenőrizzük, hogy az aktív kapcsolat még létezik-e
        if (activeConnectionName != null && savedConnections.containsKey(activeConnectionName)) {
            // A legutóbbi aktív kapcsolat még létezik - reaktiváljuk
            ConnectionConfig activeConfig = savedConnections.get(activeConnectionName);
            log.info("Restoring last active connection: {} ({})",
                    activeConnectionName, activeConfig.getType());

            // Kapcsolat tesztelése
            if (connectionManager.testConnection(activeConfig)) {
                connectionManager.setActiveConnection(activeConfig);
                repositoryFactory.switchConnection(activeConfig);
                activeConfig.setActive(true);
                log.info("Successfully restored active connection: {}", activeConnectionName);
            } else {
                log.warn("Last active connection failed test, will fallback: {}", activeConnectionName);
                activeConnectionName = null;
            }
        }

        // Ha nincs érvényes aktív kapcsolat, keresünk egyet
        if (activeConnectionName == null && !savedConnections.isEmpty()) {
            // Először próbáljuk a Firebase-t (ha létezik)
            ConnectionConfig firebaseConfig = savedConnections.values().stream()
                    .filter(config -> config.getType() == DatabaseType.FIREBASE)
                    .findFirst()
                    .orElse(null);

            if (firebaseConfig != null) {
                log.info("No active connection, falling back to Firebase: {}", firebaseConfig.getName());
                activateConnection(firebaseConfig.getName());
            } else {
                // Ha nincs Firebase, az első elérhető kapcsolat
                String firstConnection = savedConnections.keySet().iterator().next();
                log.info("No Firebase found, activating first available: {}", firstConnection);
                activateConnection(firstConnection);
            }
        }
    }
    
    @PreDestroy
    public void cleanup() {
        connectionManager.closeAll();
    }
    
    /**
     * Kapcsolatok betöltése fájlból
     */
    private void loadConnections() {
        Path connectionsPath = Paths.get(CONNECTIONS_FILE);
        
        if (!Files.exists(connectionsPath)) {
            log.info("No saved connections found");
            return;
        }
        
        try {
            ConnectionsData data = objectMapper.readValue(
                connectionsPath.toFile(), 
                ConnectionsData.class
            );
            
            if (data.getConnections() != null) {
                data.getConnections().forEach((name, config) -> {
                    // Jelszó dekódolása
                    if (config.getPassword() != null && config.getPassword().startsWith(ENCRYPTED_PREFIX)) {
                        config.setPassword(decrypt(config.getPassword()));
                    }
                    savedConnections.put(name, config);
                });
            }
            
            activeConnectionName = data.getActiveConnection();
            
            log.info("Loaded {} database connections", savedConnections.size());
            
        } catch (IOException e) {
            log.error("Failed to load database connections", e);
        }
    }
    
    /**
     * Kapcsolatok mentése fájlba
     */
    private void saveConnections() {
        try {
            ConnectionsData data = new ConnectionsData();
            
            // Jelszavak titkosítása mentés előtt
            Map<String, ConnectionConfig> connectionsToSave = new LinkedHashMap<>();
            savedConnections.forEach((name, config) -> {
                ConnectionConfig copy = copyConfig(config);
                if (copy.getPassword() != null && !copy.getPassword().isEmpty()) {
                    copy.setPassword(encrypt(copy.getPassword()));
                }
                connectionsToSave.put(name, copy);
            });
            
            data.setConnections(connectionsToSave);
            data.setActiveConnection(activeConnectionName);
            
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(CONNECTIONS_FILE), data);
            
            log.info("Saved {} database connections", savedConnections.size());
            
        } catch (IOException e) {
            log.error("Failed to save database connections", e);
        }
    }
    
    /**
     * Alapértelmezett kapcsolatok létrehozása
     */
    private void createDefaultConnections() {
        // Firebase kapcsolat (meglévő konfiguráció alapján)
        ConnectionConfig firebaseConfig = ConnectionConfig.builder()
            .name("Firebase (Default)")
            .type(DatabaseType.FIREBASE)
            .firebaseProjectId("employee-manager-e70b6")
            .firebaseDatabaseUrl("https://employee-manager-e70b6.firebaseio.com")
            .firebaseServiceAccountPath("classpath:service-account.json")
            .active(true)
            .build();
        
        savedConnections.put(firebaseConfig.getName(), firebaseConfig);

        /*
        
        // H2 tesztelési kapcsolat
        ConnectionConfig h2Config = ConnectionConfig.builder()
            .name("H2 In-Memory (Testing)")
            .type(DatabaseType.H2)
            .database("employeedb")
            .active(false)
            .build();
        
        savedConnections.put(h2Config.getName(), h2Config);

        */
        
        saveConnections();
    }
    
    /**
     * Új kapcsolat hozzáadása
     */
    public void addConnection(ConnectionConfig config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid connection configuration");
        }
        
        savedConnections.put(config.getName(), config);
        saveConnections();
        
        log.info("Added new database connection: {}", config.getName());
    }
    
    /**
     * Kapcsolat frissítése
     */
    public void updateConnection(String name, ConnectionConfig config) {
        if (!savedConnections.containsKey(name)) {
            throw new IllegalArgumentException("Connection not found: " + name);
        }
        
        savedConnections.put(name, config);
        
        // Ha az aktív kapcsolatot frissítettük, újra kell aktiválni
        if (name.equals(activeConnectionName)) {
            activateConnection(name);
        }
        
        saveConnections();
        log.info("Updated database connection: {}", name);
    }
    
    /**
     * Kapcsolat törlése
     */
    public void removeConnection(String name) {
        if (!savedConnections.containsKey(name)) {
            throw new IllegalArgumentException("Connection not found: " + name);
        }

        // Ha az aktív kapcsolatot töröljük
        if (name.equals(activeConnectionName)) {
            savedConnections.remove(name);

            if (!savedConnections.isEmpty()) {
                // Először próbáljuk a Firebase-t
                ConnectionConfig firebaseConfig = savedConnections.values().stream()
                        .filter(config -> config.getType() == DatabaseType.FIREBASE)
                        .findFirst()
                        .orElse(null);

                if (firebaseConfig != null) {
                    log.info("Active connection removed, switching to Firebase: {}", firebaseConfig.getName());
                    activateConnection(firebaseConfig.getName());
                } else {
                    // Ha nincs Firebase, az első elérhető
                    String newActive = savedConnections.keySet().iterator().next();
                    log.info("Active connection removed, switching to: {}", newActive);
                    activateConnection(newActive);
                }
            } else {
                activeConnectionName = null;
                log.info("No connections remaining after removal");
            }
        } else {
            // Nem aktív kapcsolat törlése
            savedConnections.remove(name);
        }

        saveConnections();
        log.info("Removed database connection: {}", name);
    }
    
    /**
     * Kapcsolat aktiválása
     */
    public boolean activateConnection(String name) {
        ConnectionConfig config = savedConnections.get(name);
        if (config == null) {
            log.error("Connection not found: {}", name);
            return false;
        }
        
        // Kapcsolat tesztelése
        if (!connectionManager.testConnection(config)) {
            log.error("Failed to activate connection: {}", name);
            return false;
        }
        
        // Aktiválás
        connectionManager.setActiveConnection(config);
        repositoryFactory.switchConnection(config);
        
        activeConnectionName = name;
        
        // Minden kapcsolatot inaktívra állítunk, kivéve az aktívat
        savedConnections.values().forEach(c -> c.setActive(false));
        config.setActive(true);
        
        saveConnections();
        
        log.info("Activated database connection: {} ({})", name, config.getType());
        return true;
    }
    
    /**
     * Kapcsolat tesztelése
     */
    public boolean testConnection(ConnectionConfig config) {
        return connectionManager.testConnection(config);
    }
    
    /**
     * Összes mentett kapcsolat lekérése
     */
    public List<ConnectionConfig> getAllConnections() {
        return new ArrayList<>(savedConnections.values());
    }
    
    /**
     * Kapcsolat lekérése név alapján
     */
    public Optional<ConnectionConfig> getConnection(String name) {
        return Optional.ofNullable(savedConnections.get(name));
    }
    
    /**
     * Aktív kapcsolat lekérése
     */
    public Optional<ConnectionConfig> getActiveConnection() {
        return Optional.ofNullable(savedConnections.get(activeConnectionName));
    }
    
    /**
     * Konfiguráció másolása (jelszó nélkül a biztonság érdekében)
     */
    private ConnectionConfig copyConfig(ConnectionConfig original) {
        return ConnectionConfig.builder()
            .name(original.getName())
            .type(original.getType())
            .host(original.getHost())
            .port(original.getPort())
            .database(original.getDatabase())
            .username(original.getUsername())
            .password(original.getPassword()) // Külön kezeljük
            .active(original.isActive())
            .firebaseProjectId(original.getFirebaseProjectId())
            .firebaseDatabaseUrl(original.getFirebaseDatabaseUrl())
            .firebaseServiceAccountPath(original.getFirebaseServiceAccountPath())
            .maxPoolSize(original.getMaxPoolSize())
            .minIdle(original.getMinIdle())
            .connectionTimeout(original.getConnectionTimeout())
            .build();
    }
    
    /**
     * Egyszerű jelszó titkosítás (Base64)
     * Produkciós környezetben használjon megfelelő titkosítást!
     */
    private String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(password.getBytes());
    }
    
    /**
     * Egyszerű jelszó dekódolás (Base64)
     */
    private String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || !encryptedPassword.startsWith(ENCRYPTED_PREFIX)) {
            return encryptedPassword;
        }
        String encoded = encryptedPassword.substring(ENCRYPTED_PREFIX.length());
        return new String(Base64.getDecoder().decode(encoded));
    }

    /**
     * Belső osztály a kapcsolatok JSON tárolásához
     */
    private static class ConnectionsData {
        private Map<String, ConnectionConfig> connections;
        private String activeConnection;
        
        public Map<String, ConnectionConfig> getConnections() {
            return connections;
        }
        
        public void setConnections(Map<String, ConnectionConfig> connections) {
            this.connections = connections;
        }
        
        public String getActiveConnection() {
            return activeConnection;
        }
        
        public void setActiveConnection(String activeConnection) {
            this.activeConnection = activeConnection;
        }
    }

    /**
     * Kapcsolat tesztelése részletes hibaüzenettel
     */
    public String testConnectionWithDetails(ConnectionConfig config) {
        try {
            boolean success = connectionManager.testConnection(config);
            if (success) {
                return "Sikeres kapcsolat";
            } else {
                return "Kapcsolódás sikertelen - ellenőrizze a logokat részletekért";
            }
        } catch (Exception e) {
            return "Hiba: " + e.getMessage();
        }
    }
}