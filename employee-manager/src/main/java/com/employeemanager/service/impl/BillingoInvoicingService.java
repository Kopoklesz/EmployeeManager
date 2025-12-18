package com.employeemanager.service.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.model.Customer;
import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.repository.CompanySettingsRepository;
import com.employeemanager.service.InvoicingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Billingo API v3 integráció
 *
 * Dokumentáció: https://www.billingo.hu/api/v3
 * API: REST JSON alapú, API kulcs szükséges (Bearer token)
 */
@Slf4j
public class BillingoInvoicingService implements InvoicingService {

    private static final String API_URL = "https://api.billingo.hu/v3";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CompanySettingsRepository settingsRepository;
    private final ObjectMapper objectMapper;

    public BillingoInvoicingService(CompanySettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public InvoicingResult issueInvoice(Invoice invoice) throws Exception {
        log.info("Issuing invoice on Billingo: {}", invoice.getInvoiceNumber());

        CompanySettings settings = settingsRepository.get();
        String apiKey = settings.getBillingoApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Billingo API kulcs nincs beállítva!");
        }

        // 1. Partner létrehozása/frissítése (ha még nincs)
        Integer partnerId = createOrUpdatePartner(invoice.getCustomer(), apiKey);

        // 2. Számla létrehozása
        ObjectNode invoiceJson = buildBillingoInvoiceJson(invoice, partnerId);

        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL + "/documents").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-API-KEY", apiKey);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(invoiceJson));
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        log.info("Billingo response code: {}", responseCode);

