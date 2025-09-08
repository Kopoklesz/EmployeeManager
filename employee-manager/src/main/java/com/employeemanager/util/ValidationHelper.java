package com.employeemanager.util;

import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class ValidationHelper {

    private static final Pattern TAX_NUMBER_PATTERN = Pattern.compile("\\d{10}");
    private static final Pattern SOCIAL_SECURITY_PATTERN = Pattern.compile("\\d{9}");
    private static final Pattern EBEV_SERIAL_PATTERN = Pattern.compile("\\d{2,}");
    private static final Pattern TIME_PATTERN = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");

    public static boolean isValidTaxNumber(String taxNumber) {
        // Null check hozzáadása
        if (taxNumber == null || taxNumber.trim().isEmpty()) {
            return false;
        }
        return TAX_NUMBER_PATTERN.matcher(taxNumber).matches();
    }

    public static boolean isValidSocialSecurityNumber(String ssn) {
        return SOCIAL_SECURITY_PATTERN.matcher(ssn).matches();
    }

    public static boolean isValidEbevSerial(String serial) {
        return EBEV_SERIAL_PATTERN.matcher(serial).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() >= 2;
    }

    public static boolean isValidBirthDate(LocalDate birthDate) {
        return birthDate != null &&
                birthDate.isBefore(LocalDate.now()) &&
                birthDate.isAfter(LocalDate.now().minusYears(100));
    }

    public static boolean isValidWorkHours(int hours) {
        return hours > 0 && hours <= 24;
    }

    public static boolean isValidPayment(double payment) {
        return payment > 0;
    }

    public static boolean isValidNotificationTime(String time) {
        return time != null && TIME_PATTERN.matcher(time).matches();
    }

    public static LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    public static void setNumberOnlyListener(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    public static void setUpperCaseListener(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            textField.setText(newValue.toUpperCase());
        });
    }

    public static void setTimeFormatListener(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Engedélyezi a számok és : beírását
            if (!newValue.matches("\\d{0,2}:?\\d{0,2}")) {
                textField.setText(oldValue);
                return;
            }

            // Automatikus : beszúrása 2 szám után
            if (newValue.length() == 2 && !newValue.contains(":") && oldValue.length() < 2) {
                textField.setText(newValue + ":");
            }
        });
    }

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
    }

    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isValidWorkDate(LocalDate workDate) {
        return workDate != null && !workDate.isAfter(LocalDate.now());
    }

    public static boolean isValidNotificationDate(LocalDate notificationDate, LocalDate workDate) {
        return notificationDate != null &&
                !notificationDate.isAfter(LocalDate.now()) &&
                (workDate == null || !notificationDate.isAfter(workDate));
    }
}