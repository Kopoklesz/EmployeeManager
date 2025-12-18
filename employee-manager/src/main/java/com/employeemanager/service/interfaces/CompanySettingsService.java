package com.employeemanager.service.interfaces;

import com.employeemanager.model.CompanySettings;

import java.util.concurrent.ExecutionException;

/**
 * Company Settings Service interface
 * Vállalati beállítások kezelése
 */
public interface CompanySettingsService {

    /**
     * Vállalati beállítások lekérdezése
     */
    CompanySettings getSettings() throws ExecutionException, InterruptedException;

    /**
     * Vállalati beállítások mentése
     */
    CompanySettings saveSettings(CompanySettings settings) throws ExecutionException, InterruptedException;

    /**
     * Következő számlaszám generálása
     */
    String generateNextInvoiceNumber() throws ExecutionException, InterruptedException;

    /**
     * Számlázó backend lekérdezése
     */
    String getInvoicingBackend() throws ExecutionException, InterruptedException;

    /**
     * Számlázó backend beállítása
     */
    void setInvoicingBackend(String backend) throws ExecutionException, InterruptedException;

    /**
     * NAV beállítások validálása
     */
    boolean areNavSettingsValid() throws ExecutionException, InterruptedException;

    /**
     * Szamlazz.hu beállítások validálása
     */
    boolean areSzamlazzSettingsValid() throws ExecutionException, InterruptedException;

    /**
     * Billingo beállítások validálása
     */
    boolean areBillingoSettingsValid() throws ExecutionException, InterruptedException;
}
