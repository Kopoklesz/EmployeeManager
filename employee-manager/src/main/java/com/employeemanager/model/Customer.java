package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vevő/Vásárló entitás
 */
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_number")
    private String taxNumber;

    @Column(name = "eu_tax_number")
    private String euTaxNumber;

    @Column(name = "zip_code")
    private String zipCode;

    private String city;

    private String address;

    private String country;

    private String email;

    private String phone;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "billing_zip_code")
    private String billingZipCode;

    @Column(name = "billing_city")
    private String billingCity;

    @Column(name = "billing_country")
    private String billingCountry;

    @Column(name = "payment_deadline_days")
    @Builder.Default
    private Integer paymentDeadlineDays = 8;

    private String notes;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_company")
    @Builder.Default
    private Boolean isCompany = true;

    @Transient
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Invoice> invoices = new ArrayList<>();

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
     * Teljes cím
     */
    public String getFullAddress() {
        if (zipCode != null && city != null && address != null) {
            return String.format("%s %s, %s", zipCode, city, address);
        }
        return address != null ? address : "";
    }

    /**
     * Teljes számlázási cím
     */
    public String getFullBillingAddress() {
        if (billingZipCode != null && billingCity != null && billingAddress != null) {
            return String.format("%s %s, %s", billingZipCode, billingCity, billingAddress);
        }
        if (billingAddress != null) {
            return billingAddress;
        }
        // Ha nincs külön számlázási cím, a normál címet használjuk
        return getFullAddress();
    }

    /**
     * Megjelenítendő név adószámmal (ha van)
     */
    public String getDisplayName() {
        if (taxNumber != null && !taxNumber.isEmpty()) {
            return String.format("%s (Adószám: %s)", name, taxNumber);
        }
        return name;
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("taxNumber", taxNumber);
        map.put("euTaxNumber", euTaxNumber);
        map.put("zipCode", zipCode);
        map.put("city", city);
        map.put("address", address);
        map.put("country", country);
        map.put("email", email);
        map.put("phone", phone);
        map.put("contactPerson", contactPerson);
        map.put("billingAddress", billingAddress);
        map.put("billingZipCode", billingZipCode);
        map.put("billingCity", billingCity);
        map.put("billingCountry", billingCountry);
        map.put("paymentDeadlineDays", paymentDeadlineDays);
        map.put("notes", notes);
        map.put("isActive", isActive);
        map.put("isCompany", isCompany);
        map.put("createdAt", FirebaseDateConverter.dateTimeToString(createdAt));
        map.put("updatedAt", FirebaseDateConverter.dateTimeToString(updatedAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál
     */
    public static Customer fromMap(Map<String, Object> map) {
        Customer customer = new Customer();
        customer.setId((String) map.get("id"));
        customer.setName((String) map.get("name"));
        customer.setTaxNumber((String) map.get("taxNumber"));
        customer.setEuTaxNumber((String) map.get("euTaxNumber"));
        customer.setZipCode((String) map.get("zipCode"));
        customer.setCity((String) map.get("city"));
        customer.setAddress((String) map.get("address"));
        customer.setCountry((String) map.get("country"));
        customer.setEmail((String) map.get("email"));
        customer.setPhone((String) map.get("phone"));
        customer.setContactPerson((String) map.get("contactPerson"));
        customer.setBillingAddress((String) map.get("billingAddress"));
        customer.setBillingZipCode((String) map.get("billingZipCode"));
        customer.setBillingCity((String) map.get("billingCity"));
        customer.setBillingCountry((String) map.get("billingCountry"));

        Object paymentDays = map.get("paymentDeadlineDays");
        if (paymentDays instanceof Number) {
            customer.setPaymentDeadlineDays(((Number) paymentDays).intValue());
        }

        customer.setNotes((String) map.get("notes"));
        customer.setIsActive((Boolean) map.getOrDefault("isActive", true));
        customer.setIsCompany((Boolean) map.getOrDefault("isCompany", true));
        customer.setCreatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("createdAt")));
        customer.setUpdatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("updatedAt")));

        return customer;
    }
}
