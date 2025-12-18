package com.employeemanager.service.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.model.Customer;
import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * NAV Online Invoice 3.0 XML generátor
 * A NAV által előírt XML formátumban állítja elő a számlákat
 */
@Service
@Slf4j
public class NavInvoiceXmlGenerator {

    private static final String NAV_VERSION = "3.0";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Számla XML generálása NAV 3.0 formátumban
     */
    public String generateInvoiceXml(Invoice invoice, CompanySettings companySettings) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Root element: InvoiceData
        Element rootElement = doc.createElement("InvoiceData");
        doc.appendChild(rootElement);

        // Namespace és verzió
        rootElement.setAttribute("xmlns", "http://schemas.nav.gov.hu/OSA/3.0/data");
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

        // Invoice Exchange
        Element invoiceExchange = doc.createElement("invoiceExchange");
        rootElement.appendChild(invoiceExchange);

        // Invoice Header
        Element invoiceHead = doc.createElement("invoiceHead");
        invoiceExchange.appendChild(invoiceHead);

        // Supplier info (Eladó)
        addSupplierInfo(doc, invoiceHead, companySettings);

        // Customer info (Vevő)
        addCustomerInfo(doc, invoiceHead, invoice.getCustomer());

        // Invoice Detail
        addInvoiceDetail(doc, invoiceHead, invoice);

        // Invoice Lines (Tételek)
        Element invoiceLines = doc.createElement("invoiceLines");
        invoiceExchange.appendChild(invoiceLines);

        int lineNumber = 1;
        for (InvoiceItem item : invoice.getItems()) {
            addInvoiceLine(doc, invoiceLines, item, lineNumber++);
        }

        // Invoice Summary (Összesítők)
        addInvoiceSummary(doc, invoiceExchange, invoice);

