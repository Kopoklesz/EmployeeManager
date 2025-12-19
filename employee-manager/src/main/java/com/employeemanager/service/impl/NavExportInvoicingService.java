package com.employeemanager.service.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.model.Invoice;
import com.employeemanager.repository.interfaces.CompanySettingsRepository;
import com.employeemanager.service.InvoicingService;
import lombok.extern.slf4j.Slf4j;

/**
 * NAV XML export számlázási szolgáltatás
 *
 * Helyi NAV Online Számla 3.0 XML generálás kézi NAV feltöltéshez.
 * Nem igényel külső API kulcsot vagy internetkapcsolatot.
 */
@Slf4j
public class NavExportInvoicingService implements InvoicingService {

    private final NavInvoiceXmlGenerator xmlGenerator;
    private final InvoicePdfGenerator pdfGenerator;
    private final CompanySettingsRepository settingsRepository;

    public NavExportInvoicingService(NavInvoiceXmlGenerator xmlGenerator, InvoicePdfGenerator pdfGenerator, CompanySettingsRepository settingsRepository) {
        this.xmlGenerator = xmlGenerator;
        this.pdfGenerator = pdfGenerator;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public InvoicingResult issueInvoice(Invoice invoice) throws Exception {
        log.info("Generating NAV XML for invoice: {}", invoice.getInvoiceNumber());

        try {
            // CompanySettings lekérése
            CompanySettings settings = settingsRepository.get();

            // XML generálás
            String xml = xmlGenerator.generateInvoiceXml(invoice, settings);

            // XML mentése byte array-ként
            byte[] xmlData = xml.getBytes("UTF-8");

            log.info("NAV XML generated successfully for invoice: {} ({} bytes)",
                invoice.getInvoiceNumber(), xmlData.length);

            return new InvoicingResult(
                true,
                "NAV XML sikeresen generálva. Töltse fel a NAV Online Számla portálra.",
                invoice.getInvoiceNumber(),
                null, // Nincs külső URL
                xmlData
            );

        } catch (Exception e) {
            log.error("Failed to generate NAV XML for invoice: {}", invoice.getInvoiceNumber(), e);
            return new InvoicingResult(
                false,
                "NAV XML generálás sikertelen: " + e.getMessage(),
                null,
                null,
                null
            );
        }
    }

    @Override
    public boolean cancelInvoice(Invoice invoice, String reason) throws Exception {
        log.info("Generating cancellation NAV XML for invoice: {}", invoice.getInvoiceNumber());

        // NAV XML export esetén nem szükséges automatikus sztornózás
        // A felhasználó manuálisan tölti fel a sztornó számlát a NAV-ra
        log.info("NAV export mode: manual cancellation required on NAV portal");

        return true;
    }

    @Override
    public byte[] downloadInvoicePdf(Invoice invoice) throws Exception {
        log.info("Generating PDF for invoice: {}", invoice.getInvoiceNumber());

        try {
            return pdfGenerator.generatePdf(invoice);
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new Exception("PDF generálás sikertelen: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        // NAV export mindig elérhető, nem igényel külső szolgáltatást
        return true;
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.NAV_EXPORT;
    }
}
