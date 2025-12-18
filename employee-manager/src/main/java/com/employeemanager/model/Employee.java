package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
public class Employee {

    @Id
    private String id; // Firebase ID-k String típusúak

    @NotBlank(message = "A név megadása kötelező")
    @Size(min = 2, max = 200, message = "A név hossza 2 és 200 karakter között kell legyen")
    @Column(nullable = false)
    private String name;

    @Size(max = 200, message = "A születési hely maximum 200 karakter lehet")
    @Column(name = "birth_place")
    private String birthPlace;

    @Past(message = "A születési dátum múltbeli dátum kell legyen")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 200, message = "Az anyja neve maximum 200 karakter lehet")
    @Column(name = "mother_name")
    private String motherName;

    @NotBlank(message = "Az adószám megadása kötelező")
    @Pattern(regexp = "\\d{10}", message = "Az adószám 10 számjegyből kell álljon")
    @Column(name = "tax_number", unique = true)
    private String taxNumber;

    @NotBlank(message = "A TAJ szám megadása kötelező")
    @Pattern(regexp = "\\d{9}", message = "A TAJ szám 9 számjegyből kell álljon")
    @Column(name = "social_security_number", unique = true)
    private String socialSecurityNumber;

    @Size(max = 500, message = "A cím maximum 500 karakter lehet")
    private String address;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<WorkRecord> workRecords = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("birthPlace", birthPlace);
        map.put("birthDate", FirebaseDateConverter.dateToString(birthDate));
        map.put("motherName", motherName);
        map.put("taxNumber", taxNumber);
        map.put("socialSecurityNumber", socialSecurityNumber);
        map.put("address", address);
        map.put("createdAt", FirebaseDateConverter.dateToString(createdAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál
     */
    public static Employee fromMap(Map<String, Object> map) {
        Employee employee = new Employee();
        employee.setId((String) map.get("id"));
        employee.setName((String) map.get("name"));
        employee.setBirthPlace((String) map.get("birthPlace"));
        employee.setBirthDate(FirebaseDateConverter.stringToDate((String) map.get("birthDate")));
        employee.setMotherName((String) map.get("motherName"));
        employee.setTaxNumber((String) map.get("taxNumber"));
        employee.setSocialSecurityNumber((String) map.get("socialSecurityNumber"));
        employee.setAddress((String) map.get("address"));
        employee.setCreatedAt(FirebaseDateConverter.stringToDate((String) map.get("createdAt")));
        return employee;
    }
}