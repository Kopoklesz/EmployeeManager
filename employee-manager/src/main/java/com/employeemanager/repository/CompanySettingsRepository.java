package com.employeemanager.repository;

import com.employeemanager.model.CompanySettings;

import java.util.concurrent.ExecutionException;

/**
 * CompanySettings repository interface
 *
 * Céges beállítások kezelése - Singleton pattern
 * Csak egy CompanySettings entitás létezik az adatbázisban (id = "default")
 */
public interface CompanySettingsRepository {

    /**
     * Céges beállítások lekérése
     * Ha még nem létezik, alapértelmezett értékekkel hozza létre
     *
     * @return céges beállítások
     * @throws ExecutionException ha a lekérés sikertelen
     * @throws InterruptedException ha a lekérés megszakad
     */
    CompanySettings get() throws ExecutionException, InterruptedException;

    /**
     * Céges beállítások mentése
     *
     * @param settings mentendő beállítások
     * @return mentett beállítások
     * @throws ExecutionException ha a mentés sikertelen
     * @throws InterruptedException ha a mentés megszakad
     */
    CompanySettings save(CompanySettings settings) throws ExecutionException, InterruptedException;

    /**
     * Következő számlaszám generálása és mentése
     * Atomi művelet, thread-safe
     *
     * @return generált számlaszám (pl. "INV-0001")
     * @throws ExecutionException ha a művelet sikertelen
     * @throws InterruptedException ha a művelet megszakad
     */
    String generateNextInvoiceNumber() throws ExecutionException, InterruptedException;
}
