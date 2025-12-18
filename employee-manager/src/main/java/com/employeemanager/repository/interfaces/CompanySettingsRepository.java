package com.employeemanager.repository.interfaces;

import com.employeemanager.model.CompanySettings;

import java.util.concurrent.ExecutionException;

/**
 * CompanySettings repository interface
 * Egyetlen beállítás rekord kezelése (singleton pattern)
 */
public interface CompanySettingsRepository {

    /**
     * Vállalati beállítások lekérdezése
     * Ha nem létezik, alapértelmezett értékkel visszatér
     */
    CompanySettings get() throws ExecutionException, InterruptedException;

    /**
     * Vállalati beállítások mentése
     */
    CompanySettings save(CompanySettings settings) throws ExecutionException, InterruptedException;

    /**
     * Beállítások létezik-e
     */
    boolean exists() throws ExecutionException, InterruptedException;

    /**
     * Következő számlaszám generálása és mentése
     */
    String generateNextInvoiceNumber() throws ExecutionException, InterruptedException;
}
