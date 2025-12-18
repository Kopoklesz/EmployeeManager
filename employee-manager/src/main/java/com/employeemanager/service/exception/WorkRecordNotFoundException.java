package com.employeemanager.service.exception;

/**
 * Kivétel, amely akkor dobódik, ha egy munkanapló nem található
 */
public class WorkRecordNotFoundException extends ServiceException {

    public WorkRecordNotFoundException(String id) {
        super("Munkanapló nem található: " + id);
    }

    public WorkRecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
