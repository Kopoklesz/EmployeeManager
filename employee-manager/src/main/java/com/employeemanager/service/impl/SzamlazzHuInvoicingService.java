package com.employeemanager.service.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.model.Customer;
import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.repository.CompanySettingsRepository;
import com.employeemanager.service.InvoicingService;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Szamlazz.hu API integráció
 *
 * Dokumentáció: https://www.szamlazz.hu/szamlazas/dokumentacio/
 * API: XML alapú, Agent kulcs szükséges
 */
@Slf4j
public class SzamlazzHuInvoicingService implements InvoicingService {

    private static final String API_URL = "https://www.szamlazz.hu/szamla/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CompanySettingsRepository settingsRepository;

    public SzamlazzHuInvoicingService(CompanySettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public InvoicingResult issueInvoice(Invoice invoice) throws Exception {
        log.info("Issuing invoice on Szamlazz.hu: {}", invoice.getInvoiceNumber());

        CompanySettings settings = settingsRepository.get();
        String agentKey = settings.getSzamlazzAgentKey();

        if (agentKey == null || agentKey.isEmpty()) {
            throw new IllegalStateException("Szamlazz.hu Agent kulcs nincs beállítva!");
        }

        // XML összeállítása
        String xml = buildSzamlazzXml(invoice, settings, agentKey);

        // API hívás
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(xml.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        log.info("Szamlazz.hu response code: {}", responseCode);

        if (responseCode == 200) {
            // Sikeres kiállítás - PDF letöltése a válaszból
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            connection.getInputStream().transferTo(baos);
            byte[] pdfData = baos.toByteArray();

            String szlahuId = connection.getHeaderField("szlahu_szamlaszam");

            log.info("Invoice issued successfully on Szamlazz.hu: {} (Szamlazz.hu ID: {})",
                invoice.getInvoiceNumber(), szlahuId);

            return new InvoicingResult(
                true,
                "Számla sikeresen kiállítva a Szamlazz.hu-n",
                szlahuId,
                "https://www.szamlazz.hu/szamla/",
                pdfData
            );
        } else {
            // Hibaüzenet olvasása
            String errorMsg = new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.error("Szamlazz.hu error: {}", errorMsg);

            return new InvoicingResult(
                false,
                "Szamlazz.hu hiba: " + errorMsg,
                null,
                null,
                null
            );
        }
    }

    @Override
    public boolean cancelInvoice(Invoice invoice, String reason) throws Exception {
        log.info("Cancelling invoice on Szamlazz.hu: {}", invoice.getInvoiceNumber());

        CompanySettings settings = settingsRepository.get();
        String agentKey = settings.getSzamlazzAgentKey();

        if (agentKey == null || agentKey.isEmpty()) {
            throw new IllegalStateException("Szamlazz.hu Agent kulcs nincs beállítva!");
        }

        // Sztornó számla XML összeállítása
        String xml = buildSzamlazzCancellationXml(invoice, settings, agentKey, reason);

        // API hívás
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(xml.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        log.info("Szamlazz.hu cancellation response code: {}", responseCode);

        return responseCode == 200;
    }

    @Override
    public byte[] downloadInvoicePdf(Invoice invoice) throws Exception {
        log.info("Downloading PDF from Szamlazz.hu for invoice: {}", invoice.getInvoiceNumber());

        CompanySettings settings = settingsRepository.get();
        String agentKey = settings.getSzamlazzAgentKey();

        if (agentKey == null || agentKey.isEmpty()) {
            throw new IllegalStateException("Szamlazz.hu Agent kulcs nincs beállítva!");
        }

        // PDF letöltés XML kérés
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<xmlszamlapdf xmlns=\"http://www.szamlazz.hu/xmlszamlapdf\">\n" +
                     "  <felhasznalo>" + agentKey + "</felhasznalo>\n" +
                     "  <szamlaszam>" + invoice.getInvoiceNumber() + "</szamlaszam>\n" +
                     "</xmlszamlapdf>";

        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(xml.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            connection.getInputStream().transferTo(baos);
            return baos.toByteArray();
        } else {
            throw new Exception("Failed to download PDF from Szamlazz.hu: " + responseCode);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            CompanySettings settings = settingsRepository.get();
            String agentKey = settings.getSzamlazzAgentKey();
            return agentKey != null && !agentKey.isEmpty();
        } catch (Exception e) {
            log.error("Error checking Szamlazz.hu availability", e);
            return false;
        }
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.SZAMLAZZ_HU;
    }

    /**
     * Szamlazz.hu XML összeállítása számla kiállításhoz
     */
    private String buildSzamlazzXml(Invoice invoice, CompanySettings settings, String agentKey) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<xmlszamla xmlns=\"http://www.szamlazz.hu/xmlszamla\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");

        // Beállítások
        xml.append("  <beallitasok>\n");
        xml.append("    <felhasznalo>").append(agentKey).append("</felhasznalo>\n");
        xml.append("    <szamlaLetoltes>true</szamlaLetoltes>\n");
        xml.append("  </beallitasok>\n");

        // Fejléc
        xml.append("  <fejlec>\n");
        xml.append("    <keltDatum>").append(invoice.getInvoiceDate().format(DATE_FORMATTER)).append("</keltDatum>\n");
        xml.append("    <teljesitesDatum>").append(invoice.getDeliveryDate().format(DATE_FORMATTER)).append("</teljesitesDatum>\n");
        xml.append("    <fizetesiHataridoDatum>").append(invoice.getPaymentDeadline().format(DATE_FORMATTER)).append("</fizetesiHataridoDatum>\n");
        xml.append("    <fizmod>").append(getPaymentMethod(invoice)).append("</fizmod>\n");
        xml.append("    <penznem>").append(invoice.getCurrency()).append("</penznem>\n");
        xml.append("    <szamlaNyelve>hu</szamlaNyelve>\n");
        xml.append("    <megjegyzes>").append(escapeXml(invoice.getNotes())).append("</megjegyzes>\n");
        xml.append("  </fejlec>\n");

        // Eladó (cég adatok)
        xml.append("  <elado>\n");
        xml.append("    <bank>").append(escapeXml(settings.getBankName())).append("</bank>\n");
        xml.append("    <bankszamlaszam>").append(settings.getBankAccountNumber()).append("</bankszamlaszam>\n");
        xml.append("  </elado>\n");

        // Vevő
        Customer customer = invoice.getCustomer();
        xml.append("  <vevo>\n");
        xml.append("    <nev>").append(escapeXml(customer.getName())).append("</nev>\n");
        xml.append("    <orszag>").append(customer.getCountry()).append("</orszag>\n");
        xml.append("    <irsz>").append(customer.getZipCode()).append("</irsz>\n");
        xml.append("    <telepules>").append(escapeXml(customer.getCity())).append("</telepules>\n");
        xml.append("    <cim>").append(escapeXml(customer.getAddress())).append("</cim>\n");
        if (customer.getTaxNumber() != null && !customer.getTaxNumber().isEmpty()) {
            xml.append("    <adoszam>").append(customer.getTaxNumber()).append("</adoszam>\n");
        }
        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            xml.append("    <email>").append(customer.getEmail()).append("</email>\n");
        }
        xml.append("  </vevo>\n");

        // Tételek
        xml.append("  <tetelek>\n");
        for (InvoiceItem item : invoice.getItems()) {
            xml.append("    <tetel>\n");
            xml.append("      <megnevezes>").append(escapeXml(item.getDescription())).append("</megnevezes>\n");
            xml.append("      <mennyiseg>").append(item.getQuantity().setScale(2, RoundingMode.HALF_UP)).append("</mennyiseg>\n");
            xml.append("      <mennyisegiEgyseg>").append(escapeXml(item.getUnit())).append("</mennyisegiEgyseg>\n");
            xml.append("      <nettoEgysegar>").append(item.getUnitPrice().setScale(2, RoundingMode.HALF_UP)).append("</nettoEgysegar>\n");
            xml.append("      <afakulcs>").append(item.getVatRate().intValue()).append("</afakulcs>\n");
            xml.append("      <nettoErtek>").append(item.getNetAmount().setScale(2, RoundingMode.HALF_UP)).append("</nettoErtek>\n");
            xml.append("      <afaErtek>").append(item.getVatAmount().setScale(2, RoundingMode.HALF_UP)).append("</afaErtek>\n");
            xml.append("      <bruttoErtek>").append(item.getGrossAmount().setScale(2, RoundingMode.HALF_UP)).append("</bruttoErtek>\n");
            xml.append("    </tetel>\n");
        }
        xml.append("  </tetelek>\n");

        xml.append("</xmlszamla>");

        return xml.toString();
    }

