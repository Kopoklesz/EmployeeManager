package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Vállalati beállítások és adatok
 */
@Entity
@Table(name = "company_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySettings {

    @Id
    private String id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_address")
    private String companyAddress;

    @Column(name = "company_zip_code")
    private String companyZipCode;

    @Column(name = "company_city")
    private String companyCity;

    @Column(name = "company_tax_number", unique = true)
    private String companyTaxNumber;

    @Column(name = "company_eu_tax_number")
    private String companyEUTaxNumber;

    @Column(name = "company_bank_account")
    private String companyBankAccount;

    @Column(name = "company_bank_name")
    private String companyBankName;

    @Column(name = "company_email")
    private String companyEmail;

    @Column(name = "company_phone")
    private String companyPhone;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "company_logo_path")
    private String companyLogoPath;

    // NAV Online Invoice beállítások
    @Column(name = "nav_technical_user")
    private String navTechnicalUser;

    @Column(name = "nav_signature_key")
    private String navSignatureKey;

    @Column(name = "nav_replacement_key")
    private String navReplacementKey;

    @Column(name = "nav_test_mode")
    @Builder.Default
    private Boolean navTestMode = true;

    // Számla beállítások
    @Column(name = "invoice_prefix")
    @Builder.Default
    private String invoicePrefix = "INV";

    @Column(name = "invoice_next_number")
    @Builder.Default
    private Integer invoiceNextNumber = 1;

    @Column(name = "invoice_footer_text", length = 1000)
    private String invoiceFooterText;

    @Column(name = "default_payment_deadline_days")
    @Builder.Default
    private Integer defaultPaymentDeadlineDays = 8;

    @Column(name = "default_currency")
    @Builder.Default
    private String defaultCurrency = "HUF";

    // Alapértelmezett ÁFA kulcsok
    @Column(name = "default_vat_rate")
    @Builder.Default
    private Double defaultVatRate = 27.0;

    @Transient
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Teljes cégnév adószámmal
     */
    public String getFullCompanyName() {
        if (companyTaxNumber != null && !companyTaxNumber.isEmpty()) {
            return companyName + " (Adószám: " + companyTaxNumber + ")";
        }
        return companyName;
    }

    /**
     * Teljes cím
     */
    public String getFullAddress() {
        return String.format("%s %s, %s", companyZipCode, companyCity, companyAddress);
    }

    /**
     * Következő számlaszám generálása
     */
    public String generateNextInvoiceNumber() {
        String invoiceNumber = String.format("%s-%04d", invoicePrefix, invoiceNextNumber);
        invoiceNextNumber++;
        return invoiceNumber;
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("companyName", companyName);
        map.put("companyAddress", companyAddress);
        map.put("companyZipCode", companyZipCode);
        map.put("companyCity", companyCity);
        map.put("companyTaxNumber", companyTaxNumber);
        map.put("companyEUTaxNumber", companyEUTaxNumber);
        map.put("companyBankAccount", companyBankAccount);
        map.put("companyBankName", companyBankName);
        map.put("companyEmail", companyEmail);
        map.put("companyPhone", companyPhone);
        map.put("companyWebsite", companyWebsite);
        map.put("companyLogoPath", companyLogoPath);
        map.put("navTechnicalUser", navTechnicalUser);
        map.put("navSignatureKey", navSignatureKey);
        map.put("navReplacementKey", navReplacementKey);
        map.put("navTestMode", navTestMode);
        map.put("invoicePrefix", invoicePrefix);
        map.put("invoiceNextNumber", invoiceNextNumber);
        map.put("invoiceFooterText", invoiceFooterText);
        map.put("defaultPaymentDeadlineDays", defaultPaymentDeadlineDays);
        map.put("defaultCurrency", defaultCurrency);
        map.put("defaultVatRate", defaultVatRate);
        map.put("createdAt", FirebaseDateConverter.dateTimeToString(createdAt));
        map.put("updatedAt", FirebaseDateConverter.dateTimeToString(updatedAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál
     */
    public static CompanySettings fromMap(Map<String, Object> map) {
        CompanySettings settings = new CompanySettings();
        settings.setId((String) map.get("id"));
        settings.setCompanyName((String) map.get("companyName"));
        settings.setCompanyAddress((String) map.get("companyAddress"));
        settings.setCompanyZipCode((String) map.get("companyZipCode"));
        settings.setCompanyCity((String) map.get("companyCity"));
        settings.setCompanyTaxNumber((String) map.get("companyTaxNumber"));
        settings.setCompanyEUTaxNumber((String) map.get("companyEUTaxNumber"));
        settings.setCompanyBankAccount((String) map.get("companyBankAccount"));
        settings.setCompanyBankName((String) map.get("companyBankName"));
        settings.setCompanyEmail((String) map.get("companyEmail"));
        settings.setCompanyPhone((String) map.get("companyPhone"));
        settings.setCompanyWebsite((String) map.get("companyWebsite"));
        settings.setCompanyLogoPath((String) map.get("companyLogoPath"));
        settings.setNavTechnicalUser((String) map.get("navTechnicalUser"));
        settings.setNavSignatureKey((String) map.get("navSignatureKey"));
        settings.setNavReplacementKey((String) map.get("navReplacementKey"));
        settings.setNavTestMode((Boolean) map.getOrDefault("navTestMode", true));
        settings.setInvoicePrefix((String) map.getOrDefault("invoicePrefix", "INV"));

        Object nextNumber = map.get("invoiceNextNumber");
        if (nextNumber instanceof Number) {
            settings.setInvoiceNextNumber(((Number) nextNumber).intValue());
        }

        settings.setInvoiceFooterText((String) map.get("invoiceFooterText"));

        Object paymentDays = map.get("defaultPaymentDeadlineDays");
        if (paymentDays instanceof Number) {
            settings.setDefaultPaymentDeadlineDays(((Number) paymentDays).intValue());
        }

        settings.setDefaultCurrency((String) map.getOrDefault("defaultCurrency", "HUF"));

        Object vatRate = map.get("defaultVatRate");
        if (vatRate instanceof Number) {
            settings.setDefaultVatRate(((Number) vatRate).doubleValue());
        }

        settings.setCreatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("createdAt")));
        settings.setUpdatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("updatedAt")));

        return settings;
    }
}