        if (responseCode == 201) {
            // Sikeres kiállítás
            String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode response = objectMapper.readTree(responseBody);

            Integer billingoId = response.get("id").asInt();
            String publicUrl = response.has("public_url") ? response.get("public_url").asText() : null;

            log.info("Invoice issued successfully on Billingo: {} (Billingo ID: {})",
                invoice.getInvoiceNumber(), billingoId);

            // PDF letöltése
            byte[] pdfData = downloadBillingoPdf(billingoId, apiKey);

            return new InvoicingResult(
                true,
                "Számla sikeresen kiállítva a Billingo-n",
                billingoId.toString(),
                publicUrl,
                pdfData
            );
        } else {
            // Hibaüzenet olvasása
            String errorMsg = new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.error("Billingo error: {}", errorMsg);

            return new InvoicingResult(
                false,
                "Billingo hiba: " + errorMsg,
                null,
                null,
                null
            );
        }
    }

    @Override
    public boolean cancelInvoice(Invoice invoice, String reason) throws Exception {
        log.info("Cancelling invoice on Billingo: {}", invoice.getInvoiceNumber());

        CompanySettings settings = settingsRepository.get();
        String apiKey = settings.getBillingoApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Billingo API kulcs nincs beállítva!");
        }

        // Billingo-n a számlát sztornózni kell
        // Ehhez először meg kell keresni a számla ID-ját a külső rendszerben
        // (Ezt az externalId-ben kell tárolni)

        if (invoice.getExternalInvoiceId() == null) {
            log.error("Cannot cancel invoice: Billingo ID not found");
            return false;
        }

        // Sztornó számla létrehozása
        HttpURLConnection connection = (HttpURLConnection)
            new URL(API_URL + "/documents/" + invoice.getExternalInvoiceId() + "/cancel").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-API-KEY", apiKey);

        int responseCode = connection.getResponseCode();
        log.info("Billingo cancellation response code: {}", responseCode);

        return responseCode == 201;
    }

    @Override
    public byte[] downloadInvoicePdf(Invoice invoice) throws Exception {
        log.info("Downloading PDF from Billingo for invoice: {}", invoice.getInvoiceNumber());

        CompanySettings settings = settingsRepository.get();
        String apiKey = settings.getBillingoApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Billingo API kulcs nincs beállítva!");
        }

        if (invoice.getExternalInvoiceId() == null) {
            throw new IllegalStateException("Billingo invoice ID not found");
        }

        return downloadBillingoPdf(Integer.parseInt(invoice.getExternalInvoiceId()), apiKey);
    }

    @Override
    public boolean isAvailable() {
        try {
            CompanySettings settings = settingsRepository.get();
            String apiKey = settings.getBillingoApiKey();
            return apiKey != null && !apiKey.isEmpty();
        } catch (Exception e) {
            log.error("Error checking Billingo availability", e);
            return false;
        }
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.BILLINGO;
    }

    /**
     * Partner (vevő) létrehozása vagy frissítése Billingo-n
     */
    private Integer createOrUpdatePartner(Customer customer, String apiKey) throws Exception {
        ObjectNode partnerJson = objectMapper.createObjectNode();
        partnerJson.put("name", customer.getName());
        partnerJson.put("country", customer.getCountry());
        partnerJson.put("zip", customer.getZipCode());
        partnerJson.put("city", customer.getCity());
        partnerJson.put("address", customer.getAddress());

        if (customer.getTaxNumber() != null && !customer.getTaxNumber().isEmpty()) {
            partnerJson.put("tax_number", customer.getTaxNumber());
        }

        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            partnerJson.put("emails", objectMapper.createArrayNode().add(customer.getEmail()));
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL + "/partners").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-API-KEY", apiKey);
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(partnerJson));
            os.flush();
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == 201) {
            String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode response = objectMapper.readTree(responseBody);
            return response.get("id").asInt();
        } else if (responseCode == 422) {
            // Partner már létezik, keresés adószám vagy név alapján
            log.info("Partner already exists, searching...");
            return findPartnerByName(customer.getName(), apiKey);
        } else {
            throw new Exception("Failed to create partner: " + responseCode);
        }
    }

    /**
     * Partner keresése név alapján
     */
    private Integer findPartnerByName(String name, String apiKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
            new URL(API_URL + "/partners?name=" + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8)).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-API-KEY", apiKey);

        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            String responseBody = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonNode response = objectMapper.readTree(responseBody);

            if (response.isArray() && response.size() > 0) {
                return response.get(0).get("id").asInt();
            }
        }

        throw new Exception("Partner not found: " + name);
    }

    /**
     * Billingo számla JSON összeállítása
     */
    private ObjectNode buildBillingoInvoiceJson(Invoice invoice, Integer partnerId) {
        ObjectNode json = objectMapper.createObjectNode();

        json.put("partner_id", partnerId);
        json.put("type", "invoice"); // Normál számla
        json.put("fulfillment_date", invoice.getDeliveryDate().format(DATE_FORMATTER));
        json.put("due_date", invoice.getPaymentDeadline().format(DATE_FORMATTER));
        json.put("payment_method", getPaymentMethod(invoice));
        json.put("currency", invoice.getCurrency());
        json.put("comment", invoice.getNotes());

        // Tételek
        ArrayNode items = objectMapper.createArrayNode();
        for (InvoiceItem item : invoice.getItems()) {
            ObjectNode itemJson = objectMapper.createObjectNode();
            itemJson.put("name", item.getDescription());
            itemJson.put("quantity", item.getQuantity().setScale(2, RoundingMode.HALF_UP).doubleValue());
            itemJson.put("unit", item.getUnit());
            itemJson.put("unit_price", item.getUnitPrice().setScale(2, RoundingMode.HALF_UP).doubleValue());
            itemJson.put("vat", item.getVatRate().intValue() + "%");

            items.add(itemJson);
        }
        json.set("items", items);

        return json;
    }

    /**
     * PDF letöltése Billingo-ról
     */
    private byte[] downloadBillingoPdf(Integer billingoId, String apiKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
            new URL(API_URL + "/documents/" + billingoId + "/download").openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-API-KEY", apiKey);

        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            connection.getInputStream().transferTo(baos);
            return baos.toByteArray();
        } else {
            throw new Exception("Failed to download PDF from Billingo: " + responseCode);
        }
    }

    private String getPaymentMethod(Invoice invoice) {
        // Billingo fizetési módok
        return switch (invoice.getPaymentMethod()) {
            case BANK_TRANSFER -> "transfer";
            case CASH -> "cash";
            case CARD -> "card";
            default -> "transfer";
        };
    }
}
