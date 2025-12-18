package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Számla tétel entitás
 */
@Entity
@Table(name = "invoice_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "unit_of_measure")
    @Builder.Default
    private String unitOfMeasure = "db";

    @Column(nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("27.00");

    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "vat_amount", precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "gross_amount", precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String notes;

    @Transient
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        calculateAmounts();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateAmounts();
    }

    /**
     * Összegek kiszámítása
     */
    public void calculateAmounts() {
        if (unitPrice == null || quantity == null) {
            this.netAmount = BigDecimal.ZERO;
            this.vatAmount = BigDecimal.ZERO;
            this.grossAmount = BigDecimal.ZERO;
            return;
        }

        // Nettó ár = egységár * mennyiség
        BigDecimal baseNet = unitPrice.multiply(quantity)
            .setScale(2, RoundingMode.HALF_UP);

        // Kedvezmény alkalmazása
        if (discountPercent != null && discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = baseNet
                .multiply(discountPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }

        this.netAmount = baseNet.subtract(discountAmount)
            .setScale(2, RoundingMode.HALF_UP);

        // ÁFA számítás
        if (vatRate != null && vatRate.compareTo(BigDecimal.ZERO) > 0) {
            this.vatAmount = netAmount
                .multiply(vatRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            this.vatAmount = BigDecimal.ZERO;
        }

        // Bruttó ár
        this.grossAmount = netAmount.add(vatAmount)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Egységár kedvezmény után
     */
    public BigDecimal getUnitPriceAfterDiscount() {
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal baseNet = unitPrice.multiply(quantity);
            BigDecimal netAfterDiscount = baseNet.subtract(discountAmount);
            return netAfterDiscount.divide(quantity, 2, RoundingMode.HALF_UP);
        }
        return unitPrice;
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("invoiceId", invoice != null ? invoice.getId() : null);
        map.put("lineNumber", lineNumber);
        map.put("description", description);
        map.put("unitOfMeasure", unitOfMeasure);
        map.put("quantity", quantity != null ? quantity.toString() : "1");
        map.put("unitPrice", unitPrice != null ? unitPrice.toString() : "0");
        map.put("vatRate", vatRate != null ? vatRate.toString() : "27.00");
        map.put("netAmount", netAmount != null ? netAmount.toString() : "0");
        map.put("vatAmount", vatAmount != null ? vatAmount.toString() : "0");
        map.put("grossAmount", grossAmount != null ? grossAmount.toString() : "0");
        map.put("discountPercent", discountPercent != null ? discountPercent.toString() : "0");
        map.put("discountAmount", discountAmount != null ? discountAmount.toString() : "0");
        map.put("notes", notes);
        map.put("createdAt", FirebaseDateConverter.dateTimeToString(createdAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál (invoice nélkül)
     */
    public static InvoiceItem fromMap(Map<String, Object> map) {
        InvoiceItem item = new InvoiceItem();
        item.setId((String) map.get("id"));

        Object lineNum = map.get("lineNumber");
        if (lineNum instanceof Number) {
            item.setLineNumber(((Number) lineNum).intValue());
        }

        item.setDescription((String) map.get("description"));
        item.setUnitOfMeasure((String) map.getOrDefault("unitOfMeasure", "db"));

        String quantityStr = (String) map.get("quantity");
        if (quantityStr != null && !quantityStr.isEmpty()) {
            item.setQuantity(new BigDecimal(quantityStr));
        }

        String unitPriceStr = (String) map.get("unitPrice");
        if (unitPriceStr != null && !unitPriceStr.isEmpty()) {
            item.setUnitPrice(new BigDecimal(unitPriceStr));
        }

        String vatRateStr = (String) map.get("vatRate");
        if (vatRateStr != null && !vatRateStr.isEmpty()) {
            item.setVatRate(new BigDecimal(vatRateStr));
        }

        String netAmountStr = (String) map.get("netAmount");
        if (netAmountStr != null && !netAmountStr.isEmpty()) {
            item.setNetAmount(new BigDecimal(netAmountStr));
        }

        String vatAmountStr = (String) map.get("vatAmount");
        if (vatAmountStr != null && !vatAmountStr.isEmpty()) {
            item.setVatAmount(new BigDecimal(vatAmountStr));
        }

        String grossAmountStr = (String) map.get("grossAmount");
        if (grossAmountStr != null && !grossAmountStr.isEmpty()) {
            item.setGrossAmount(new BigDecimal(grossAmountStr));
        }

        String discountPercentStr = (String) map.get("discountPercent");
        if (discountPercentStr != null && !discountPercentStr.isEmpty()) {
            item.setDiscountPercent(new BigDecimal(discountPercentStr));
        }

        String discountAmountStr = (String) map.get("discountAmount");
        if (discountAmountStr != null && !discountAmountStr.isEmpty()) {
            item.setDiscountAmount(new BigDecimal(discountAmountStr));
        }

        item.setNotes((String) map.get("notes"));
        item.setCreatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("createdAt")));

        return item;
    }
}
