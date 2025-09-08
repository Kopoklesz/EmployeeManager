package com.employeemanager.database.schema;

import com.employeemanager.database.config.DatabaseType;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Séma inicializáló factory
 */
@Slf4j
public class SchemaInitializerFactory {
    
    /**
     * Megfelelő inicializáló visszaadása adatbázis típus alapján
     */
    public static SchemaInitializer getInitializer(DatabaseType type) {
        switch (type) {
            case MYSQL:
                return new MySQLSchemaInitializer();
            case POSTGRESQL:
                return new PostgreSQLSchemaInitializer();
            case H2:
                return new H2SchemaInitializer();
            default:
                throw new IllegalArgumentException("No schema initializer for: " + type);
        }
    }
    
    /**
     * Séma inicializálása és adatok betöltése
     */
    public static void initializeDatabase(DataSource dataSource, DatabaseType type) {
        try {
            SchemaInitializer initializer = getInitializer(type);
            
            boolean schemaExists = initializer.isSchemaExists(dataSource);
            log.info("Schema exists: {}", schemaExists);
            
            if (!schemaExists) {
                log.info("Initializing database schema for: {}", type);
                initializer.initializeSchema(dataSource);
                log.info("Database schema initialized successfully");
            } else {
                log.info("Database schema already exists, skipping initialization");
            }
            
        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}