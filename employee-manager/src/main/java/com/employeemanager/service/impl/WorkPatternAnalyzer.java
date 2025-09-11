package com.employeemanager.service.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WarningLevel;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.service.interfaces.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Munkaidő minták elemzésére szolgáló service
 * Figyelmeztetéseket generál túlzott munkavégzés esetén
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkPatternAnalyzer {
    
    private final EmployeeService employeeService;
    
    /**
     * Egy alkalmazott munkaidő mintáinak teljes elemzése
     */
    public WarningAnalysis analyzeEmployee(Employee employee) {
        try {
            // Az EmployeeService-en keresztül kérjük le a munkanaplókat
            // Széles dátum intervallummal, hogy minden rekordot megkapjunk
            LocalDate startDate = LocalDate.of(2000, 1, 1);
            LocalDate endDate = LocalDate.now().plusYears(1);
            
            List<WorkRecord> allRecords = employeeService.getEmployeeMonthlyRecords(
                employee.getId(), startDate, endDate
            );
            
            if (allRecords.isEmpty()) {
                return new WarningAnalysis(WarningLevel.NONE, "");
            }
            
            // Munkanapok kinyerése és rendezése
            List<LocalDate> workDates = allRecords.stream()
                    .map(WorkRecord::getWorkDate)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            
            // Elemzések futtatása
            ConsecutiveDaysResult consecutiveResult = checkConsecutiveWorkDays(workDates);
            MonthlyDaysResult monthlyResult = checkMonthlyWorkDays(workDates);
            YearlyDaysResult yearlyResult = checkYearlyWorkDays(workDates);
            
            // Legmagasabb prioritású figyelmeztetés meghatározása
            WarningLevel highestLevel = WarningLevel.NONE;
            highestLevel = WarningLevel.getHigherPriority(highestLevel, consecutiveResult.level);
            highestLevel = WarningLevel.getHigherPriority(highestLevel, monthlyResult.level);
            highestLevel = WarningLevel.getHigherPriority(highestLevel, yearlyResult.level);
            
            // Üzenet összeállítása
            StringBuilder message = new StringBuilder();
            if (highestLevel != WarningLevel.NONE) {
                message.append("Figyelmeztetés:\n");
                
                if (consecutiveResult.level != WarningLevel.NONE) {
                    message.append("• ").append(consecutiveResult.message).append("\n");
                }
                if (monthlyResult.level != WarningLevel.NONE) {
                    message.append("• ").append(monthlyResult.message).append("\n");
                }
                if (yearlyResult.level != WarningLevel.NONE) {
                    message.append("• ").append(yearlyResult.message).append("\n");
                }
            }
            
            return new WarningAnalysis(highestLevel, message.toString().trim());
            
        } catch (Exception e) {
            log.error("Error analyzing employee work patterns: {}", employee.getName(), e);
            return new WarningAnalysis(WarningLevel.NONE, "");
        }
    }
    
    /**
     * Egymást követő munkanapok ellenőrzése
     */
    private ConsecutiveDaysResult checkConsecutiveWorkDays(List<LocalDate> sortedDates) {
        if (sortedDates.isEmpty()) {
            return new ConsecutiveDaysResult(WarningLevel.NONE, "", 0);
        }
        
        int maxConsecutive = 1;
        int currentConsecutive = 1;
        LocalDate lastDate = sortedDates.get(0);
        
        // Végigmegyünk a rendezett dátumokon
        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate currentDate = sortedDates.get(i);
            long daysBetween = ChronoUnit.DAYS.between(lastDate, currentDate);
            
            if (daysBetween == 1) {
                // Egymást követő napok
                currentConsecutive++;
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            } else {
                // Megszakadt a sorozat
                currentConsecutive = 1;
            }
            
            lastDate = currentDate;
        }
        
        // Ellenőrzés, hogy az aktuális sorozat folytatódik-e ma
        if (!sortedDates.isEmpty()) {
            LocalDate lastWorkDate = sortedDates.get(sortedDates.size() - 1);
            if (lastWorkDate.equals(LocalDate.now()) || lastWorkDate.equals(LocalDate.now().minusDays(1))) {
                // Ha ma vagy tegnap volt az utolsó munkanap, akkor az aktuális sorozatot használjuk
                maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            }
        }
        
        // Figyelmeztetési szint meghatározása
        WarningLevel level = WarningLevel.NONE;
        String message = "";
        
        if (maxConsecutive >= 5) {
            level = WarningLevel.RED;
            message = String.format("%d egymást követő munkanap", maxConsecutive);
        } else if (maxConsecutive == 4) {
            level = WarningLevel.YELLOW;
            message = String.format("%d egymást követő munkanap", maxConsecutive);
        }
        
        return new ConsecutiveDaysResult(level, message, maxConsecutive);
    }
    
    /**
     * Havi munkanapok ellenőrzése
     */
    private MonthlyDaysResult checkMonthlyWorkDays(List<LocalDate> dates) {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();
        
        // Aktuális hónap munkanapjainak számolása
        long monthlyDays = dates.stream()
                .filter(date -> date.getMonthValue() == currentMonth && date.getYear() == currentYear)
                .count();
        
        // Figyelmeztetési szint meghatározása
        WarningLevel level = WarningLevel.NONE;
        String message = "";
        
        if (monthlyDays >= 15) {
            level = WarningLevel.RED;
            message = String.format("%d munkanap ebben a hónapban", monthlyDays);
        } else if (monthlyDays == 14) {
            level = WarningLevel.YELLOW;
            message = String.format("%d munkanap ebben a hónapban", monthlyDays);
        }
        
        return new MonthlyDaysResult(level, message, (int) monthlyDays);
    }
    
    /**
     * Éves munkanapok ellenőrzése
     */
    private YearlyDaysResult checkYearlyWorkDays(List<LocalDate> dates) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        
        // Aktuális év munkanapjainak számolása
        long yearlyDays = dates.stream()
                .filter(date -> date.getYear() == currentYear)
                .count();
        
        // Figyelmeztetési szint meghatározása
        WarningLevel level = WarningLevel.NONE;
        String message = "";
        
        if (yearlyDays >= 90) {
            level = WarningLevel.RED;
            message = String.format("%d munkanap ebben az évben", yearlyDays);
        } else if (yearlyDays == 89) {
            level = WarningLevel.YELLOW;
            message = String.format("%d munkanap ebben az évben", yearlyDays);
        }
        
        return new YearlyDaysResult(level, message, (int) yearlyDays);
    }
    
    /**
     * Összes alkalmazott elemzése
     */
    public Map<String, WarningAnalysis> analyzeAllEmployees(List<Employee> employees) {
        Map<String, WarningAnalysis> results = new HashMap<>();
        
        for (Employee employee : employees) {
            WarningAnalysis analysis = analyzeEmployee(employee);
            results.put(employee.getId(), analysis);
        }
        
        return results;
    }
    
    // Belső osztályok az eredmények tárolására
    
    public static class WarningAnalysis {
        public final WarningLevel level;
        public final String message;
        
        public WarningAnalysis(WarningLevel level, String message) {
            this.level = level;
            this.message = message;
        }
    }
    
    private static class ConsecutiveDaysResult {
        final WarningLevel level;
        final String message;
        ConsecutiveDaysResult(WarningLevel level, String message, int days) {
            this.level = level;
            this.message = message;
        }
    }
    
    private static class MonthlyDaysResult {
        final WarningLevel level;
        final String message;
        MonthlyDaysResult(WarningLevel level, String message, int days) {
            this.level = level;
            this.message = message;
        }
    }
    
    private static class YearlyDaysResult {
        final WarningLevel level;
        final String message;
        YearlyDaysResult(WarningLevel level, String message, int days) {
            this.level = level;
            this.message = message;
        }
    }
}