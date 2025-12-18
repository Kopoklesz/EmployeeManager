package com.employeemanager.service.exception;

/**
 * Kivétel, amely akkor dobódik, ha egy alkalmazott nem található
 */
public class EmployeeNotFoundException extends ServiceException {

    public EmployeeNotFoundException(String id) {
        super("Alkalmazott nem található: " + id);
    }

    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
