package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Számla entitás
 */
@Entity
@Table(name = "invoices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    private String id;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "payment_deadline")
    private LocalDate paymentDeadline;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    private String currency;

    @Column(name = "exchange_rate", precision = 12, scale = 6)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "vat_amount", precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "gross_amount", precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "is_paid")
    @Builder.Default
    private Boolean isPaid = false;

    @Column(name = "is_sent_to_nav")
    @Builder.Default
    private Boolean isSentToNav = false;

    @Column(name = "nav_transaction_id")
    private String navTransactionId;

    @Column(name = "nav_sent_at")
    private LocalDateTime navSentAt;

    @Column(name = "external_invoice_id")
    private String externalInvoiceId; // Szamlazz.hu vagy Billingo számla ID

    @Column(name = "is_reverse_charge")
    @Builder.Default
    private Boolean isReverseCharge = false;

    @Column(name = "is_cash_accounting")
    @Builder.Default
    private Boolean isCashAccounting = false;

    @Column(name = "footer_text", length = 1000)
    private String footerText;

    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Fizetési módok
     */
    public enum PaymentMethod {
        BANK_TRANSFER("Átutalás"),
        CASH("Készpénz"),
        CARD("Bankkártya"),
        OTHER("Egyéb");

        private final String displayName;

        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Számla állapotok
     */
    public enum InvoiceStatus {
        DRAFT("Piszkozat"),
        ISSUED("Kiállítva"),
        SENT("Elküldve"),
        PAID("Kifizetve"),
        CANCELLED("Sztornózva"),
        OVERDUE("Lejárt");

        private final String displayName;

        InvoiceStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Számla összegek újraszámítása a tételek alapján
     */
    public void recalculateAmounts() {
        if (items == null || items.isEmpty()) {
            this.netAmount = BigDecimal.ZERO;
            this.vatAmount = BigDecimal.ZERO;
            this.grossAmount = BigDecimal.ZERO;
            return;
        }

        this.netAmount = items.stream()
            .map(InvoiceItem::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.vatAmount = items.stream()
            .map(InvoiceItem::getVatAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.grossAmount = items.stream()
            .map(InvoiceItem::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Tétel hozzáadása
     */
    public void addItem(InvoiceItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        item.setInvoice(this);
        recalculateAmounts();
    }

    /**
     * Tétel eltávolítása
     */
    public void removeItem(InvoiceItem item) {
        if (items != null) {
            items.remove(item);
            item.setInvoice(null);
            recalculateAmounts();
        }
    }

    /**
     * Határidő lejárt-e
     */
    public boolean isOverdue() {
        return !isPaid && paymentDeadline != null &&
               paymentDeadline.isBefore(LocalDate.now());
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("invoiceNumber", invoiceNumber);
        map.put("customerId", customer != null ? customer.getId() : null);
        map.put("customerName", customer != null ? customer.getName() : null);
        map.put("invoiceDate", FirebaseDateConverter.dateToString(invoiceDate));
        map.put("paymentDeadline", FirebaseDateConverter.dateToString(paymentDeadline));
        map.put("deliveryDate", FirebaseDateConverter.dateToString(deliveryDate));
        map.put("paymentDate", FirebaseDateConverter.dateToString(paymentDate));
        map.put("paymentMethod", paymentMethod != null ? paymentMethod.name() : PaymentMethod.BANK_TRANSFER.name());
        map.put("currency", currency);
        map.put("externalInvoiceId", externalInvoiceId);
        map.put("exchangeRate", exchangeRate != null ? exchangeRate.toString() : "1.000000");
        map.put("netAmount", netAmount != null ? netAmount.toString() : "0");
        map.put("vatAmount", vatAmount != null ? vatAmount.toString() : "0");
        map.put("grossAmount", grossAmount != null ? grossAmount.toString() : "0");
        map.put("status", status.name());
        map.put("isPaid", isPaid);
        map.put("isSentToNav", isSentToNav);
        map.put("navTransactionId", navTransactionId);
        map.put("navSentAt", FirebaseDateConverter.dateTimeToString(navSentAt));
        map.put("isReverseCharge", isReverseCharge);
        map.put("isCashAccounting", isCashAccounting);
        map.put("footerText", footerText);
        map.put("notes", notes);
        map.put("createdAt", FirebaseDateConverter.dateTimeToString(createdAt));
        map.put("updatedAt", FirebaseDateConverter.dateTimeToString(updatedAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál (items nélkül)
     */
    public static Invoice fromMap(Map<String, Object> map) {
        Invoice invoice = new Invoice();
        invoice.setId((String) map.get("id"));
        invoice.setInvoiceNumber((String) map.get("invoiceNumber"));
        invoice.setInvoiceDate(FirebaseDateConverter.stringToDate((String) map.get("invoiceDate")));
        invoice.setPaymentDeadline(FirebaseDateConverter.stringToDate((String) map.get("paymentDeadline")));
        invoice.setDeliveryDate(FirebaseDateConverter.stringToDate((String) map.get("deliveryDate")));
        invoice.setPaymentDate(FirebaseDateConverter.stringToDate((String) map.get("paymentDate")));

        String paymentMethodStr = (String) map.get("paymentMethod");
        if (paymentMethodStr != null) {
            try {
                invoice.setPaymentMethod(PaymentMethod.valueOf(paymentMethodStr));
            } catch (IllegalArgumentException e) {
                invoice.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
            }
        }

        invoice.setCurrency((String) map.get("currency"));
        invoice.setExternalInvoiceId((String) map.get("externalInvoiceId"));

        String exchangeRateStr = (String) map.get("exchangeRate");
        if (exchangeRateStr != null && !exchangeRateStr.isEmpty()) {
            invoice.setExchangeRate(new BigDecimal(exchangeRateStr));
        } else {
            invoice.setExchangeRate(BigDecimal.ONE);
        }

        String netAmountStr = (String) map.get("netAmount");
        if (netAmountStr != null && !netAmountStr.isEmpty()) {
            invoice.setNetAmount(new BigDecimal(netAmountStr));
        }

        String vatAmountStr = (String) map.get("vatAmount");
        if (vatAmountStr != null && !vatAmountStr.isEmpty()) {
            invoice.setVatAmount(new BigDecimal(vatAmountStr));
        }

        String grossAmountStr = (String) map.get("grossAmount");
        if (grossAmountStr != null && !grossAmountStr.isEmpty()) {
            invoice.setGrossAmount(new BigDecimal(grossAmountStr));
        }

        String statusStr = (String) map.get("status");
        if (statusStr != null) {
            invoice.setStatus(InvoiceStatus.valueOf(statusStr));
        }

        invoice.setIsPaid((Boolean) map.getOrDefault("isPaid", false));
        invoice.setIsSentToNav((Boolean) map.getOrDefault("isSentToNav", false));
        invoice.setNavTransactionId((String) map.get("navTransactionId"));
        invoice.setNavSentAt(FirebaseDateConverter.stringToDateTime((String) map.get("navSentAt")));
        invoice.setIsReverseCharge((Boolean) map.getOrDefault("isReverseCharge", false));
        invoice.setIsCashAccounting((Boolean) map.getOrDefault("isCashAccounting", false));
        invoice.setFooterText((String) map.get("footerText"));
        invoice.setNotes((String) map.get("notes"));
        invoice.setCreatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("createdAt")));
        invoice.setUpdatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("updatedAt")));

        return invoice;
    }
}
