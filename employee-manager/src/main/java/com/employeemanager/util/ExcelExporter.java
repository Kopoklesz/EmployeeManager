package com.employeemanager.util;

import com.employeemanager.model.fx.WorkRecordFX;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ExcelExporter {

    private static final String EXPORT_DIRECTORY = "exports";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.");

    public String exportWorkRecords(List<WorkRecordFX> records, LocalDate startDate, LocalDate endDate) throws Exception {
        createExportDirectory();

        try (Workbook workbook = new XSSFWorkbook()) {

            // 1. "e-bev" munkalap - részletes lista
            createEbevWorksheet(workbook, records);

            // 2. "dátum szerint" munkalap - dátum szerinti összesítés
            createDateBasedWorksheet(workbook, records);

            // 3. "név szerint" munkalap - dolgozók szerinti csoportosítás
            createEmployeeBasedWorksheet(workbook, records);

            // 4. "ki hány napot dolgozott" munkalap - havi összesítő
            createMonthlySummaryWorksheet(workbook, records, startDate, endDate);

            // Fájl mentése
            String fileName = String.format("munkanaplot_%s_%s.xlsx",
                    startDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")),
                    endDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")));

            Path filePath = Paths.get(EXPORT_DIRECTORY, fileName);
            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                workbook.write(fileOut);
            }

            return filePath.toString();
        }
    }

    /**
     * 1. "e-bev" munkalap - részletes munkanaplók
     */
    private void createEbevWorksheet(Workbook workbook, List<WorkRecordFX> records) {
        Sheet sheet = workbook.createSheet("e-bev");

        // Stílusok
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateBoldStyle = createDateBoldStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle centerStyle = createCenterStyle(workbook);

        // Fejléc sor
        Row headerRow = sheet.createRow(0);
        String[] headers = {"bejelentés dátuma", "bejelentés időpontja", "e-BEV sorszáma",
                "munkavégzés dátuma", "dolgozó neve", "TAJ", "kifizetett összeg", "ledolgozott óra"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Adatok csoportosítása bejelentés szerint (dátum + időpont + e-BEV szám)
        Map<String, List<WorkRecordFX>> groupedRecords = records.stream()
                .collect(Collectors.groupingBy(record ->
                        record.getNotificationDate().toString() + "_" +
                                (record.getNotificationTime() != null ? record.getNotificationTime().toString() : "") + "_" +
                                (record.getEbevSerialNumber() != null ? record.getEbevSerialNumber() : "")
                ));

        int rowNum = 1;

        for (Map.Entry<String, List<WorkRecordFX>> entry : groupedRecords.entrySet()) {
            List<WorkRecordFX> groupRecords = entry.getValue();
            groupRecords.sort(Comparator.comparing(WorkRecordFX::getWorkDate));

            for (int i = 0; i < groupRecords.size(); i++) {
                WorkRecordFX record = groupRecords.get(i);
                Row row = sheet.createRow(rowNum++);

                // Bejelentés adatai csak az első sornál
                if (i == 0) {
                    // Bejelentés dátuma - félkövér
                    Cell notifDateCell = row.createCell(0);
                    notifDateCell.setCellValue(record.getNotificationDate().format(DATE_FORMATTER));
                    notifDateCell.setCellStyle(dateBoldStyle);

                    // Bejelentés időpontja
                    Cell notifTimeCell = row.createCell(1);
                    if (record.getNotificationTime() != null) {
                        notifTimeCell.setCellValue(record.getNotificationTime().toString());
                    }
                    notifTimeCell.setCellStyle(centerStyle);

                    // e-BEV sorszáma
                    Cell ebevCell = row.createCell(2);
                    if (record.getEbevSerialNumber() != null) {
                        ebevCell.setCellValue(record.getEbevSerialNumber());
                    }
                    ebevCell.setCellStyle(centerStyle);
                }

                // Munkavégzés dátuma - félkövér
                Cell workDateCell = row.createCell(3);
                workDateCell.setCellValue(record.getWorkDate().format(DATE_FORMATTER));
                workDateCell.setCellStyle(dateBoldStyle);

                // Dolgozó neve
                Cell nameCell = row.createCell(4);
                nameCell.setCellValue(record.getEmployeeName());

                // TAJ
                Cell tajCell = row.createCell(5);
                if (record.getEmployee() != null && record.getEmployee().getSocialSecurityNumber() != null) {
                    String ssn = record.getEmployee().getSocialSecurityNumber();
                    // Format: 000-000-000
                    if (ssn.length() == 9) {
                        ssn = ssn.substring(0, 3) + "-" + ssn.substring(3, 6) + "-" + ssn.substring(6);
                    }
                    tajCell.setCellValue(ssn);
                }
                tajCell.setCellStyle(centerStyle);

                // Kifizetett összeg
                Cell paymentCell = row.createCell(6);
                paymentCell.setCellValue(record.getPayment().doubleValue());
                paymentCell.setCellStyle(currencyStyle);

                // Ledolgozott óra
                Cell hoursCell = row.createCell(7);
                hoursCell.setCellValue(record.getHoursWorked());
                hoursCell.setCellStyle(centerStyle);
            }
        }

        // Oszlopszélességek beállítása
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 2. "dátum szerint" munkalap - dátum szerinti összesítés
     */
    private void createDateBasedWorksheet(Workbook workbook, List<WorkRecordFX> records) {
        Sheet sheet = workbook.createSheet("dátum szerint");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dateBoldStyle = createDateBoldStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        // Fejléc
        Row headerRow = sheet.createRow(1);
        String[] headers = {"Dátum", "Név", "Összeg", "Napi összeg"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i + 2); // C-F oszlopok
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Adatok csoportosítása dátum szerint
        Map<LocalDate, List<WorkRecordFX>> dateGroups = records.stream()
                .collect(Collectors.groupingBy(
                        WorkRecordFX::getWorkDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        int rowNum = 4; // 5. sortól kezdünk (0-indexelés miatt 4)

        for (Map.Entry<LocalDate, List<WorkRecordFX>> dateEntry : dateGroups.entrySet()) {
            LocalDate date = dateEntry.getKey();
            List<WorkRecordFX> dayRecords = dateEntry.getValue();

            BigDecimal dayTotal = BigDecimal.ZERO;

            for (int i = 0; i < dayRecords.size(); i++) {
                WorkRecordFX record = dayRecords.get(i);
                Row row = sheet.createRow(rowNum++);

                // Dátum csak az első rekordnál - félkövér
                if (i == 0) {
                    Cell dateCell = row.createCell(2);
                    dateCell.setCellValue(date.format(DATE_FORMATTER));
                    dateCell.setCellStyle(dateBoldStyle);
                }

                // Név
                Cell nameCell = row.createCell(3);
                nameCell.setCellValue(record.getEmployeeName());

                // Összeg
                Cell amountCell = row.createCell(4);
                amountCell.setCellValue(record.getPayment().doubleValue());
                amountCell.setCellStyle(currencyStyle);

                dayTotal = dayTotal.add(record.getPayment());
            }

            // Napi összeg az első sorban
            Row firstRow = sheet.getRow(rowNum - dayRecords.size());
            if (firstRow != null) {
                Cell totalCell = firstRow.createCell(5);
                totalCell.setCellValue(dayTotal.doubleValue());
                totalCell.setCellStyle(currencyStyle);
            }

            // Üres sor a dátum után
            rowNum++;
        }

        // Oszlopszélességek
        for (int i = 2; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 3. "név szerint" munkalap - dolgozók szerinti csoportosítás
     */
    private void createEmployeeBasedWorksheet(Workbook workbook, List<WorkRecordFX> records) {
        Sheet sheet = workbook.createSheet("név szerint");

        CellStyle boldStyle = createBoldStyle(workbook);
        CellStyle dateBoldStyle = createDateBoldStyle(workbook);
        CellStyle currencyBoldStyle = createCurrencyBoldStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        // Dolgozók szerinti csoportosítás
        Map<String, List<WorkRecordFX>> employeeGroups = records.stream()
                .collect(Collectors.groupingBy(
                        WorkRecordFX::getEmployeeName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        int rowNum = 3; // 4. sortól kezdünk

        for (Map.Entry<String, List<WorkRecordFX>> employeeEntry : employeeGroups.entrySet()) {
            String employeeName = employeeEntry.getKey();
            List<WorkRecordFX> employeeRecords = employeeEntry.getValue();

            // Ha van dolgozó adat, akkor megjelenítjük a személyes adatokat
            WorkRecordFX firstRecord = employeeRecords.get(0);
            if (firstRecord.getEmployee() != null) {
                // Név - félkövér
                Row nameRow = sheet.createRow(rowNum++);
                Cell nameLabel = nameRow.createCell(1);
                nameLabel.setCellValue("név:");
                nameLabel.setCellStyle(boldStyle);

                Cell nameValue = nameRow.createCell(2);
                nameValue.setCellValue(employeeName);
                nameValue.setCellStyle(boldStyle);

                // Anyja neve
                if (firstRecord.getEmployee().getMotherName() != null) {
                    Row motherRow = sheet.createRow(rowNum++);
                    Cell motherLabel = motherRow.createCell(1);
                    motherLabel.setCellValue("anyja neve:");
                    motherLabel.setCellStyle(boldStyle);

                    Cell motherValue = motherRow.createCell(2);
                    motherValue.setCellValue(firstRecord.getEmployee().getMotherName());
                }

                // Születési hely, idő
                if (firstRecord.getEmployee().getBirthPlace() != null || firstRecord.getEmployee().getBirthDate() != null) {
                    Row birthRow = sheet.createRow(rowNum++);
                    Cell birthLabel = birthRow.createCell(1);
                    birthLabel.setCellValue("szül.hely, idő:");
                    birthLabel.setCellStyle(boldStyle);

                    Cell birthValue = birthRow.createCell(2);
                    String birthInfo = "";
                    if (firstRecord.getEmployee().getBirthPlace() != null) {
                        birthInfo += firstRecord.getEmployee().getBirthPlace();
                    }
                    if (firstRecord.getEmployee().getBirthDate() != null) {
                        if (!birthInfo.isEmpty()) birthInfo += ", ";
                        birthInfo += firstRecord.getEmployee().getBirthDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd."));
                    }
                    birthValue.setCellValue(birthInfo);
                }

                // Adóazonosító
                if (firstRecord.getEmployee().getTaxNumber() != null) {
                    Row taxRow = sheet.createRow(rowNum++);
                    Cell taxLabel = taxRow.createCell(1);
                    taxLabel.setCellValue("adóazonosító:");
                    taxLabel.setCellStyle(boldStyle);

                    Cell taxValue = taxRow.createCell(2);
                    taxValue.setCellValue(firstRecord.getEmployee().getTaxNumber());
                }

                // TAJ szám
                if (firstRecord.getEmployee().getSocialSecurityNumber() != null) {
                    Row tajRow = sheet.createRow(rowNum++);
                    Cell tajLabel = tajRow.createCell(1);
                    tajLabel.setCellValue("TAJ szám:");
                    tajLabel.setCellStyle(boldStyle);

                    Cell tajValue = tajRow.createCell(2);
                    String ssn = firstRecord.getEmployee().getSocialSecurityNumber();
                    if (ssn.length() == 9) {
                        ssn = ssn.substring(0, 3) + "-" + ssn.substring(3, 6) + "-" + ssn.substring(6);
                    }
                    tajValue.setCellValue(ssn);
                }

                // Lakcím
                if (firstRecord.getEmployee().getAddress() != null) {
                    Row addressRow = sheet.createRow(rowNum++);
                    Cell addressLabel = addressRow.createCell(1);
                    addressLabel.setCellValue("lakcím:");
                    addressLabel.setCellStyle(boldStyle);

                    Cell addressValue = addressRow.createCell(2);
                    addressValue.setCellValue(firstRecord.getEmployee().getAddress());
                }
            }

            rowNum++; // Üres sor

            // Munkanaplók táblázata
            employeeRecords.sort(Comparator.comparing(WorkRecordFX::getWorkDate));

            int totalHours = 0;
            BigDecimal totalPayment = BigDecimal.ZERO;

            for (WorkRecordFX record : employeeRecords) {
                Row workRow = sheet.createRow(rowNum++);

                // Dátum - félkövér
                Cell dateCell = workRow.createCell(1);
                dateCell.setCellValue(record.getWorkDate().format(DATE_FORMATTER));
                dateCell.setCellStyle(dateBoldStyle);

                // Órák
                Cell hoursCell = workRow.createCell(2);
                hoursCell.setCellValue(record.getHoursWorked());

                // Összeg
                Cell amountCell = workRow.createCell(3);
                amountCell.setCellValue(record.getPayment().doubleValue());
                amountCell.setCellStyle(currencyStyle);

                totalHours += record.getHoursWorked();
                totalPayment = totalPayment.add(record.getPayment());
            }

            // Összesítő sor - félkövér
            Row totalRow = sheet.createRow(rowNum++);
            Cell totalHoursCell = totalRow.createCell(2);
            totalHoursCell.setCellValue(totalHours);
            totalHoursCell.setCellStyle(boldStyle);

            Cell totalAmountCell = totalRow.createCell(3);
            totalAmountCell.setCellValue(totalPayment.doubleValue());
            totalAmountCell.setCellStyle(currencyBoldStyle);

            rowNum += 2; // Üres sorok a következő dolgozó előtt
        }

        // Oszlopszélességek - specifikus szélességekkel a ##### elkerülésére
        sheet.setColumnWidth(1, 4000); // Dátum oszlop
        sheet.setColumnWidth(2, 3000); // Órák oszlop
        sheet.setColumnWidth(3, 6000); // Összeg oszlop (szélesebb a pénzösszegnek)
    }

    /**
     * 4. "ki hány napot dolgozott" munkalap - havi összesítő
     */
    private void createMonthlySummaryWorksheet(Workbook workbook, List<WorkRecordFX> records,
                                               LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("ki hány napot dolgozott");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyBoldStyle = createCurrencyBoldStyle(workbook);
        CellStyle topDashedBorderStyle = createTopDashedBorderStyle(workbook);
        CellStyle bottomDashedBorderStyle = createBottomDashedBorderStyle(workbook);

        // Fejléc sorok
        Row row1 = sheet.createRow(1);
        Cell nameHeader = row1.createCell(1);
        nameHeader.setCellValue("név");
        nameHeader.setCellStyle(headerStyle);

        // Hónapok fejléce
        LocalDate currentMonth = LocalDate.of(startDate.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(endDate.getYear(), 12, 31);

        int col = 2;
        while (!currentMonth.isAfter(yearEnd)) {
            Cell monthCell = row1.createCell(col);
            monthCell.setCellValue(currentMonth.format(YEAR_MONTH_FORMATTER));
            monthCell.setCellStyle(headerStyle);
            col++;
            currentMonth = currentMonth.plusMonths(1);
        }

        // Összesen oszlopok
        Cell totalPaymentHeader = row1.createCell(col);
        totalPaymentHeader.setCellValue("bérkifizetés összesen");
        totalPaymentHeader.setCellStyle(headerStyle);

        Cell totalDaysHeader = row1.createCell(col + 1);
        totalDaysHeader.setCellValue("munkanapok összesen");
        totalDaysHeader.setCellStyle(headerStyle);

        // Dolgozók szerinti csoportosítás
        Map<String, List<WorkRecordFX>> employeeGroups = records.stream()
                .collect(Collectors.groupingBy(WorkRecordFX::getEmployeeName));

        int rowNum = 4; // 5. sortól kezdünk az adatokkal

        for (Map.Entry<String, List<WorkRecordFX>> employeeEntry : employeeGroups.entrySet()) {
            String employeeName = employeeEntry.getKey();
            List<WorkRecordFX> employeeRecords = employeeEntry.getValue();

            Row dataRow = sheet.createRow(rowNum);
            Row daysRow = sheet.createRow(rowNum + 1);

            // Név - felső szaggatott keret
            Cell nameCell = dataRow.createCell(1);
            nameCell.setCellValue(employeeName);
            nameCell.setCellStyle(topDashedBorderStyle);

            Cell emptyNameCell = daysRow.createCell(1);
            emptyNameCell.setCellStyle(bottomDashedBorderStyle);

            // Havi adatok számítása
            Map<String, BigDecimal> monthlyPayments = new HashMap<>();
            Map<String, Integer> monthlyDays = new HashMap<>();

            for (WorkRecordFX record : employeeRecords) {
                String monthKey = record.getWorkDate().format(YEAR_MONTH_FORMATTER);

                monthlyPayments.merge(monthKey, record.getPayment(), BigDecimal::add);
                monthlyDays.merge(monthKey, 1, Integer::sum);
            }

            // Havi oszlopok kitöltése
            currentMonth = LocalDate.of(startDate.getYear(), 1, 1);
            col = 2;
            BigDecimal totalPayment = BigDecimal.ZERO;
            int totalDays = 0;

            while (!currentMonth.isAfter(yearEnd)) {
                String monthKey = currentMonth.format(YEAR_MONTH_FORMATTER);

                Cell paymentCell = dataRow.createCell(col);
                Cell daysCell = daysRow.createCell(col);

                // Felső sor: felső keret, alsó sor: alsó keret
                paymentCell.setCellStyle(topDashedBorderStyle);
                daysCell.setCellStyle(bottomDashedBorderStyle);

                if (monthlyPayments.containsKey(monthKey)) {
                    BigDecimal monthPayment = monthlyPayments.get(monthKey);
                    paymentCell.setCellValue(monthPayment.doubleValue());

                    int monthDays = monthlyDays.get(monthKey);
                    daysCell.setCellValue(monthDays);

                    totalPayment = totalPayment.add(monthPayment);
                    totalDays += monthDays;
                }

                col++;
                currentMonth = currentMonth.plusMonths(1);
            }

            // Összesen oszlopok - szaggatott keret továbbra is
            Cell totalPaymentCell = dataRow.createCell(col);
            totalPaymentCell.setCellValue(totalPayment.doubleValue());
            totalPaymentCell.setCellStyle(currencyBoldStyle);
            // Felső keret hozzáadása a bérkifizetés összesen-hez
            CellStyle currencyBoldTopStyle = workbook.createCellStyle();
            currencyBoldTopStyle.cloneStyleFrom(currencyBoldStyle);
            currencyBoldTopStyle.setBorderTop(BorderStyle.DASHED);
            totalPaymentCell.setCellStyle(currencyBoldTopStyle);

            Cell totalDaysCell = dataRow.createCell(col + 1);
            totalDaysCell.setCellValue(totalDays);
            totalDaysCell.setCellStyle(topDashedBorderStyle);

            // Üres cellák a napok sorában az összesen oszlopokhoz - alsó keret
            Cell emptyPaymentCell = daysRow.createCell(col);
            emptyPaymentCell.setCellStyle(bottomDashedBorderStyle);

            Cell emptyDaysCell = daysRow.createCell(col + 1);
            emptyDaysCell.setCellStyle(bottomDashedBorderStyle);

            rowNum += 2; // Következő dolgozó
        }

        // Oszlopszélességek
        sheet.setColumnWidth(1, 5000); // Név oszlop szélesebb
        for (int i = 2; i < col + 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Segéd metódusok a stílusokhoz
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDateBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy.mm.dd"));
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0 \"Ft\""));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createCurrencyBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0 \"Ft\""));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createCenterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createTopDashedBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.DASHED);
        // Csak felső keret
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBottomDashedBorderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.DASHED);
        // Csak alsó keret
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createExportDirectory() throws Exception {
        Path directory = Paths.get(EXPORT_DIRECTORY);
        if (!Files.exists(directory)) {
            Files.createDirectory(directory);
        }
    }
}