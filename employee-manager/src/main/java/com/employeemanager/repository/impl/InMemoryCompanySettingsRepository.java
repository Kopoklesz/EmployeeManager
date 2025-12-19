package com.employeemanager.repository.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.repository.interfaces.CompanySettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory CompanySettings Repository implementáció
 *
 * Ez az alapértelmezett implementáció, ami akkor lesz használva, ha nincs
 * sem Firebase, sem JDBC konfiguráció. Memóriában tárolja az adatokat,
 * újraindításkor elvesznek.
 *
 * Lehetővé teszi az alkalmazás indítását adatbázis konfiguráció nélkül,
 * így a felhasználó a UI-ban beállíthatja az adatbázist.
 */
@Slf4j
@Repository("inMemoryCompanySettingsRepository")
@ConditionalOnMissingBean(CompanySettingsRepository.class)
public class InMemoryCompanySettingsRepository implements CompanySettingsRepository {

    private static final String SETTINGS_ID = "default";
    private final AtomicReference<CompanySettings> settings = new AtomicReference<>();

    public InMemoryCompanySettingsRepository() {
        log.warn("Using in-memory CompanySettings repository - data will be lost on restart!");
        log.info("Please configure a database connection (Firebase, MySQL, PostgreSQL, or H2) for persistent storage");
        initializeDefaults();
    }

    private void initializeDefaults() {
        CompanySettings defaults = CompanySettings.builder()
            .id(SETTINGS_ID)
            .companyName("Cégem Kft.")
            .companyAddress("1234 Budapest, Példa utca 1.")
            .companyZipCode("1234")
            .companyCity("Budapest")
            .companyTaxNumber("12345678")
            .invoicePrefix("INV")
            .invoiceNextNumber(1)
            .defaultPaymentDeadlineDays(8)
            .defaultCurrency("HUF")
            .defaultVatRate(27.0)
            .invoicingBackend("NAV_EXPORT")
            .navTestMode(true)
            .build();

        settings.set(defaults);
        log.info("Initialized in-memory CompanySettings with defaults");
    }

    @Override
    public CompanySettings get() throws ExecutionException, InterruptedException {
        CompanySettings current = settings.get();
        if (current == null) {
            initializeDefaults();
            current = settings.get();
        }
        return CompanySettings.builder()
            .id(current.getId())
            .companyName(current.getCompanyName())
            .companyAddress(current.getCompanyAddress())
            .companyZipCode(current.getCompanyZipCode())
            .companyCity(current.getCompanyCity())
            .companyTaxNumber(current.getCompanyTaxNumber())
            .companyEUTaxNumber(current.getCompanyEUTaxNumber())
            .companyBankName(current.getCompanyBankName())
            .companyBankAccount(current.getCompanyBankAccount())
            .invoicePrefix(current.getInvoicePrefix())
            .invoiceNextNumber(current.getInvoiceNextNumber())
            .defaultPaymentDeadlineDays(current.getDefaultPaymentDeadlineDays())
            .defaultCurrency(current.getDefaultCurrency())
            .defaultVatRate(current.getDefaultVatRate())
            .invoicingBackend(current.getInvoicingBackend())
            .szamlazzAgentKey(current.getSzamlazzAgentKey())
            .billingoApiKey(current.getBillingoApiKey())
            .navTechnicalUser(current.getNavTechnicalUser())
            .navSignatureKey(current.getNavSignatureKey())
            .navReplacementKey(current.getNavReplacementKey())
            .navTestMode(current.getNavTestMode())
            .build();
    }

    @Override
    public CompanySettings save(CompanySettings newSettings) throws ExecutionException, InterruptedException {
        // Biztosítjuk, hogy az ID mindig "default" legyen
        newSettings.setId(SETTINGS_ID);

        settings.set(newSettings);
        log.info("CompanySettings saved to in-memory storage");

        return get();
    }

    @Override
    public boolean exists() throws ExecutionException, InterruptedException {
        return settings.get() != null;
    }

    @Override
    public synchronized String generateNextInvoiceNumber() throws ExecutionException, InterruptedException {
        CompanySettings current = get();

        String invoiceNumber = current.generateNextInvoiceNumber();

        // Mentjük a frissített számlaszám számlálót
        save(current);

        log.info("Generated invoice number: {}", invoiceNumber);

        return invoiceNumber;
    }
}
