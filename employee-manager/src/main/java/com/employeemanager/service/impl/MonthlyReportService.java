package com.employeemanager.service.impl;

import com.employeemanager.model.WorkRecord;
import com.employeemanager.model.dto.EmployeeMonthlySummary;
import com.employeemanager.model.dto.MonthlyWorkSummary;
import com.employeemanager.service.interfaces.WorkRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Havi kimutatások és összesítések kezelése
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyReportService {

    private final WorkRecordService workRecordService;

    /**
     * Havi összesítő egy alkalmazotthoz
     */
    public EmployeeMonthlySummary getEmployeeMonthlySummary(String employeeId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<WorkRecord> records = workRecordService.findAll().stream()
            .filter(r -> r.getEmployee() != null && r.getEmployee().getId().equals(employeeId))
            .filter(r -> r.getWorkDate() != null)
            .filter(r -> !r.getWorkDate().isBefore(startDate) && !r.getWorkDate().isAfter(endDate))
            .collect(Collectors.toList());

        if (records.isEmpty()) {
            return EmployeeMonthlySummary.builder()
                .employeeId(employeeId)
                .month(month)
                .workDates(new HashSet<>())
                .totalHoursWorked(0)
                .totalPayment(BigDecimal.ZERO)
                .build();
        }

        Set<LocalDate> workDates = records.stream()
            .map(WorkRecord::getWorkDate)
            .collect(Collectors.toSet());

        Integer totalHours = records.stream()
            .map(WorkRecord::getHoursWorked)
            .filter(Objects::nonNull)
            .reduce(0, Integer::sum);

        BigDecimal totalPayment = records.stream()
            .map(WorkRecord::getPayment)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EmployeeMonthlySummary.builder()
            .employeeId(employeeId)
            .employeeName(records.get(0).getEmployee().getName())
            .month(month)
            .workDates(workDates)
            .totalHoursWorked(totalHours)
            .totalPayment(totalPayment)
            .build();
    }

    /**
     * Összes alkalmazott havi összesítése
     */
    public List<EmployeeMonthlySummary> getAllEmployeesMonthlySummary(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<WorkRecord> allRecords = workRecordService.findAll().stream()
            .filter(r -> r.getEmployee() != null)
            .filter(r -> r.getWorkDate() != null)
            .filter(r -> !r.getWorkDate().isBefore(startDate) && !r.getWorkDate().isAfter(endDate))
            .collect(Collectors.toList());

        // Csoportosítás alkalmazott szerint
        Map<String, List<WorkRecord>> recordsByEmployee = allRecords.stream()
            .collect(Collectors.groupingBy(r -> r.getEmployee().getId()));

        List<EmployeeMonthlySummary> summaries = new ArrayList<>();

        recordsByEmployee.forEach((employeeId, records) -> {
            Set<LocalDate> workDates = records.stream()
                .map(WorkRecord::getWorkDate)
                .collect(Collectors.toSet());

            Integer totalHours = records.stream()
                .map(WorkRecord::getHoursWorked)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);

            BigDecimal totalPayment = records.stream()
                .map(WorkRecord::getPayment)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            EmployeeMonthlySummary summary = EmployeeMonthlySummary.builder()
                .employeeId(employeeId)
                .employeeName(records.get(0).getEmployee().getName())
                .month(month)
                .workDates(workDates)
                .totalHoursWorked(totalHours)
                .totalPayment(totalPayment)
                .build();

            summaries.add(summary);
        });

        // Rendezés név szerint
        summaries.sort(Comparator.comparing(EmployeeMonthlySummary::getEmployeeName));

        log.info("Generated monthly summary for {} employees in {}", summaries.size(), month);
        return summaries;
    }

    /**
     * Több hónap összesítése egy alkalmazotthoz
     */
    public List<MonthlyWorkSummary> getEmployeeYearlySummary(String employeeId, int year) {
        List<MonthlyWorkSummary> summaries = new ArrayList<>();

        for (int monthValue = 1; monthValue <= 12; monthValue++) {
            YearMonth month = YearMonth.of(year, monthValue);
            EmployeeMonthlySummary monthlySummary = getEmployeeMonthlySummary(employeeId, month);

            MonthlyWorkSummary summary = MonthlyWorkSummary.builder()
                .month(month)
                .employeeId(employeeId)
                .employeeName(monthlySummary.getEmployeeName())
                .totalDaysWorked(monthlySummary.getUniqueDaysWorked())
                .totalHoursWorked(monthlySummary.getTotalHoursWorked())
                .totalPayment(monthlySummary.getTotalPayment())
                .numberOfRecords(0)
                .build();

            summaries.add(summary);
        }

        return summaries;
    }

    /**
     * Napi bontás egy hónapra
     */
    public Map<LocalDate, List<WorkRecord>> getDailyBreakdown(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<WorkRecord> records = workRecordService.findAll().stream()
            .filter(r -> r.getWorkDate() != null)
            .filter(r -> !r.getWorkDate().isBefore(startDate) && !r.getWorkDate().isAfter(endDate))
            .collect(Collectors.toList());

        return records.stream()
            .collect(Collectors.groupingBy(
                WorkRecord::getWorkDate,
                TreeMap::new,
                Collectors.toList()
            ));
    }

    /**
     * Havi statisztikák
     */
    public Map<String, Object> getMonthlyStatistics(YearMonth month) {
        List<EmployeeMonthlySummary> summaries = getAllEmployeesMonthlySummary(month);

        Integer totalDaysWorked = summaries.stream()
            .map(EmployeeMonthlySummary::getUniqueDaysWorked)
            .reduce(0, Integer::sum);

        Integer totalHours = summaries.stream()
            .map(EmployeeMonthlySummary::getTotalHoursWorked)
            .reduce(0, Integer::sum);

        BigDecimal totalPayment = summaries.stream()
            .map(EmployeeMonthlySummary::getTotalPayment)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("month", month);
        statistics.put("numberOfEmployees", summaries.size());
        statistics.put("totalDaysWorked", totalDaysWorked);
        statistics.put("totalHoursWorked", totalHours);
        statistics.put("totalPayment", totalPayment);
        statistics.put("averageHoursPerEmployee", summaries.isEmpty() ? 0 : totalHours / summaries.size());
        statistics.put("averagePaymentPerEmployee",
            summaries.isEmpty() ? BigDecimal.ZERO :
            totalPayment.divide(BigDecimal.valueOf(summaries.size()), 2, BigDecimal.ROUND_HALF_UP));

        return statistics;
    }
}
