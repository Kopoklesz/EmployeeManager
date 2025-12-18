package com.employeemanager.dialog;

import com.employeemanager.model.fx.EmployeeFX;
import com.employeemanager.util.ValidationHelper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EmployeeDialog extends Dialog<EmployeeFX> {

    private final TextField nameField = new TextField();
    private final TextField birthPlaceField = new TextField();
    private final DatePicker birthDatePicker = new DatePicker();
    private final TextField motherNameField = new TextField();
    private final TextField taxNumberField = new TextField();
    private final TextField socialSecurityField = new TextField();
    private final TextField addressField = new TextField();

    private final EmployeeFX employee;

    public EmployeeDialog() {
        this(new EmployeeFX());
    }

    public EmployeeDialog(EmployeeFX employee) {
        this.employee = employee;

        setTitle(employee.getId() == null ? "Új alkalmazott" : "Alkalmazott szerkesztése");
        setHeaderText("Kérem, adja meg az alkalmazott adatait:");

        setupDialog();
        populateFields();
        setupValidation();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Név:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Születési hely:"), 0, 1);
        grid.add(birthPlaceField, 1, 1);
        grid.add(new Label("Születési idő:"), 0, 2);
        grid.add(birthDatePicker, 1, 2);
        grid.add(new Label("Anyja neve:"), 0, 3);
        grid.add(motherNameField, 1, 3);
        grid.add(new Label("Adószám:"), 0, 4);
        grid.add(taxNumberField, 1, 4);
        grid.add(new Label("TAJ szám:"), 0, 5);
        grid.add(socialSecurityField, 1, 5);
        grid.add(new Label("Lakcím:"), 0, 6);
        grid.add(addressField, 1, 6);

        dialogPane.setContent(grid);

        // Mezők szélességének beállítása
        nameField.setPrefWidth(300);
        birthPlaceField.setPrefWidth(300);
        birthDatePicker.setPrefWidth(300);
        motherNameField.setPrefWidth(300);
        taxNumberField.setPrefWidth(300);
        socialSecurityField.setPrefWidth(300);
        addressField.setPrefWidth(300);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return createEmployeeFromFields();
            }
            return null;
        });

        // OK gomb engedélyezése/tiltása a validáció alapján
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        birthDatePicker.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                try {
                    return string != null && !string.isEmpty() ? LocalDate.parse(string, formatter) : null;
                } catch (Exception e) {
                    return null;
                }
            }
        });

        birthDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    private void populateFields() {
        nameField.setText(employee.getName());
        birthPlaceField.setText(employee.getBirthPlace());
        birthDatePicker.setValue(employee.getBirthDate());
        motherNameField.setText(employee.getMotherName());
        taxNumberField.setText(employee.getTaxNumber());
        socialSecurityField.setText(employee.getSocialSecurityNumber());
        addressField.setText(employee.getAddress());
    }

    private void setupValidation() {
        // Csak szám az adószámhoz és TAJ számhoz
        ValidationHelper.setNumberOnlyListener(taxNumberField);
        ValidationHelper.setNumberOnlyListener(socialSecurityField);

        // Validáció az OK gombhoz
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);

        Runnable validateFields = () -> {
            try {
                String taxNumberText = taxNumberField.getText();
                String ssnText = socialSecurityField.getText();
                String nameText = nameField.getText();
                LocalDate birthDate = birthDatePicker.getValue();

                boolean isValid = nameText != null && ValidationHelper.isValidName(nameText) &&
                        taxNumberText != null && ValidationHelper.isValidTaxNumber(taxNumberText) &&
                        ssnText != null && ValidationHelper.isValidSocialSecurityNumber(ssnText) &&
                        birthDate != null && ValidationHelper.isValidBirthDate(birthDate);

                okButton.setDisable(!isValid);
            } catch (Exception e) {
                // Ha bármilyen hiba történik a validáció során, letiltjuk az OK gombot
                okButton.setDisable(true);
            }
        };

        // Figyelők beállítása
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        birthDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        taxNumberField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());
        socialSecurityField.textProperty().addListener((obs, oldVal, newVal) -> validateFields.run());

        // Kezdeti validáció
        validateFields.run();
    }

    private EmployeeFX createEmployeeFromFields() {
        EmployeeFX result = new EmployeeFX();
        result.setId(employee.getId());
        result.setName(nameField.getText());
        result.setBirthPlace(birthPlaceField.getText());

        LocalDate birthDate = birthDatePicker.getValue();
        result.setBirthDate(birthDate);

        result.setMotherName(motherNameField.getText());
        result.setTaxNumber(taxNumberField.getText());
        result.setSocialSecurityNumber(socialSecurityField.getText());
        result.setAddress(addressField.getText());

        return result;
    }
}