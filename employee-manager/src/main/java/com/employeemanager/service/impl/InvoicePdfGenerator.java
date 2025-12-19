package com.employeemanager.service.impl;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.model.Customer;
import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.repository.CompanySettingsRepository;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Számla PDF generátor - Magyar formátum
 *
 * Jogszabályi követelmények:
 * - 2007. évi CXXVII. törvény (ÁFA tv.)
 * - Magyar számla formátum
 * - Kötelező adattartalom
 */
@Slf4j
public class InvoicePdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185); // Kék fejléc
    private static final DeviceRgb FOOTER_COLOR = new DeviceRgb(236, 240, 241); // Világosszürke lábléc

    private final CompanySettingsRepository settingsRepository;
    private PdfFont normalFont;
    private PdfFont boldFont;

    public InvoicePdfGenerator(CompanySettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        try {
            // Magyar ékezetes karakterek támogatása - Helvetica alapértelmezett
            this.normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA, PdfEncodings.CP1250);
            this.boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD, PdfEncodings.CP1250);
        } catch (IOException e) {
            log.error("Failed to load PDF fonts", e);
            throw new RuntimeException("Failed to initialize PDF generator", e);
        }
    }

    /**
     * Számla PDF generálása byte array-ként
     *
     * @param invoice a számla entitás
     * @return PDF tartalom byte array-ként
     * @throws Exception ha a generálás sikertelen
     */
    public byte[] generatePdf(Invoice invoice) throws Exception {
        log.info("Generating PDF for invoice: {}", invoice.getInvoiceNumber());

        CompanySettings settings = settingsRepository.get();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // Margók beállítása
        document.setMargins(50, 50, 50, 50);

        // 1. Fejléc - Céges adatok
        addCompanyHeader(document, settings);

        // 2. SZÁMLA cím
        addInvoiceTitle(document);

        // 3. Vevő és számla adatok
        addInvoiceInfo(document, invoice, settings);

        // 4. Tételek táblázat
        addItemsTable(document, invoice);

        // 5. Összesítő
        addSummary(document, invoice);

        // 6. Lábléc - Fizetési információk
        addFooter(document, invoice, settings);

        document.close();

        log.info("PDF generated successfully: {} bytes", baos.size());
        return baos.toByteArray();
    }

    /**
     * Céges fejléc
     */
    private void addCompanyHeader(Document document, CompanySettings settings) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1}))
            .useAllAvailableWidth()
            .setBorder(Border.NO_BORDER);

        // Cégnév
        Paragraph companyName = new Paragraph(settings.getCompanyName())
            .setFont(boldFont)
            .setFontSize(16)
            .setFontColor(HEADER_COLOR)
            .setMarginBottom(2);
        headerTable.addCell(new Cell().add(companyName).setBorder(Border.NO_BORDER));

        // Cím
        if (settings.getCompanyAddress() != null) {
            String fullAddress = String.format("%s %s, %s",
                settings.getCompanyZipCode() != null ? settings.getCompanyZipCode() : "",
                settings.getCompanyCity() != null ? settings.getCompanyCity() : "",
                settings.getCompanyAddress());
            headerTable.addCell(new Cell().add(new Paragraph(fullAddress).setFont(normalFont).setFontSize(10))
                .setBorder(Border.NO_BORDER));
        }

        // Adószám
        if (settings.getCompanyTaxNumber() != null) {
            headerTable.addCell(new Cell().add(new Paragraph("Adószám: " + settings.getCompanyTaxNumber())
                .setFont(normalFont).setFontSize(10)).setBorder(Border.NO_BORDER));
        }

        // EU adószám
        if (settings.getCompanyEUTaxNumber() != null && !settings.getCompanyEUTaxNumber().isEmpty()) {
            headerTable.addCell(new Cell().add(new Paragraph("EU adószám: " + settings.getCompanyEUTaxNumber())
                .setFont(normalFont).setFontSize(10)).setBorder(Border.NO_BORDER));
        }

        // Email
        if (settings.getCompanyEmail() != null) {
            headerTable.addCell(new Cell().add(new Paragraph("Email: " + settings.getCompanyEmail())
                .setFont(normalFont).setFontSize(10)).setBorder(Border.NO_BORDER));
        }

        // Telefon
        if (settings.getCompanyPhone() != null) {
            headerTable.addCell(new Cell().add(new Paragraph("Tel: " + settings.getCompanyPhone())
                .setFont(normalFont).setFontSize(10)).setBorder(Border.NO_BORDER));
        }

        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * SZÁMLA cím
     */
    private void addInvoiceTitle(Document document) {
        Paragraph title = new Paragraph("SZÁMLA")
            .setFont(boldFont)
            .setFontSize(24)
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(HEADER_COLOR)
            .setMarginBottom(20);
        document.add(title);
    }

    /**
     * Vevő és számla információk (két oszlopos)
     */
    private void addInvoiceInfo(Document document, Invoice invoice, CompanySettings settings) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth();

        // Bal oldal: Vevő adatok
        Customer customer = invoice.getCustomer();
        Table buyerTable = new Table(1).useAllAvailableWidth();
        buyerTable.addCell(new Cell().add(new Paragraph("VEVŐ").setFont(boldFont).setFontSize(12))
            .setBorder(Border.NO_BORDER).setBackgroundColor(FOOTER_COLOR).setPadding(5));
        buyerTable.addCell(new Cell().add(new Paragraph(customer.getName()).setFont(boldFont).setFontSize(11))
            .setBorder(Border.NO_BORDER).setPadding(5));

        if (customer.getZipCode() != null || customer.getCity() != null || customer.getAddress() != null) {
            String address = String.format("%s %s",
                customer.getZipCode() != null ? customer.getZipCode() + " " + customer.getCity() : customer.getCity(),
                customer.getAddress() != null ? customer.getAddress() : "");
            buyerTable.addCell(new Cell().add(new Paragraph(address).setFont(normalFont).setFontSize(10))
                .setBorder(Border.NO_BORDER).setPadding(5));
        }

        if (customer.getTaxNumber() != null && !customer.getTaxNumber().isEmpty()) {
            buyerTable.addCell(new Cell().add(new Paragraph("Adószám: " + customer.getTaxNumber())
                .setFont(normalFont).setFontSize(10)).setBorder(Border.NO_BORDER).setPadding(5));
        }

        infoTable.addCell(new Cell().add(buyerTable).setBorder(Border.NO_BORDER).setPaddingRight(10));

        // Jobb oldal: Számla adatok
        Table invoiceDataTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth();

        invoiceDataTable.addCell(createInfoCell("Számlaszám:", false));
        invoiceDataTable.addCell(createInfoCell(invoice.getInvoiceNumber(), true));

        invoiceDataTable.addCell(createInfoCell("Kiállítás dátuma:", false));
        invoiceDataTable.addCell(createInfoCell(invoice.getInvoiceDate().format(DATE_FORMATTER), true));

        invoiceDataTable.addCell(createInfoCell("Teljesítés dátuma:", false));
        invoiceDataTable.addCell(createInfoCell(invoice.getDeliveryDate().format(DATE_FORMATTER), true));

        invoiceDataTable.addCell(createInfoCell("Fizetési határidő:", false));
        invoiceDataTable.addCell(createInfoCell(invoice.getPaymentDeadline().format(DATE_FORMATTER), true));

        invoiceDataTable.addCell(createInfoCell("Fizetési mód:", false));
        invoiceDataTable.addCell(createInfoCell(invoice.getPaymentMethod().getDisplayName(), true));

        invoiceDataTable.addCell(createInfoCell("Pénznem:", false));
        invoiceDataTable.addCell(createInfoCell(invoice.getCurrency(), true));

        // Fordított adózás jelzése
        if (invoice.getIsReverseCharge() != null && invoice.getIsReverseCharge()) {
            Cell reverseChargeCell = createInfoCell("", false);
            reverseChargeCell.setRowspan(1);
            reverseChargeCell.setColspan(2);
            reverseChargeCell.add(new Paragraph("FORDÍTOTT ADÓZÁS").setFont(boldFont).setFontSize(10)
                .setFontColor(ColorConstants.RED));
            invoiceDataTable.addCell(reverseChargeCell);
        }

        // Pénzforgalmi elszámolás jelzése
        if (invoice.getIsCashAccounting() != null && invoice.getIsCashAccounting()) {
            Cell cashAccountingCell = createInfoCell("", false);
            cashAccountingCell.setRowspan(1);
            cashAccountingCell.setColspan(2);
            cashAccountingCell.add(new Paragraph("PÉNZFORGALMI ELSZÁMOLÁS").setFont(boldFont).setFontSize(10)
                .setFontColor(ColorConstants.RED));
            invoiceDataTable.addCell(cashAccountingCell);
        }

        infoTable.addCell(new Cell().add(invoiceDataTable).setBorder(Border.NO_BORDER));

        document.add(infoTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Tételek táblázat
     */
    private void addItemsTable(Document document, Invoice invoice) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1f, 3f, 1f, 1f, 1.5f, 1.2f, 1.5f, 1.5f, 2f}))
            .useAllAvailableWidth();

        // Fejléc
        String[] headers = {"Sorszám", "Megnevezés", "Menny.", "ME", "Egységár", "ÁFA%", "Nettó", "ÁFA", "Bruttó"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                .add(new Paragraph(header).setFont(boldFont).setFontSize(9))
                .setBackgroundColor(HEADER_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5));
        }

        // Tételek
        int lineNumber = 1;
        for (InvoiceItem item : invoice.getItems()) {
            table.addCell(createTableCell(String.valueOf(lineNumber++), TextAlignment.CENTER));
            table.addCell(createTableCell(item.getDescription(), TextAlignment.LEFT));
            table.addCell(createTableCell(formatNumber(item.getQuantity()), TextAlignment.RIGHT));
            table.addCell(createTableCell(item.getUnit(), TextAlignment.CENTER));
            table.addCell(createTableCell(formatAmount(item.getUnitPrice()), TextAlignment.RIGHT));

            // ÁFA% vagy mentességi ok
            if (item.getVatExemptionReason() != null && !item.getVatExemptionReason().isEmpty()) {
                table.addCell(createTableCell("Mentes", TextAlignment.CENTER)
                    .setFontSize(8).setItalic());
            } else {
                table.addCell(createTableCell(formatNumber(item.getVatRate()) + "%", TextAlignment.RIGHT));
            }

            table.addCell(createTableCell(formatAmount(item.getNetAmount()), TextAlignment.RIGHT));
            table.addCell(createTableCell(formatAmount(item.getVatAmount()), TextAlignment.RIGHT));
            table.addCell(createTableCell(formatAmount(item.getGrossAmount()), TextAlignment.RIGHT));
        }

        document.add(table);
    }

    /**
     * Összesítő táblázat
     */
    private void addSummary(Document document, Invoice invoice) {
        document.add(new Paragraph("\n"));

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1, 2}))
            .useAllAvailableWidth()
            .setMarginTop(10);

        // Nettó összeg
        summaryTable.addCell(createSummaryCell("", false));
        summaryTable.addCell(createSummaryCell("Nettó összesen:", true).setBold());
        summaryTable.addCell(createSummaryCell(formatAmount(invoice.getNetAmount()) + " " + invoice.getCurrency(), false)
            .setTextAlignment(TextAlignment.RIGHT).setBold());

        // ÁFA összeg
        summaryTable.addCell(createSummaryCell("", false));
        summaryTable.addCell(createSummaryCell("ÁFA összesen:", true).setBold());
        summaryTable.addCell(createSummaryCell(formatAmount(invoice.getVatAmount()) + " " + invoice.getCurrency(), false)
            .setTextAlignment(TextAlignment.RIGHT).setBold());

        // Bruttó összeg
        summaryTable.addCell(createSummaryCell("", false));
        summaryTable.addCell(createSummaryCell("Fizetendő bruttó:", true)
            .setBold().setBackgroundColor(FOOTER_COLOR));
        summaryTable.addCell(createSummaryCell(formatAmount(invoice.getGrossAmount()) + " " + invoice.getCurrency(), false)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBold()
            .setFontSize(14)
            .setBackgroundColor(FOOTER_COLOR));

        document.add(summaryTable);
    }

    /**
     * Lábléc - Fizetési információk és megjegyzések
     */
    private void addFooter(Document document, Invoice invoice, CompanySettings settings) {
        document.add(new Paragraph("\n\n"));

        // Fizetési információk
        Table paymentTable = new Table(1).useAllAvailableWidth();
        paymentTable.addCell(new Cell().add(new Paragraph("FIZETÉSI INFORMÁCIÓK")
            .setFont(boldFont).setFontSize(11))
            .setBackgroundColor(FOOTER_COLOR)
            .setBorder(Border.NO_BORDER)
            .setPadding(5));

        if (settings.getCompanyBankName() != null && settings.getCompanyBankAccount() != null) {
            paymentTable.addCell(new Cell().add(new Paragraph(
                "Bankszámlaszám: " + settings.getCompanyBankAccount() + " (" + settings.getCompanyBankName() + ")")
                .setFont(normalFont).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(5));
        }

        paymentTable.addCell(new Cell().add(new Paragraph(
            "Fizetési határidő: " + invoice.getPaymentDeadline().format(DATE_FORMATTER))
            .setFont(normalFont).setFontSize(10))
            .setBorder(Border.NO_BORDER)
            .setPadding(5));

        document.add(paymentTable);

        // Megjegyzések
        if ((invoice.getNotes() != null && !invoice.getNotes().isEmpty()) ||
            (invoice.getFooterText() != null && !invoice.getFooterText().isEmpty())) {

            document.add(new Paragraph("\n"));

            Table notesTable = new Table(1).useAllAvailableWidth();
            notesTable.addCell(new Cell().add(new Paragraph("MEGJEGYZÉSEK")
                .setFont(boldFont).setFontSize(11))
                .setBackgroundColor(FOOTER_COLOR)
                .setBorder(Border.NO_BORDER)
                .setPadding(5));

            if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
                notesTable.addCell(new Cell().add(new Paragraph(invoice.getNotes())
                    .setFont(normalFont).setFontSize(9))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5));
            }

            if (invoice.getFooterText() != null && !invoice.getFooterText().isEmpty()) {
                notesTable.addCell(new Cell().add(new Paragraph(invoice.getFooterText())
                    .setFont(normalFont).setFontSize(9).setItalic())
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5));
            }

            document.add(notesTable);
        }

        // Aláírás hely
        document.add(new Paragraph("\n\n\n"));
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .useAllAvailableWidth();

        signatureTable.addCell(new Cell().add(new Paragraph("_________________________\nKiállító")
            .setFont(normalFont).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
            .setBorder(Border.NO_BORDER));

        signatureTable.addCell(new Cell().add(new Paragraph("_________________________\nVevő")
            .setFont(normalFont).setFontSize(9).setTextAlignment(TextAlignment.CENTER))
            .setBorder(Border.NO_BORDER));

        document.add(signatureTable);
    }

    // Helper metódusok
    private Cell createInfoCell(String text, boolean bold) {
        Paragraph p = new Paragraph(text).setFontSize(10);
        if (bold) {
            p.setFont(boldFont);
        } else {
            p.setFont(normalFont);
        }
        return new Cell().add(p).setBorder(Border.NO_BORDER).setPadding(2);
    }

    private Cell createTableCell(String text, TextAlignment alignment) {
        return new Cell()
            .add(new Paragraph(text).setFont(normalFont).setFontSize(9))
            .setTextAlignment(alignment)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(4)
            .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f));
    }

    private Cell createSummaryCell(String text, boolean bold) {
        Paragraph p = new Paragraph(text).setFontSize(11);
        if (bold) {
            p.setFont(boldFont);
        } else {
            p.setFont(normalFont);
        }
        return new Cell().add(p).setBorder(Border.NO_BORDER).setPadding(3);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,d", amount.setScale(0, RoundingMode.HALF_UP).longValue())
            .replace(',', ' ');
    }

    private String formatNumber(BigDecimal number) {
        if (number == null) return "0";
        return number.stripTrailingZeros().toPlainString();
    }
}
