package com.employeemanager.service.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.repository.interfaces.CompanySettingsRepository;
import com.employeemanager.service.exception.ValidationException;
import com.employeemanager.service.interfaces.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

/**
 * Company Settings Service implementáció
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CompanySettingsServiceImpl implements CompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;

    @Override
    public CompanySettings getSettings() throws ExecutionException, InterruptedException {
        log.debug("Getting company settings");
        return companySettingsRepository.get();
    }

    @Override
    public CompanySettings saveSettings(CompanySettings settings) throws ExecutionException, InterruptedException {
        log.info("Saving company settings");

        // Validálás
        validateSettings(settings);

        CompanySettings savedSettings = companySettingsRepository.save(settings);
        log.info("Company settings saved successfully");

        return savedSettings;
    }

    @Override
    public String generateNextInvoiceNumber() throws ExecutionException, InterruptedException {
        log.debug("Generating next invoice number");
        return companySettingsRepository.generateNextInvoiceNumber();
    }

    @Override
    public String getInvoicingBackend() throws ExecutionException, InterruptedException {
        CompanySettings settings = getSettings();
        return settings.getInvoicingBackend() != null ? settings.getInvoicingBackend() : "NAV_EXPORT";
    }

    @Override
    public void setInvoicingBackend(String backend) throws ExecutionException, InterruptedException {
        log.info("Setting invoicing backend to: {}", backend);

        // Validálás
        if (backend == null || backend.isEmpty()) {
            throw new ValidationException("Számlázó backend nem lehet üres");
        }

        if (!backend.equals("NAV_EXPORT") && !backend.equals("SZAMLAZZ_HU") && !backend.equals("BILLINGO")) {
            throw new ValidationException("Érvénytelen számlázó backend: " + backend);
        }

        CompanySettings settings = getSettings();
        settings.setInvoicingBackend(backend);

        // Beállítások validálása a backend alapján
        if (backend.equals("SZAMLAZZ_HU") && !areSzamlazzSettingsValid()) {
            log.warn("Szamlazz.hu API kulcs nincs beállítva!");
        }

        if (backend.equals("BILLINGO") && !areBillingoSettingsValid()) {
            log.warn("Billingo API kulcs nincs beállítva!");
        }

        companySettingsRepository.save(settings);
    }

    @Override
    public boolean areNavSettingsValid() throws ExecutionException, InterruptedException {
        CompanySettings settings = getSettings();

        // NAV technikai felhasználó és aláíró kulcs kötelező
        return settings.getNavTechnicalUser() != null && !settings.getNavTechnicalUser().isEmpty() &&
               settings.getNavSignatureKey() != null && !settings.getNavSignatureKey().isEmpty() &&
               settings.getNavReplacementKey() != null && !settings.getNavReplacementKey().isEmpty();
    }

    @Override
    public boolean areSzamlazzSettingsValid() throws ExecutionException, InterruptedException {
        CompanySettings settings = getSettings();

        // Szamlazz.hu Agent kulcs kötelező
        return settings.getSzamlazzAgentKey() != null && !settings.getSzamlazzAgentKey().isEmpty();
    }

    @Override
    public boolean areBillingoSettingsValid() throws ExecutionException, InterruptedException {
        CompanySettings settings = getSettings();

        // Billingo API kulcs kötelező
        return settings.getBillingoApiKey() != null && !settings.getBillingoApiKey().isEmpty();
    }

    /**
     * Beállítások validálása
     */
    private void validateSettings(CompanySettings settings) {
        if (settings == null) {
            throw new ValidationException("Beállítások nem lehetnek null");
        }

        // Cégnév kötelező
        if (settings.getCompanyName() == null || settings.getCompanyName().trim().isEmpty()) {
            throw new ValidationException("Cégnév kötelező");
        }

        // Adószám formátum (ha van)
        if (settings.getCompanyTaxNumber() != null && !settings.getCompanyTaxNumber().isEmpty()) {
            if (!isValidTaxNumber(settings.getCompanyTaxNumber())) {
                throw new ValidationException("Érvénytelen adószám formátum");
            }
        }

        // Számla előtag validálás
        if (settings.getInvoicePrefix() == null || settings.getInvoicePrefix().isEmpty()) {
            throw new ValidationException("Számla előtag kötelező");
        }

        if (settings.getInvoicePrefix().length() > 10) {
            throw new ValidationException("Számla előtag maximum 10 karakter lehet");
        }

        // Számla kezdőszám validálás
        if (settings.getInvoiceNextNumber() == null || settings.getInvoiceNextNumber() < 1) {
            throw new ValidationException("Számla kezdőszám legalább 1 kell legyen");
        }

        // Fizetési határidő validálás
        if (settings.getDefaultPaymentDeadlineDays() == null ||
            settings.getDefaultPaymentDeadlineDays() < 0 ||
            settings.getDefaultPaymentDeadlineDays() > 365) {
            throw new ValidationException("Fizetési határidő 0 és 365 nap között lehet");
        }

        // ÁFA kulcs validálás
        if (settings.getDefaultVatRate() == null ||
            settings.getDefaultVatRate() < 0 ||
            settings.getDefaultVatRate() > 100) {
            throw new ValidationException("ÁFA kulcs 0 és 100 között lehet");
        }

        // Pénznem validálás
        if (settings.getDefaultCurrency() == null || settings.getDefaultCurrency().isEmpty()) {
            throw new ValidationException("Pénznem kötelező");
        }

        if (settings.getDefaultCurrency().length() != 3) {
            throw new ValidationException("Pénznem 3 betűs kód kell legyen (pl. HUF, EUR, USD)");
        }
    }

    /**
     * Adószám validálása
     */
    private boolean isValidTaxNumber(String taxNumber) {
        if (taxNumber == null || taxNumber.isEmpty()) {
            return false;
        }

        // Magyar adószám: 8 számjegy vagy 8-1-2 formátum
        return taxNumber.matches("^\\d{8}(-\\d{1}(-\\d{2})?)?$");
    }
}
