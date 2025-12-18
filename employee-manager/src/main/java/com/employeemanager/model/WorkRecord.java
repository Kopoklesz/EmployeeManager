package com.employeemanager.model;

import com.employeemanager.util.FirebaseDateConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Entity
@Table(name = "work_records")
public class WorkRecord {

    @Id
    private String id; // Firebase ID-k String típusúak

    @NotNull(message = "Az alkalmazott megadása kötelező")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull(message = "A bejelentés dátumának megadása kötelező")
    @PastOrPresent(message = "A bejelentés dátuma nem lehet jövőbeli")
    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;

    @Column(name = "notification_time")
    private LocalTime notificationTime;

    @NotBlank(message = "Az EBEV sorozatszám megadása kötelező")
    @Size(max = 100, message = "Az EBEV sorozatszám maximum 100 karakter lehet")
    @Column(name = "ebev_serial", nullable = false)
    private String ebevSerialNumber;

    @NotNull(message = "A munka dátumának megadása kötelező")
    @PastOrPresent(message = "A munka dátuma nem lehet jövőbeli")
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @NotNull(message = "A fizetés összegének megadása kötelező")
    @DecimalMin(value = "0.01", message = "A fizetés összege pozitív szám kell legyen")
    @Digits(integer = 8, fraction = 2, message = "A fizetés formátuma nem megfelelő")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal payment;

    @NotNull(message = "A ledolgozott órák számának megadása kötelező")
    @Min(value = 1, message = "Legalább 1 órát kell dolgozni")
    @Max(value = 24, message = "Maximum 24 órát lehet dolgozni egy nap")
    @Column(name = "hours_worked", nullable = false)
    private Integer hoursWorked;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (notificationTime == null) {
            notificationTime = LocalTime.now();
        }
    }

    /**
     * Firebase számára Map formátumba konvertál
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("employeeId", employee != null ? employee.getId() : null);
        map.put("employeeName", employee != null ? employee.getName() : null);
        map.put("notificationDate", FirebaseDateConverter.dateToString(notificationDate));
        map.put("notificationTime", FirebaseDateConverter.timeToString(notificationTime));
        map.put("ebevSerialNumber", ebevSerialNumber);
        map.put("workDate", FirebaseDateConverter.dateToString(workDate));
        map.put("payment", payment != null ? payment.toString() : "0");
        map.put("hoursWorked", hoursWorked);
        map.put("createdAt", FirebaseDateConverter.dateTimeToString(createdAt));
        return map;
    }

    /**
     * Firebase Map-ből objektummá konvertál (employee nélkül)
     */
    public static WorkRecord fromMap(Map<String, Object> map) {
        WorkRecord record = new WorkRecord();
        record.setId((String) map.get("id"));
        record.setNotificationDate(FirebaseDateConverter.stringToDate((String) map.get("notificationDate")));
        record.setNotificationTime(FirebaseDateConverter.stringToTime((String) map.get("notificationTime")));
        record.setEbevSerialNumber((String) map.get("ebevSerialNumber"));
        record.setWorkDate(FirebaseDateConverter.stringToDate((String) map.get("workDate")));

        String paymentStr = (String) map.get("payment");
        if (paymentStr != null && !paymentStr.isEmpty()) {
            record.setPayment(new BigDecimal(paymentStr));
        }

        Object hoursObj = map.get("hoursWorked");
        if (hoursObj instanceof Number) {
            record.setHoursWorked(((Number) hoursObj).intValue());
        }

        record.setCreatedAt(FirebaseDateConverter.stringToDateTime((String) map.get("createdAt")));
        return record;
    }
}