    /**
     * Sztornó XML összeállítása
     */
    private String buildSzamlazzCancellationXml(Invoice invoice, CompanySettings settings, String agentKey, String reason) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<xmlszamlast xmlns=\"http://www.szamlazz.hu/xmlszamlast\">\n");
        xml.append("  <beallitasok>\n");
        xml.append("    <felhasznalo>").append(agentKey).append("</felhasznalo>\n");
        xml.append("    <szamlaLetoltes>true</szamlaLetoltes>\n");
        xml.append("  </beallitasok>\n");
        xml.append("  <fejlec>\n");
        xml.append("    <szamlaszam>").append(invoice.getInvoiceNumber()).append("</szamlaszam>\n");
        xml.append("    <keltDatum>").append(java.time.LocalDate.now().format(DATE_FORMATTER)).append("</keltDatum>\n");
        xml.append("    <megjegyzes>Sztornó oka: ").append(escapeXml(reason)).append("</megjegyzes>\n");
        xml.append("  </fejlec>\n");
        xml.append("</xmlszamlast>");

        return xml.toString();
    }

    private String getPaymentMethod(Invoice invoice) {
        // Szamlazz.hu fizetési módok
        return switch (invoice.getPaymentMethod()) {
            case BANK_TRANSFER -> "Átutalás";
            case CASH -> "Készpénz";
            case CARD -> "Bankkártya";
            default -> "Átutalás";
        };
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
