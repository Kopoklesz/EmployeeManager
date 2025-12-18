package com.employeemanager.service.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Kivétel, amely validációs hibák esetén dobódik
 */
public class ValidationException extends ServiceException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        super(message);
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(message);
    }

    public ValidationException(List<String> errors) {
        super("Validációs hibák: " + String.join(", ", errors));
        this.validationErrors = new ArrayList<>(errors);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = new ArrayList<>();
        this.validationErrors.add(message);
    }

    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }

    public void addValidationError(String error) {
        this.validationErrors.add(error);
    }
}
