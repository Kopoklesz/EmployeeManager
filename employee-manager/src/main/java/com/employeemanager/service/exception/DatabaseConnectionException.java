package com.employeemanager.service.exception;

/**
 * Kivétel, amely adatbázis kapcsolati hibák esetén dobódik
 */
public class DatabaseConnectionException extends ServiceException {

    public DatabaseConnectionException(String message) {
        super(message);
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
