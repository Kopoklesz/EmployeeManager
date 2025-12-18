package com.employeemanager.repository.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.repository.interfaces.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * JDBC alapú CompanySettings repository
 * Singleton pattern - csak 1 beállítás létezik
 */
@Slf4j
@Repository("jdbcCompanySettingsRepository")
@RequiredArgsConstructor
public class JdbcCompanySettingsRepository implements CompanySettingsRepository {

    private static final String SETTINGS_ID = "default";
    protected final DataSource dataSource;

    @Override
    public CompanySettings get() throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM company_settings WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, SETTINGS_ID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSettings(rs);
                } else {
                    // Ha nem létezik, alapértelmezettet hozunk létre
                    return createDefaultSettings();
                }
            }

        } catch (SQLException e) {
            log.error("Error getting company settings", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public CompanySettings save(CompanySettings settings) throws ExecutionException, InterruptedException {
        if (exists()) {
            return update(settings);
        } else {
            return insert(settings);
        }
    }

    private CompanySettings insert(CompanySettings settings) throws ExecutionException {
        String sql = """
            INSERT INTO company_settings
            (id, company_name, company_address, company_zip_code, company_city,
             company_tax_number, company_eu_tax_number, company_bank_account,
             company_bank_name, company_email, company_phone, company_website,
             company_logo_path, nav_technical_user, nav_signature_key,
             nav_replacement_key, nav_test_mode, invoice_prefix, invoice_next_number,
             invoice_footer_text, default_payment_deadline_days, default_currency,
             default_vat_rate, invoicing_backend, szamlazz_agent_key, billingo_api_key,
             created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            settings.setId(SETTINGS_ID);
            setSettingsParameters(ps, settings);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(27, now);
            ps.setTimestamp(28, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Inserted company settings");
                return settings;
            } else {
                throw new ExecutionException("Failed to insert company settings", null);
            }

        } catch (SQLException e) {
            log.error("Error inserting company settings", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private CompanySettings update(CompanySettings settings) throws ExecutionException {
        String sql = """
            UPDATE company_settings SET
            company_name = ?, company_address = ?, company_zip_code = ?, company_city = ?,
            company_tax_number = ?, company_eu_tax_number = ?, company_bank_account = ?,
            company_bank_name = ?, company_email = ?, company_phone = ?, company_website = ?,
            company_logo_path = ?, nav_technical_user = ?, nav_signature_key = ?,
            nav_replacement_key = ?, nav_test_mode = ?, invoice_prefix = ?, invoice_next_number = ?,
            invoice_footer_text = ?, default_payment_deadline_days = ?, default_currency = ?,
            default_vat_rate = ?, invoicing_backend = ?, szamlazz_agent_key = ?,
            billingo_api_key = ?, updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, settings.getCompanyName());
            ps.setString(2, settings.getCompanyAddress());
            ps.setString(3, settings.getCompanyZipCode());
            ps.setString(4, settings.getCompanyCity());
            ps.setString(5, settings.getCompanyTaxNumber());
            ps.setString(6, settings.getCompanyEUTaxNumber());
            ps.setString(7, settings.getCompanyBankAccount());
            ps.setString(8, settings.getCompanyBankName());
            ps.setString(9, settings.getCompanyEmail());
            ps.setString(10, settings.getCompanyPhone());
            ps.setString(11, settings.getCompanyWebsite());
            ps.setString(12, settings.getCompanyLogoPath());
            ps.setString(13, settings.getNavTechnicalUser());
            ps.setString(14, settings.getNavSignatureKey());
            ps.setString(15, settings.getNavReplacementKey());
            ps.setBoolean(16, settings.getNavTestMode() != null ? settings.getNavTestMode() : true);
            ps.setString(17, settings.getInvoicePrefix());
            ps.setInt(18, settings.getInvoiceNextNumber() != null ? settings.getInvoiceNextNumber() : 1);
            ps.setString(19, settings.getInvoiceFooterText());
            ps.setInt(20, settings.getDefaultPaymentDeadlineDays() != null ? settings.getDefaultPaymentDeadlineDays() : 8);
            ps.setString(21, settings.getDefaultCurrency());
            ps.setDouble(22, settings.getDefaultVatRate() != null ? settings.getDefaultVatRate() : 27.0);
            ps.setString(23, settings.getInvoicingBackend());
            ps.setString(24, settings.getSzamlazzAgentKey());
            ps.setString(25, settings.getBillingoApiKey());
            ps.setTimestamp(26, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(27, SETTINGS_ID);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Updated company settings");
                return settings;
            } else {
                throw new ExecutionException("Company settings not found for update", null);
            }

        } catch (SQLException e) {
            log.error("Error updating company settings", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private void setSettingsParameters(PreparedStatement ps, CompanySettings settings) throws SQLException {
        ps.setString(1, settings.getId());
        ps.setString(2, settings.getCompanyName());
        ps.setString(3, settings.getCompanyAddress());
        ps.setString(4, settings.getCompanyZipCode());
        ps.setString(5, settings.getCompanyCity());
        ps.setString(6, settings.getCompanyTaxNumber());
        ps.setString(7, settings.getCompanyEUTaxNumber());
        ps.setString(8, settings.getCompanyBankAccount());
        ps.setString(9, settings.getCompanyBankName());
        ps.setString(10, settings.getCompanyEmail());
        ps.setString(11, settings.getCompanyPhone());
        ps.setString(12, settings.getCompanyWebsite());
        ps.setString(13, settings.getCompanyLogoPath());
        ps.setString(14, settings.getNavTechnicalUser());
        ps.setString(15, settings.getNavSignatureKey());
        ps.setString(16, settings.getNavReplacementKey());
        ps.setBoolean(17, settings.getNavTestMode() != null ? settings.getNavTestMode() : true);
        ps.setString(18, settings.getInvoicePrefix());
        ps.setInt(19, settings.getInvoiceNextNumber() != null ? settings.getInvoiceNextNumber() : 1);
        ps.setString(20, settings.getInvoiceFooterText());
        ps.setInt(21, settings.getDefaultPaymentDeadlineDays() != null ? settings.getDefaultPaymentDeadlineDays() : 8);
        ps.setString(22, settings.getDefaultCurrency());
        ps.setDouble(23, settings.getDefaultVatRate() != null ? settings.getDefaultVatRate() : 27.0);
        ps.setString(24, settings.getInvoicingBackend());
        ps.setString(25, settings.getSzamlazzAgentKey());
        ps.setString(26, settings.getBillingoApiKey());
    }

    @Override
    public boolean exists() throws ExecutionException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM company_settings WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, SETTINGS_ID);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }

        } catch (SQLException e) {
            log.error("Error checking if company settings exist", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public String generateNextInvoiceNumber() throws ExecutionException, InterruptedException {
        CompanySettings settings = get();
        String invoiceNumber = settings.generateNextInvoiceNumber();
        save(settings); // Mentjük a frissített számlaszám számlálót
        return invoiceNumber;
    }

    private CompanySettings createDefaultSettings() throws ExecutionException {
        CompanySettings settings = CompanySettings.builder()
                .id(SETTINGS_ID)
                .companyName("Cég Neve")
                .invoicePrefix("INV")
                .invoiceNextNumber(1)
                .defaultPaymentDeadlineDays(8)
                .defaultCurrency("HUF")
                .defaultVatRate(27.0)
                .navTestMode(true)
                .invoicingBackend("NAV_EXPORT")
                .build();

        return insert(settings);
    }

    private CompanySettings mapResultSetToSettings(ResultSet rs) throws SQLException {
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
}
