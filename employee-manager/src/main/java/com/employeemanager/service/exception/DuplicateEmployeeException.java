package com.employeemanager.service.exception;

/**
 * Kivétel, amely akkor dobódik, ha egy alkalmazott már létezik (adószám vagy TAJ szám alapján)
 */
public class DuplicateEmployeeException extends ServiceException {

    public DuplicateEmployeeException(String field, String value) {
        super("Az alkalmazott már létezik a rendszerben: " + field + " = " + value);
    }

    public DuplicateEmployeeException(String message) {
        super(message);
    }

    public DuplicateEmployeeException(String message, Throwable cause) {
        super(message, cause);
    }
}
