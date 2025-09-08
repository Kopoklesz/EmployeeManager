package com.employeemanager.database.schema;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;

/**
 * H2 adatbázis séma inicializáló
 */
@Slf4j
public class H2SchemaInitializer implements SchemaInitializer {
    
    @Override
    public void initializeSchema(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            
            // H2 kompatibilitási mód beállítása
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET MODE MySQL");
            }
            
            // employees tábla - H2 szintaxis
            String createEmployeesTable = """
                CREATE TABLE IF NOT EXISTS employees (
                    id VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    birth_place VARCHAR(200),
                    birth_date DATE,
                    mother_name VARCHAR(200),
                    tax_number VARCHAR(10) UNIQUE,
                    social_security_number VARCHAR(9) UNIQUE,
                    address TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            // work_records tábla
            String createWorkRecordsTable = """
                CREATE TABLE IF NOT EXISTS work_records (
                    id VARCHAR(50) PRIMARY KEY,
                    employee_id VARCHAR(50) NOT NULL,
                    notification_date DATE NOT NULL,
                    notification_time TIME,
                    ebev_serial_number VARCHAR(100),
                    work_date DATE NOT NULL,
                    payment DECIMAL(12, 2) NOT NULL,
                    hours_worked INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
                )
                """;
            
            // database_info tábla
            String createInfoTable = """
                CREATE TABLE IF NOT EXISTS database_info (
                    property_key VARCHAR(50) PRIMARY KEY,
                    property_value VARCHAR(200),
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            // Indexek
            String[] createIndexes = {
                "CREATE INDEX IF NOT EXISTS idx_employees_tax_number ON employees(tax_number)",
                "CREATE INDEX IF NOT EXISTS idx_employees_ssn ON employees(social_security_number)",
                "CREATE INDEX IF NOT EXISTS idx_employees_name ON employees(name)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_employee_id ON work_records(employee_id)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_work_date ON work_records(work_date)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_notification_date ON work_records(notification_date)"
            };
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createEmployeesTable);
                log.info("Employees table created/verified");
                
                stmt.execute(createWorkRecordsTable);
                log.info("Work records table created/verified");
                
                stmt.execute(createInfoTable);
                log.info("Database info table created/verified");
                
                // Indexek létrehozása
                for (String createIndex : createIndexes) {
                    stmt.execute(createIndex);
                }
                log.info("Indexes created/verified");
                
                // Verzió információ beszúrása - H2 kompatibilis módon
                stmt.execute("""
                    MERGE INTO database_info (property_key, property_value)
                    KEY(property_key)
                    VALUES ('schema_version', '1.0')
                    """);
                
                log.info("H2 schema initialization completed");
            }
        }
    }
    
    @Override
    public boolean isSchemaExists(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // H2-ben a táblák nagybetűsek
            try (ResultSet rs = metaData.getTables(null, null, "EMPLOYEES", null)) {
                if (!rs.next()) {
                    // Próbáljuk kisbetűvel is
                    try (ResultSet rs2 = metaData.getTables(null, null, "employees", null)) {
                        if (!rs2.next()) {
                            return false;
                        }
                    }
                }
            }
            
            try (ResultSet rs = metaData.getTables(null, null, "WORK_RECORDS", null)) {
                if (!rs.next()) {
                    // Próbáljuk kisbetűvel is
                    try (ResultSet rs2 = metaData.getTables(null, null, "work_records", null)) {
                        return rs2.next();
                    }
                }
                return true;
            }
        }
    }
}