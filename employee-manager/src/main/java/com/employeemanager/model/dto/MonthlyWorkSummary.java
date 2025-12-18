package com.employeemanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Havi összesítő adatok
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyWorkSummary {

    private YearMonth month;
    private String employeeId;
    private String employeeName;
    private Integer totalDaysWorked;
    private Integer totalHoursWorked;
    private BigDecimal totalPayment;
    private Integer numberOfRecords;

    public String getMonthDisplay() {
        if (month == null) return "";
        return String.format("%d. %s",
            month.getYear(),
            getHungarianMonth(month.getMonthValue()));
    }

    private String getHungarianMonth(int monthValue) {
        String[] months = {
            "Január", "Február", "Március", "Április",
            "Május", "Június", "Július", "Augusztus",
            "Szeptember", "Október", "November", "December"
        };
        return months[monthValue - 1];
    }
}
