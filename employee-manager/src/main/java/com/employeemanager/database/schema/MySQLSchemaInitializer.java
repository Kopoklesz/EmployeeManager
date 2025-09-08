package com.employeemanager.database.schema;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;

/**
 * MySQL séma inicializáló
 */
@Slf4j
public class MySQLSchemaInitializer implements SchemaInitializer {
    
    @Override
    public void initializeSchema(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            
            // employees tábla
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
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_tax_number (tax_number),
                    INDEX idx_ssn (social_security_number),
                    INDEX idx_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
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
                    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
                    INDEX idx_employee_id (employee_id),
                    INDEX idx_work_date (work_date),
                    INDEX idx_notification_date (notification_date)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            // database_info tábla (verziókövetéshez)
            String createInfoTable = """
                CREATE TABLE IF NOT EXISTS database_info (
                    property_key VARCHAR(50) PRIMARY KEY,
                    property_value VARCHAR(200),
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createEmployeesTable);
                log.info("Employees table created/verified");
                
                stmt.execute(createWorkRecordsTable);
                log.info("Work records table created/verified");
                
                stmt.execute(createInfoTable);
                log.info("Database info table created/verified");
                
                // Verzió információ beszúrása
                stmt.execute("""
                    INSERT INTO database_info (property_key, property_value)
                    VALUES ('schema_version', '1.0')
                    ON DUPLICATE KEY UPDATE property_value = '1.0'
                    """);
                
                log.info("MySQL schema initialization completed");
            }
        }
    }

    @Override
    public boolean isSchemaExists(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // MySQL specifikus: katalógus (adatbázis név) lekérése
            String catalog = conn.getCatalog();
            log.debug("Checking schema in catalog: {}", catalog);

            // Próbáljuk különböző formátumokban
            String[] tableVariants = {"employees", "EMPLOYEES"};
            boolean employeesExists = false;
            boolean workRecordsExists = false;

            // employees tábla ellenőrzése
            for (String tableName : tableVariants) {
                try (ResultSet rs = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        log.debug("Found employees table as: {}", tableName);
                        employeesExists = true;
                        break;
                    }
                }
            }

            if (!employeesExists) {
                log.debug("employees table not found");
                return false;
            }

            // work_records tábla ellenőrzése
            String[] workRecordVariants = {"work_records", "WORK_RECORDS"};
            for (String tableName : workRecordVariants) {
                try (ResultSet rs = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        log.debug("Found work_records table as: {}", tableName);
                        workRecordsExists = true;
                        break;
                    }
                }
            }

            log.debug("Schema check result - employees: {}, work_records: {}", employeesExists, workRecordsExists);
            return employeesExists && workRecordsExists;
        }
    }
}