        // XML konvertálás String-gé
        return convertDocumentToString(doc);
    }

    /**
     * Eladó adatok hozzáadása
     */
    private void addSupplierInfo(Document doc, Element parent, CompanySettings settings) {
        Element supplierInfo = doc.createElement("supplierInfo");
        parent.appendChild(supplierInfo);

        // Adószám
        Element supplierTaxNumber = doc.createElement("supplierTaxNumber");
        supplierInfo.appendChild(supplierTaxNumber);

        Element taxpayerId = doc.createElement("taxpayerId");
        taxpayerId.setTextContent(cleanTaxNumber(settings.getCompanyTaxNumber()));
        supplierTaxNumber.appendChild(taxpayerId);

        // Név
        Element supplierName = doc.createElement("supplierName");
        supplierName.setTextContent(settings.getCompanyName());
        supplierInfo.appendChild(supplierName);

        // Cím
        Element supplierAddress = doc.createElement("supplierAddress");
        supplierInfo.appendChild(supplierAddress);

        Element simpleAddress = doc.createElement("simpleAddress");
        supplierAddress.appendChild(simpleAddress);

        addTextElement(doc, simpleAddress, "countryCode", "HU");
        addTextElement(doc, simpleAddress, "region", "");
        addTextElement(doc, simpleAddress, "postalCode", settings.getCompanyZipCode());
        addTextElement(doc, simpleAddress, "city", settings.getCompanyCity());
        addTextElement(doc, simpleAddress, "additionalAddressDetail", settings.getCompanyAddress());

        // Bankszámla
        if (settings.getCompanyBankAccount() != null) {
            Element supplierBankAccount = doc.createElement("supplierBankAccountNumber");
            supplierBankAccount.setTextContent(settings.getCompanyBankAccount().replaceAll("[^0-9]", ""));
            supplierInfo.appendChild(supplierBankAccount);
        }
    }

    /**
     * Vevő adatok hozzáadása
     */
    private void addCustomerInfo(Document doc, Element parent, Customer customer) {
        Element customerInfo = doc.createElement("customerInfo");
        parent.appendChild(customerInfo);

        // Adószám (ha van)
        if (customer.getTaxNumber() != null && !customer.getTaxNumber().isEmpty()) {
            Element customerTaxNumber = doc.createElement("customerTaxNumber");
            customerInfo.appendChild(customerTaxNumber);

            Element taxpayerId = doc.createElement("taxpayerId");
            taxpayerId.setTextContent(cleanTaxNumber(customer.getTaxNumber()));
            customerTaxNumber.appendChild(taxpayerId);
        }

        // Név
        Element customerName = doc.createElement("customerName");
        customerName.setTextContent(customer.getName());
        customerInfo.appendChild(customerName);

        // Cím
        Element customerAddress = doc.createElement("customerAddress");
        customerInfo.appendChild(customerAddress);

        Element simpleAddress = doc.createElement("simpleAddress");
        customerAddress.appendChild(simpleAddress);

        String country = customer.getCountry() != null ? customer.getCountry() : "HU";
        addTextElement(doc, simpleAddress, "countryCode", country);
        addTextElement(doc, simpleAddress, "region", "");
        addTextElement(doc, simpleAddress, "postalCode", customer.getZipCode());
        addTextElement(doc, simpleAddress, "city", customer.getCity());
        addTextElement(doc, simpleAddress, "additionalAddressDetail", customer.getAddress());
    }

    /**
     * Számla részletek hozzáadása
     * Jogszabályi megfelelés: 2007. évi CXXVII. törvény + NAV Online Számla 3.0
     */
    private void addInvoiceDetail(Document doc, Element parent, Invoice invoice) {
        Element invoiceDetail = doc.createElement("invoiceDetail");
        parent.appendChild(invoiceDetail);

        // Számla kategória
        addTextElement(doc, invoiceDetail, "invoiceCategory", "NORMAL");

        // Dátumok
        addTextElement(doc, invoiceDetail, "invoiceIssueDate",
            invoice.getInvoiceDate().format(DATE_FORMATTER));
        addTextElement(doc, invoiceDetail, "invoiceDeliveryDate",
            invoice.getDeliveryDate() != null ?
                invoice.getDeliveryDate().format(DATE_FORMATTER) :
                invoice.getInvoiceDate().format(DATE_FORMATTER));

        // Pénznem
        addTextElement(doc, invoiceDetail, "currencyCode",
            invoice.getCurrency() != null ? invoice.getCurrency() : "HUF");

        // Árfolyam - NAV KÖTELEZŐ MEZŐ! (Forintos számlánál is! érték: 1.000000)
        String exchangeRate = invoice.getExchangeRate() != null ?
                invoice.getExchangeRate().setScale(6, RoundingMode.HALF_UP).toString() :
                "1.000000";
        addTextElement(doc, invoiceDetail, "exchangeRate", exchangeRate);

        // Fizetési mód
        addTextElement(doc, invoiceDetail, "paymentMethod", "TRANSFER");

        // Fizetési határidő
        if (invoice.getPaymentDeadline() != null) {
            addTextElement(doc, invoiceDetail, "paymentDate",
                invoice.getPaymentDeadline().format(DATE_FORMATTER));
        }

        // Fordított adózás jelölése (2007. évi CXXVII. törvény)
        if (invoice.getIsReverseCharge() != null && invoice.getIsReverseCharge()) {
            addTextElement(doc, invoiceDetail, "invoiceAppearance", "ELECTRONIC");
            Element additionalInvoiceData = doc.createElement("additionalInvoiceData");
            invoiceDetail.appendChild(additionalInvoiceData);
            addTextElement(doc, additionalInvoiceData, "dataName", "Fordított adózás");
            addTextElement(doc, additionalInvoiceData, "dataDescription", "fordított adózás");
        }

        // Pénzforgalmi elszámolás jelölése (2007. évi CXXVII. törvény)
        if (invoice.getIsCashAccounting() != null && invoice.getIsCashAccounting()) {
            Element additionalInvoiceData = doc.createElement("additionalInvoiceData");
            invoiceDetail.appendChild(additionalInvoiceData);
            addTextElement(doc, additionalInvoiceData, "dataName", "Pénzforgalmi elszámolás");
            addTextElement(doc, additionalInvoiceData, "dataDescription", "pénzforgalmi elszámolás");
        }

        // Számlaszám
        addTextElement(doc, invoiceDetail, "invoiceNumber", invoice.getInvoiceNumber());
    }

    /**
     * Számla tétel hozzáadása
     * Jogszabályi megfelelés: ÁFA mentességi ok kezelése
     */
    private void addInvoiceLine(Document doc, Element parent, InvoiceItem item, int lineNumber) {
        Element line = doc.createElement("line");
        parent.appendChild(line);

        // Sorszám
        addTextElement(doc, line, "lineNumber", String.valueOf(lineNumber));

        // Megnevezés
        Element lineDescription = doc.createElement("lineDescription");
        lineDescription.setTextContent(item.getDescription());
        line.appendChild(lineDescription);

        // Mennyiség
        addTextElement(doc, line, "quantity", item.getQuantity().setScale(4, RoundingMode.HALF_UP).toString());
        addTextElement(doc, line, "unitOfMeasure", item.getUnitOfMeasure());

        // Egységár
        addTextElement(doc, line, "unitPrice", item.getUnitPrice().setScale(2, RoundingMode.HALF_UP).toString());

        // ÁFA kulcs és mentesség kezelése
        Element lineVatData = doc.createElement("lineVatData");
        line.appendChild(lineVatData);

        // Ha van ÁFA mentességi ok, akkor speciális kezelés
        if (item.getVatExemptionReason() != null && !item.getVatExemptionReason().isEmpty()) {
            addTextElement(doc, lineVatData, "lineVatContent", "false");
            addTextElement(doc, lineVatData, "lineVatExemption", "TAM"); // ÁFA mentes (TAM = adómentes)

            // Mentességi ok részletezése
            Element lineVatExemptionReason = doc.createElement("lineVatExemptionReason");
            lineVatExemptionReason.setTextContent(item.getVatExemptionReason());
            lineVatData.appendChild(lineVatExemptionReason);
        } else {
            // Normál ÁFA
            addTextElement(doc, lineVatData, "lineVatContent", "true");

            BigDecimal vatRate = item.getVatRate() != null ? item.getVatRate() : BigDecimal.ZERO;
            addTextElement(doc, lineVatData, "lineVatRate", vatRate.setScale(2, RoundingMode.HALF_UP).toString());
        }

        // Nettó érték
        addTextElement(doc, line, "lineNetAmount", item.getNetAmount().setScale(2, RoundingMode.HALF_UP).toString());

        // ÁFA összeg
        addTextElement(doc, line, "lineVatAmount", item.getVatAmount().setScale(2, RoundingMode.HALF_UP).toString());

        // Bruttó érték
        addTextElement(doc, line, "lineGrossAmount", item.getGrossAmount().setScale(2, RoundingMode.HALF_UP).toString());
    }

    /**
     * Számla összesítő hozzáadása
     */
    private void addInvoiceSummary(Document doc, Element parent, Invoice invoice) {
        Element invoiceSummary = doc.createElement("invoiceSummary");
        parent.appendChild(invoiceSummary);

        // Összesített értékek
        addTextElement(doc, invoiceSummary, "invoiceNetAmount",
            invoice.getNetAmount().setScale(2, RoundingMode.HALF_UP).toString());
        addTextElement(doc, invoiceSummary, "invoiceVatAmount",
            invoice.getVatAmount().setScale(2, RoundingMode.HALF_UP).toString());
        addTextElement(doc, invoiceSummary, "invoiceGrossAmount",
            invoice.getGrossAmount().setScale(2, RoundingMode.HALF_UP).toString());
    }

    /**
     * Adószám tisztítása (csak számok)
     */
    private String cleanTaxNumber(String taxNumber) {
        if (taxNumber == null) return "";
        return taxNumber.replaceAll("[^0-9]", "");
    }

    /**
     * Szöveges elem hozzáadása
     */
    private void addTextElement(Document doc, Element parent, String name, String value) {
        Element element = doc.createElement(name);
        element.setTextContent(value != null ? value : "");
        parent.appendChild(element);
    }

    /**
     * Document konvertálása String-gé
     */
    private String convertDocumentToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
