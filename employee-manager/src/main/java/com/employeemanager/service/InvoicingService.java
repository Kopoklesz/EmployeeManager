package com.employeemanager.service;

import com.employeemanager.model.Invoice;

/**
 * Általános számlázási szolgáltatás interface
 *
 * Különböző számlázó backend-ek egységes kezelése:
 * - NAV XML export (kézi feltöltéshez)
 * - Szamlazz.hu API integráció
 * - Billingo API integráció
 */
public interface InvoicingService {

    /**
     * Backend típus enum
     */
    enum BackendType {
        NAV_EXPORT("NAV_EXPORT", "NAV XML export (kézi feltöltés)"),
        SZAMLAZZ_HU("SZAMLAZZ_HU", "Szamlazz.hu online számlázás"),
        BILLINGO("BILLINGO", "Billingo online számlázás");

        private final String code;
        private final String displayName;

        BackendType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static BackendType fromCode(String code) {
            for (BackendType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            return NAV_EXPORT; // Default
        }
    }

    /**
     * Számlázási eredmény
     */
    record InvoicingResult(
        boolean success,
        String message,
        String externalId,      // Külső rendszer számlaazonosítója
        String documentUrl,     // PDF/XML dokumentum URL-je
        byte[] documentData     // Dokumentum tartalma (PDF/XML)
    ) {}

    /**
     * Számla kiállítása a kiválasztott backend-en
     *
     * @param invoice a kiállítandó számla
     * @return számlázási eredmény (sikeres/sikertelen, külső azonosító, dokumentum)
     * @throws Exception ha a kiállítás sikertelen
     */
    InvoicingResult issueInvoice(Invoice invoice) throws Exception;

    /**
     * Számla sztornózása/törlése a backend-en
     *
     * @param invoice a sztornózandó számla
     * @param reason sztornózás oka
     * @return sikeres-e a művelet
     * @throws Exception ha a sztornózás sikertelen
     */
    boolean cancelInvoice(Invoice invoice, String reason) throws Exception;

    /**
     * Számla PDF letöltése a backend-ről
     *
     * @param invoice a számla
     * @return PDF tartalom byte array-ként
     * @throws Exception ha a letöltés sikertelen
     */
    byte[] downloadInvoicePdf(Invoice invoice) throws Exception;

    /**
     * Backend elérhető-e (konfiguráció, hálózat, API kulcs ellenőrzés)
     *
     * @return elérhető-e a backend
     */
    boolean isAvailable();

    /**
     * Backend típus lekérdezése
     *
     * @return backend típus
     */
    BackendType getBackendType();
}
