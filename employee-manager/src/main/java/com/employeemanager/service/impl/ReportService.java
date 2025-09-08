package com.employeemanager.service.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.service.exception.ServiceException;
import com.employeemanager.service.interfaces.EmployeeService;
import com.employeemanager.service.interfaces.WorkRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    private final EmployeeService employeeService;
    private final WorkRecordService workRecordService;
    private static final String REPORTS_DIRECTORY = "reports";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateReport(LocalDate startDate, LocalDate endDate,
                                 boolean includeEmployeeDetails,
                                 boolean includeWorkRecords,
                                 boolean includeSummary) throws ServiceException {
        try {
            StringBuilder report = new StringBuilder();
            report.append("Időszaki jelentés\n");
            report.append("Időszak: ").append(startDate.format(DATE_FORMATTER))
                    .append(" - ").append(endDate.format(DATE_FORMATTER)).append("\n\n");

            if (includeEmployeeDetails) {
                appendEmployeeDetails(report);
            }

            if (includeWorkRecords) {
                appendWorkRecords(report, startDate, endDate);
            }

            if (includeSummary) {
                appendSummary(report, startDate, endDate);
            }

            return saveReport(report.toString(), startDate, endDate);
        } catch (Exception e) {
            log.error("Error generating report", e);
            throw new ServiceException("Failed to generate report", e);
        }
    }

    private void appendEmployeeDetails(StringBuilder report) {
        List<Employee> employees = employeeService.findAll();
        report.append("Alkalmazottak listája (").append(employees.size()).append(" fő)\n");
        report.append("----------------------------------------\n");

        for (Employee employee : employees) {
            report.append("Név: ").append(employee.getName()).append("\n");
            report.append("Adószám: ").append(employee.getTaxNumber()).append("\n");
            report.append("TAJ szám: ").append(employee.getSocialSecurityNumber()).append("\n");
            report.append("Lakcím: ").append(employee.getAddress()).append("\n");
            report.append("----------------------------------------\n");
        }
        report.append("\n");
    }

    private void appendWorkRecords(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        List<WorkRecord> records = workRecordService.getMonthlyRecords(startDate, endDate);
        report.append("Munkanaplók (").append(records.size()).append(" db)\n");
        report.append("----------------------------------------\n");

        for (WorkRecord record : records) {
            report.append("Alkalmazott: ").append(record.getEmployee().getName()).append("\n");
            report.append("Dátum: ").append(record.getWorkDate().format(DATE_FORMATTER)).append("\n");
            report.append("Munkaórák: ").append(record.getHoursWorked()).append("\n");
            report.append("Bérezés: ").append(String.format("%,d Ft", record.getPayment().longValue())).append("\n");
            report.append("----------------------------------------\n");
        }
        report.append("\n");
    }

    private void appendSummary(StringBuilder report, LocalDate startDate, LocalDate endDate) {
        List<WorkRecord> records = workRecordService.getMonthlyRecords(startDate, endDate);

        int totalHours = records.stream()
                .mapToInt(WorkRecord::getHoursWorked)
                .sum();

        long totalPayment = records.stream()
                .mapToLong(r -> r.getPayment().longValue())
                .sum();

        report.append("Összesítés\n");
        report.append("----------------------------------------\n");
        report.append("Összes munkaóra: ").append(totalHours).append(" óra\n");
        report.append("Összes kifizetés: ").append(String.format("%,d Ft", totalPayment)).append("\n");
    }

    private String saveReport(String content, LocalDate startDate, LocalDate endDate) throws ServiceException {
        try {
            createReportsDirectory();

            String fileName = String.format("report_%s_%s.txt",
                    startDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                    endDate.format(DateTimeFormatter.BASIC_ISO_DATE));

            Path reportPath = Paths.get(REPORTS_DIRECTORY, fileName);
            Files.writeString(reportPath, content);

            return reportPath.toString();
        } catch (IOException e) {
            throw new ServiceException("Failed to save report", e);
        }
    }

    private void createReportsDirectory() throws IOException {
        Path directory = Paths.get(REPORTS_DIRECTORY);
        if (!Files.exists(directory)) {
            Files.createDirectory(directory);
        }
    }

    public List<String> getAvailableReports() throws ServiceException {
        try {
            Path directory = Paths.get(REPORTS_DIRECTORY);
            if (!Files.exists(directory)) {
                return List.of();
            }

            return Files.list(directory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ServiceException("Failed to get available reports", e);
        }
    }
}