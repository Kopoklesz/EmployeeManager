package com.employeemanager.database.schema;

import javax.sql.DataSource;

import java.sql.*;

/**
 * Adatbázis séma inicializáló
 */
public interface SchemaInitializer {
    void initializeSchema(DataSource dataSource) throws SQLException;
    boolean isSchemaExists(DataSource dataSource) throws SQLException;
}