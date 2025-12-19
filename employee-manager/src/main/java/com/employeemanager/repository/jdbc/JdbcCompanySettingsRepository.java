package com.employeemanager.repository.jdbc;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.repository.CompanySettingsRepository;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.ExecutionException;

/**
 * JDBC CompanySettings Repository implementáció
 *
 * Singleton pattern: mindig csak egy beállítás rekord létezik (id = "default")
 */
@Slf4j
public class JdbcCompanySettingsRepository implements CompanySettingsRepository {

    private static final String SETTINGS_ID = "default";

    private final DataSource dataSource;

    public JdbcCompanySettingsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public CompanySettings get() throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM company_settings WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, SETTINGS_ID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCompanySettings(rs);
                } else {
                    // Ha még nem létezik, létrehozzuk alapértelmezett értékekkel
                    log.info("CompanySettings not found, creating default");
                    return createDefault();
                }
            }

        } catch (SQLException e) {
            log.error("Failed to get company settings", e);
            throw new ExecutionException("Failed to get company settings", e);
        }
    }

    @Override
    public CompanySettings save(CompanySettings settings) throws ExecutionException, InterruptedException {
        // Biztosítjuk, hogy az ID mindig "default" legyen
        settings.setId(SETTINGS_ID);

        String checkSql = "SELECT COUNT(*) FROM company_settings WHERE id = ?";
        String insertSql = """
            INSERT INTO company_settings (
                id, company_name, company_address, company_zip_code, company_city,
                company_tax_number, company_eu_tax_number, company_bank_account, company_bank_name,
                company_email, company_phone, company_website, company_logo_path,
                nav_technical_user, nav_signature_key, nav_replacement_key, nav_test_mode,
                invoice_prefix, invoice_next_number, invoice_footer_text,
                default_payment_deadline_days, default_currency, default_vat_rate,
                invoicing_backend, szamlazz_agent_key, billingo_api_key,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;

        String updateSql = """
            UPDATE company_settings SET
                company_name = ?, company_address = ?, company_zip_code = ?, company_city = ?,
                company_tax_number = ?, company_eu_tax_number = ?, company_bank_account = ?, company_bank_name = ?,
                company_email = ?, company_phone = ?, company_website = ?, company_logo_path = ?,
                nav_technical_user = ?, nav_signature_key = ?, nav_replacement_key = ?, nav_test_mode = ?,
                invoice_prefix = ?, invoice_next_number = ?, invoice_footer_text = ?,
                default_payment_deadline_days = ?, default_currency = ?, default_vat_rate = ?,
                invoicing_backend = ?, szamlazz_agent_key = ?, billingo_api_key = ?,
                updated_at = NOW()
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection()) {

            // Ellenőrizzük, hogy létezik-e már
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, SETTINGS_ID);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        exists = rs.getInt(1) > 0;
                    }
                }
            }

            if (exists) {
                // UPDATE
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    setCompanySettingsParameters(stmt, settings, false);
                    stmt.setString(26, SETTINGS_ID);
                    stmt.executeUpdate();
                    log.info("CompanySettings updated");
                }
            } else {
                // INSERT
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, SETTINGS_ID);
                    setCompanySettingsParameters(stmt, settings, true);
                    stmt.executeUpdate();
                    log.info("CompanySettings created");
                }
            }

            return get();

        } catch (SQLException e) {
            log.error("Failed to save company settings", e);
            throw new ExecutionException("Failed to save company settings", e);
        }
    }

    @Override
    public synchronized String generateNextInvoiceNumber() throws ExecutionException, InterruptedException {
        String selectSql = "SELECT invoice_prefix, invoice_next_number FROM company_settings WHERE id = ? FOR UPDATE";
        String updateSql = "UPDATE company_settings SET invoice_next_number = invoice_next_number + 1 WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                String prefix;
                int nextNumber;

                // Lekérjük az aktuális értékeket (row lock)
                try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                    stmt.setString(1, SETTINGS_ID);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            prefix = rs.getString("invoice_prefix");
                            nextNumber = rs.getInt("invoice_next_number");
                        } else {
                            // Ha még nem létezik, létrehozzuk
                            createDefault();
                            prefix = "INV";
                            nextNumber = 1;
                        }
                    }
                }

                // Növeljük a számlaszám számlálót
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setString(1, SETTINGS_ID);
                    stmt.executeUpdate();
                }

                conn.commit();

                // Generáljuk a számlaszámot
                String invoiceNumber = String.format("%s-%04d", prefix, nextNumber);
                log.info("Generated invoice number: {}", invoiceNumber);

                return invoiceNumber;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            log.error("Failed to generate invoice number", e);
            throw new ExecutionException("Failed to generate invoice number", e);
        }
    }

    /**
     * Alapértelmezett CompanySettings létrehozása
     */
    private CompanySettings createDefault() throws ExecutionException, InterruptedException {
        CompanySettings settings = CompanySettings.builder()
            .id(SETTINGS_ID)
            .companyName("Cégem Kft.")
            .invoicePrefix("INV")
            .invoiceNextNumber(1)
            .defaultPaymentDeadlineDays(8)
            .defaultCurrency("HUF")
            .defaultVatRate(27.0)
            .invoicingBackend("NAV_EXPORT")
            .navTestMode(true)
            .build();

        return save(settings);
    }

    /**
     * ResultSet -> CompanySettings mapping
     */
    private CompanySettings mapResultSetToCompanySettings(ResultSet rs) throws SQLException {
        CompanySettings settings = new CompanySettings();
        settings.setId(rs.getString("id"));
        settings.setCompanyName(rs.getString("company_name"));
        settings.setCompanyAddress(rs.getString("company_address"));
        settings.setCompanyZipCode(rs.getString("company_zip_code"));
        settings.setCompanyCity(rs.getString("company_city"));
        settings.setCompanyTaxNumber(rs.getString("company_tax_number"));
        settings.setCompanyEUTaxNumber(rs.getString("company_eu_tax_number"));
        settings.setCompanyBankAccount(rs.getString("company_bank_account"));
        settings.setCompanyBankName(rs.getString("company_bank_name"));
        settings.setCompanyEmail(rs.getString("company_email"));
        settings.setCompanyPhone(rs.getString("company_phone"));
        settings.setCompanyWebsite(rs.getString("company_website"));
        settings.setCompanyLogoPath(rs.getString("company_logo_path"));

        settings.setNavTechnicalUser(rs.getString("nav_technical_user"));
        settings.setNavSignatureKey(rs.getString("nav_signature_key"));
        settings.setNavReplacementKey(rs.getString("nav_replacement_key"));
        settings.setNavTestMode(rs.getBoolean("nav_test_mode"));

        settings.setInvoicePrefix(rs.getString("invoice_prefix"));
        settings.setInvoiceNextNumber(rs.getInt("invoice_next_number"));
        settings.setInvoiceFooterText(rs.getString("invoice_footer_text"));
        settings.setDefaultPaymentDeadlineDays(rs.getInt("default_payment_deadline_days"));
        settings.setDefaultCurrency(rs.getString("default_currency"));
        settings.setDefaultVatRate(rs.getDouble("default_vat_rate"));

        settings.setInvoicingBackend(rs.getString("invoicing_backend"));
        settings.setSzamlazzAgentKey(rs.getString("szamlazz_agent_key"));
        settings.setBillingoApiKey(rs.getString("billingo_api_key"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            settings.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            settings.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return settings;
    }

    /**
     * PreparedStatement paraméterek beállítása
     */
    private void setCompanySettingsParameters(PreparedStatement stmt, CompanySettings settings, boolean isInsert) throws SQLException {
        int index = isInsert ? 2 : 1; // INSERT esetén az első paraméter az ID

        stmt.setString(index++, settings.getCompanyName());
        stmt.setString(index++, settings.getCompanyAddress());
        stmt.setString(index++, settings.getCompanyZipCode());
        stmt.setString(index++, settings.getCompanyCity());
        stmt.setString(index++, settings.getCompanyTaxNumber());
        stmt.setString(index++, settings.getCompanyEUTaxNumber());
        stmt.setString(index++, settings.getCompanyBankAccount());
        stmt.setString(index++, settings.getCompanyBankName());
        stmt.setString(index++, settings.getCompanyEmail());
        stmt.setString(index++, settings.getCompanyPhone());
        stmt.setString(index++, settings.getCompanyWebsite());
        stmt.setString(index++, settings.getCompanyLogoPath());

        stmt.setString(index++, settings.getNavTechnicalUser());
        stmt.setString(index++, settings.getNavSignatureKey());
        stmt.setString(index++, settings.getNavReplacementKey());
        stmt.setBoolean(index++, settings.getNavTestMode() != null ? settings.getNavTestMode() : true);

        stmt.setString(index++, settings.getInvoicePrefix());
        stmt.setInt(index++, settings.getInvoiceNextNumber());
        stmt.setString(index++, settings.getInvoiceFooterText());
        stmt.setInt(index++, settings.getDefaultPaymentDeadlineDays());
        stmt.setString(index++, settings.getDefaultCurrency());
        stmt.setDouble(index++, settings.getDefaultVatRate() != null ? settings.getDefaultVatRate() : 27.0);

        stmt.setString(index++, settings.getInvoicingBackend());
        stmt.setString(index++, settings.getSzamlazzAgentKey());
        stmt.setString(index++, settings.getBillingoApiKey());
    }
}
