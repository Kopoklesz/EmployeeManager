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

            // company_settings tábla
            String createCompanySettingsTable = """
                CREATE TABLE IF NOT EXISTS company_settings (
                    id VARCHAR(50) PRIMARY KEY,
                    company_name VARCHAR(255) NOT NULL,
                    company_address VARCHAR(500),
                    company_zip_code VARCHAR(10),
                    company_city VARCHAR(100),
                    company_tax_number VARCHAR(50) UNIQUE,
                    company_eu_tax_number VARCHAR(50),
                    company_bank_account VARCHAR(50),
                    company_bank_name VARCHAR(100),
                    company_email VARCHAR(100),
                    company_phone VARCHAR(30),
                    company_website VARCHAR(255),
                    company_logo_path VARCHAR(500),
                    nav_technical_user VARCHAR(100),
                    nav_signature_key VARCHAR(255),
                    nav_replacement_key VARCHAR(255),
                    nav_test_mode BOOLEAN DEFAULT TRUE,
                    invoice_prefix VARCHAR(10) DEFAULT 'INV',
                    invoice_next_number INT DEFAULT 1,
                    invoice_footer_text TEXT,
                    default_payment_deadline_days INT DEFAULT 8,
                    default_currency VARCHAR(3) DEFAULT 'HUF',
                    default_vat_rate DECIMAL(5,2) DEFAULT 27.00,
                    invoicing_backend VARCHAR(50) DEFAULT 'NAV_EXPORT',
                    szamlazz_agent_key VARCHAR(255),
                    billingo_api_key VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            // customers tábla
            String createCustomersTable = """
                CREATE TABLE IF NOT EXISTS customers (
                    id VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    tax_number VARCHAR(50),
                    eu_tax_number VARCHAR(50),
                    zip_code VARCHAR(10),
                    city VARCHAR(100),
                    address VARCHAR(500),
                    country VARCHAR(100) DEFAULT 'Magyarország',
                    email VARCHAR(100),
                    phone VARCHAR(30),
                    contact_person VARCHAR(100),
                    billing_address VARCHAR(500),
                    billing_zip_code VARCHAR(10),
                    billing_city VARCHAR(100),
                    billing_country VARCHAR(100),
                    payment_deadline_days INT DEFAULT 8,
                    notes TEXT,
                    is_active BOOLEAN DEFAULT TRUE,
                    is_company BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            // invoices tábla
            String createInvoicesTable = """
                CREATE TABLE IF NOT EXISTS invoices (
                    id VARCHAR(50) PRIMARY KEY,
                    invoice_number VARCHAR(50) UNIQUE NOT NULL,
                    customer_id VARCHAR(50) NOT NULL,
                    invoice_date DATE NOT NULL,
                    payment_deadline DATE,
                    delivery_date DATE,
                    payment_date DATE,
                    payment_method VARCHAR(50) DEFAULT 'Átutalás',
                    currency VARCHAR(3) DEFAULT 'HUF',
                    exchange_rate DECIMAL(12,6) DEFAULT 1.000000,
                    net_amount DECIMAL(12,2) NOT NULL,
                    vat_amount DECIMAL(12,2) NOT NULL,
                    gross_amount DECIMAL(12,2) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                    is_paid BOOLEAN DEFAULT FALSE,
                    is_sent_to_nav BOOLEAN DEFAULT FALSE,
                    nav_transaction_id VARCHAR(100),
                    nav_sent_at TIMESTAMP,
                    is_reverse_charge BOOLEAN DEFAULT FALSE,
                    is_cash_accounting BOOLEAN DEFAULT FALSE,
                    footer_text TEXT,
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT
                )
                """;

            // invoice_items tábla
            String createInvoiceItemsTable = """
                CREATE TABLE IF NOT EXISTS invoice_items (
                    id VARCHAR(50) PRIMARY KEY,
                    invoice_id VARCHAR(50) NOT NULL,
                    line_number INT NOT NULL,
                    description VARCHAR(500) NOT NULL,
                    unit_of_measure VARCHAR(20) DEFAULT 'db',
                    quantity DECIMAL(12,4) NOT NULL DEFAULT 1.0000,
                    unit_price DECIMAL(12,2) NOT NULL,
                    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 27.00,
                    net_amount DECIMAL(12,2) NOT NULL,
                    vat_amount DECIMAL(12,2) NOT NULL,
                    gross_amount DECIMAL(12,2) NOT NULL,
                    discount_percent DECIMAL(5,2) DEFAULT 0.00,
                    discount_amount DECIMAL(12,2) DEFAULT 0.00,
                    vat_exemption_reason VARCHAR(255),
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
                )
                """;

            // Indexek
            String[] createIndexes = {
                "CREATE INDEX IF NOT EXISTS idx_employees_tax_number ON employees(tax_number)",
                "CREATE INDEX IF NOT EXISTS idx_employees_ssn ON employees(social_security_number)",
                "CREATE INDEX IF NOT EXISTS idx_employees_name ON employees(name)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_employee_id ON work_records(employee_id)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_work_date ON work_records(work_date)",
                "CREATE INDEX IF NOT EXISTS idx_work_records_notification_date ON work_records(notification_date)",
                "CREATE INDEX IF NOT EXISTS idx_customers_tax_number ON customers(tax_number)",
                "CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name)",
                "CREATE INDEX IF NOT EXISTS idx_customers_active ON customers(is_active)",
                "CREATE INDEX IF NOT EXISTS idx_invoices_number ON invoices(invoice_number)",
                "CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices(customer_id)",
                "CREATE INDEX IF NOT EXISTS idx_invoices_date ON invoices(invoice_date)",
                "CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status)",
                "CREATE INDEX IF NOT EXISTS idx_invoices_paid ON invoices(is_paid)",
                "CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id)",
                "CREATE INDEX IF NOT EXISTS idx_invoice_items_line ON invoice_items(invoice_id, line_number)"
            };

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createEmployeesTable);
                log.info("Employees table created/verified");

                stmt.execute(createWorkRecordsTable);
                log.info("Work records table created/verified");

                stmt.execute(createInfoTable);
                log.info("Database info table created/verified");

                // Számlázó táblák létrehozása
                stmt.execute(createCompanySettingsTable);
                log.info("Company settings table created/verified");

                stmt.execute(createCustomersTable);
                log.info("Customers table created/verified");

                stmt.execute(createInvoicesTable);
                log.info("Invoices table created/verified");

                stmt.execute(createInvoiceItemsTable);
                log.info("Invoice items table created/verified");

                // Indexek létrehozása
                for (String createIndex : createIndexes) {
                    stmt.execute(createIndex);
                }
                log.info("Indexes created/verified");

                // Verzió információ beszúrása - H2 kompatibilis módon
                stmt.execute("""
                    MERGE INTO database_info (property_key, property_value)
                    KEY(property_key)
                    VALUES ('schema_version', '2.0')
                    """);

                log.info("H2 schema initialization completed (version 2.0 with invoicing)");
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