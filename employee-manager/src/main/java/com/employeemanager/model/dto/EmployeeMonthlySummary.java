package com.employeemanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * Alkalmazott havi összesítése
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeMonthlySummary {

    private String employeeId;
    private String employeeName;
    private YearMonth month;
    private Set<LocalDate> workDates;  // Egyedi munkavégzési dátumok
    private Integer totalHoursWorked;
    private BigDecimal totalPayment;
    private BigDecimal averageHourlyRate;

    public Integer getUniqueDaysWorked() {
        return workDates != null ? workDates.size() : 0;
    }

    public BigDecimal getAverageHourlyRate() {
        if (totalHoursWorked == null || totalHoursWorked == 0 ||
            totalPayment == null || totalPayment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalPayment.divide(BigDecimal.valueOf(totalHoursWorked), 2, BigDecimal.ROUND_HALF_UP);
    }
}
