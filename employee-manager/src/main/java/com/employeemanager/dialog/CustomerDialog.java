package com.employeemanager.dialog;

import com.employeemanager.model.Customer;
import com.employeemanager.service.CustomerService;
import com.employeemanager.util.AlertHelper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vevő létrehozás/szerkesztés dialógus
 *
 * Funkciók:
 * - Vevő alapadatok (név, cím, adószám, email, telefon)
 * - Számlázási cím (ha eltér)
 * - Fizetési határidő napok
 * - Aktív/inaktív státusz
 * - Cég/magánszemély megkülönböztetés
 */
@Slf4j
public class CustomerDialog extends Dialog<Customer> {

    private final CustomerService customerService;
    private final Customer existingCustomer;
    private final boolean isEditMode;

    // Alapadatok
    private final TextField nameField = new TextField();
    private final TextField taxNumberField = new TextField();
    private final TextField euTaxNumberField = new TextField();
    private final TextField zipCodeField = new TextField();
    private final TextField cityField = new TextField();
    private final TextField addressField = new TextField();
    private final ComboBox<String> countryComboBox = new ComboBox<>();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField contactPersonField = new TextField();

    // Számlázási cím
    private final CheckBox useDifferentBillingAddressCheckBox = new CheckBox("Eltérő számlázási cím");
    private final TextField billingAddressField = new TextField();
    private final TextField billingZipCodeField = new TextField();
    private final TextField billingCityField = new TextField();
    private final ComboBox<String> billingCountryComboBox = new ComboBox<>();

    // Egyéb
    private final Spinner<Integer> paymentDeadlineDaysSpinner = new Spinner<>(1, 365, 8);
    private final TextArea notesArea = new TextArea();
    private final CheckBox isActiveCheckBox = new CheckBox("Aktív");
    private final CheckBox isCompanyCheckBox = new CheckBox("Cég");

    public CustomerDialog(CustomerService customerService) {
        this(customerService, null);
    }

    public CustomerDialog(CustomerService customerService, Customer existingCustomer) {
        this.customerService = customerService;
        this.existingCustomer = existingCustomer;
        this.isEditMode = (existingCustomer != null);

        setTitle(isEditMode ? "Vevő szerkesztése" : "Új vevő létrehozása");
        setHeaderText(isEditMode ? "Vevő adatainak módosítása" : "Új vevő hozzáadása");
        setWidth(700);
        setHeight(600);

        setupDialog();

        if (isEditMode) {
            loadCustomer();
        }
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 1. Alapadatok tab
        Tab basicTab = new Tab("Alapadatok");
        basicTab.setContent(createBasicTab());

        // 2. Számlázási cím tab
        Tab billingTab = new Tab("Számlázási cím");
        billingTab.setContent(createBillingTab());

        // 3. Egyéb tab
        Tab otherTab = new Tab("Egyéb");
        otherTab.setContent(createOtherTab());

        tabPane.getTabs().addAll(basicTab, billingTab, otherTab);

        ScrollPane scrollPane = new ScrollPane(tabPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        dialogPane.setContent(scrollPane);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return saveCustomer();
            }
            return null;
        });
    }

    /**
     * Alapadatok tab
     */
    private VBox createBasicTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Név:*"), 0, row);
        grid.add(nameField, 1, row++);
        nameField.setPromptText("Vevő neve");

        grid.add(isCompanyCheckBox, 1, row++);
        isCompanyCheckBox.setSelected(true);

        grid.add(new Label("Adószám:"), 0, row);
        grid.add(taxNumberField, 1, row++);
        taxNumberField.setPromptText("12345678-1-23");

        grid.add(new Label("EU adószám:"), 0, row);
        grid.add(euTaxNumberField, 1, row++);
        euTaxNumberField.setPromptText("HU12345678");

        grid.add(new Label("Ország:"), 0, row);
        countryComboBox.getItems().addAll("Magyarország", "Ausztria", "Németország", "Szlovákia", "Románia", "Egyéb");
        countryComboBox.setValue("Magyarország");
        grid.add(countryComboBox, 1, row++);

        grid.add(new Label("Irányítószám:"), 0, row);
        grid.add(zipCodeField, 1, row++);
        zipCodeField.setPromptText("1234");

        grid.add(new Label("Város:"), 0, row);
        grid.add(cityField, 1, row++);
        cityField.setPromptText("Budapest");

        grid.add(new Label("Cím:"), 0, row);
        grid.add(addressField, 1, row++);
        addressField.setPromptText("Utca, házszám");

        grid.add(new Label("Email:"), 0, row);
        grid.add(emailField, 1, row++);
        emailField.setPromptText("info@example.com");

        grid.add(new Label("Telefon:"), 0, row);
        grid.add(phoneField, 1, row++);
        phoneField.setPromptText("+36 30 123 4567");

        grid.add(new Label("Kapcsolattartó:"), 0, row);
        grid.add(contactPersonField, 1, row++);
        contactPersonField.setPromptText("Kapcsolattartó neve");

        // Mezők szélességének beállítása
        setFieldWidths();

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Számlázási cím tab
     */
    private VBox createBillingTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        Label infoLabel = new Label(
            "Ha a számlázási cím eltér a vevő címétől, add meg az alábbiakat."
        );
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        grid.add(infoLabel, 0, row++, 2, 1);

        grid.add(useDifferentBillingAddressCheckBox, 0, row++, 2, 1);

        // Számlázási cím mezők (csak ha be van pipálva)
        useDifferentBillingAddressCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            billingAddressField.setDisable(!newVal);
            billingZipCodeField.setDisable(!newVal);
            billingCityField.setDisable(!newVal);
            billingCountryComboBox.setDisable(!newVal);
        });

        grid.add(new Label("Számlázási ország:"), 0, row);
        billingCountryComboBox.getItems().addAll("Magyarország", "Ausztria", "Németország", "Szlovákia", "Románia", "Egyéb");
        billingCountryComboBox.setValue("Magyarország");
        billingCountryComboBox.setDisable(true);
        grid.add(billingCountryComboBox, 1, row++);

        grid.add(new Label("Számlázási irányítószám:"), 0, row);
        billingZipCodeField.setDisable(true);
        grid.add(billingZipCodeField, 1, row++);

        grid.add(new Label("Számlázási város:"), 0, row);
        billingCityField.setDisable(true);
        grid.add(billingCityField, 1, row++);

        grid.add(new Label("Számlázási cím:"), 0, row);
        billingAddressField.setDisable(true);
        grid.add(billingAddressField, 1, row++);

        // Mezők szélességének beállítása
        setFieldWidths();

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Egyéb tab
     */
    private VBox createOtherTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Fizetési határidő (nap):"), 0, row);
        paymentDeadlineDaysSpinner.setEditable(true);
        grid.add(paymentDeadlineDaysSpinner, 1, row++);

        grid.add(isActiveCheckBox, 1, row++);
        isActiveCheckBox.setSelected(true);

        grid.add(new Label("Megjegyzések:"), 0, row);
        notesArea.setPrefRowCount(8);
        notesArea.setWrapText(true);
        grid.add(notesArea, 1, row++);

        // Mezők szélességének beállítása
        paymentDeadlineDaysSpinner.setPrefWidth(400);
        notesArea.setPrefWidth(400);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Vevő betöltése (szerkesztés mód)
     */
    private void loadCustomer() {
        if (existingCustomer == null) return;

        nameField.setText(existingCustomer.getName());
        taxNumberField.setText(existingCustomer.getTaxNumber());
        euTaxNumberField.setText(existingCustomer.getEuTaxNumber());
        zipCodeField.setText(existingCustomer.getZipCode());
        cityField.setText(existingCustomer.getCity());
        addressField.setText(existingCustomer.getAddress());
        countryComboBox.setValue(existingCustomer.getCountry() != null ? existingCustomer.getCountry() : "Magyarország");
        emailField.setText(existingCustomer.getEmail());
        phoneField.setText(existingCustomer.getPhone());
        contactPersonField.setText(existingCustomer.getContactPerson());

        // Számlázási cím
        if (existingCustomer.getBillingAddress() != null && !existingCustomer.getBillingAddress().isEmpty()) {
            useDifferentBillingAddressCheckBox.setSelected(true);
            billingAddressField.setText(existingCustomer.getBillingAddress());
            billingZipCodeField.setText(existingCustomer.getBillingZipCode());
            billingCityField.setText(existingCustomer.getBillingCity());
            billingCountryComboBox.setValue(existingCustomer.getBillingCountry() != null ?
                existingCustomer.getBillingCountry() : "Magyarország");
        }

        // Egyéb
        if (existingCustomer.getPaymentDeadlineDays() != null) {
            paymentDeadlineDaysSpinner.getValueFactory().setValue(existingCustomer.getPaymentDeadlineDays());
        }
        notesArea.setText(existingCustomer.getNotes());
        isActiveCheckBox.setSelected(existingCustomer.getIsActive() != null && existingCustomer.getIsActive());
        isCompanyCheckBox.setSelected(existingCustomer.getIsCompany() != null && existingCustomer.getIsCompany());
    }

    /**
     * Vevő mentése
     */
    private Customer saveCustomer() {
        try {
            // Validálás
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                AlertHelper.showWarning("Figyelem", "A név megadása kötelező!");
                return null;
            }

            // Adószám validálás, ha cég
            if (isCompanyCheckBox.isSelected()) {
                String taxNumber = taxNumberField.getText();
                if (taxNumber != null && !taxNumber.trim().isEmpty()) {
                    if (!customerService.isValidTaxNumber(taxNumber)) {
                        AlertHelper.showWarning("Figyelem",
                            "Az adószám formátuma nem megfelelő! (Helyes formátum: 12345678-1-23)");
                        return null;
                    }

                    // Ellenőrzés: már létezik-e ilyen adószámú vevő
                    if (!isEditMode || !taxNumber.equals(existingCustomer.getTaxNumber())) {
                        if (customerService.existsByTaxNumber(taxNumber)) {
                            AlertHelper.showWarning("Figyelem",
                                "Már létezik vevő ezzel az adószámmal!");
                            return null;
                        }
                    }
                }
            }

            // Email validálás
            String email = emailField.getText();
            if (email != null && !email.trim().isEmpty()) {
                if (!customerService.isValidEmail(email)) {
                    AlertHelper.showWarning("Figyelem",
                        "Az email cím formátuma nem megfelelő!");
                    return null;
                }
            }

            // Customer objektum létrehozása/frissítése
            Customer customer = isEditMode ? existingCustomer : new Customer();

            if (!isEditMode) {
                customer.setId(UUID.randomUUID().toString());
                customer.setCreatedAt(LocalDateTime.now());
            }

            customer.setName(nameField.getText().trim());
            customer.setTaxNumber(taxNumberField.getText());
            customer.setEuTaxNumber(euTaxNumberField.getText());
            customer.setZipCode(zipCodeField.getText());
            customer.setCity(cityField.getText());
            customer.setAddress(addressField.getText());
            customer.setCountry(countryComboBox.getValue());
            customer.setEmail(emailField.getText());
            customer.setPhone(phoneField.getText());
            customer.setContactPerson(contactPersonField.getText());

            // Számlázási cím
            if (useDifferentBillingAddressCheckBox.isSelected()) {
                customer.setBillingAddress(billingAddressField.getText());
                customer.setBillingZipCode(billingZipCodeField.getText());
                customer.setBillingCity(billingCityField.getText());
                customer.setBillingCountry(billingCountryComboBox.getValue());
            } else {
                customer.setBillingAddress(null);
                customer.setBillingZipCode(null);
                customer.setBillingCity(null);
                customer.setBillingCountry(null);
            }

            // Egyéb
            customer.setPaymentDeadlineDays(paymentDeadlineDaysSpinner.getValue());
            customer.setNotes(notesArea.getText());
            customer.setIsActive(isActiveCheckBox.isSelected());
            customer.setIsCompany(isCompanyCheckBox.isSelected());

            customer.setUpdatedAt(LocalDateTime.now());

            // Mentés
            Customer savedCustomer;
            if (isEditMode) {
                savedCustomer = customerService.updateCustomer(customer);
                log.info("Customer updated: {}", savedCustomer.getName());
            } else {
                savedCustomer = customerService.createCustomer(customer);
                log.info("Customer created: {}", savedCustomer.getName());
            }

            return savedCustomer;

        } catch (Exception e) {
            log.error("Failed to save customer", e);
            AlertHelper.showError("Hiba", "Nem sikerült menteni a vevőt", e.getMessage());
            return null;
        }
    }

    /**
     * Mezők szélességének beállítása
     */
    private void setFieldWidths() {
        int width = 400;
        nameField.setPrefWidth(width);
        taxNumberField.setPrefWidth(width);
        euTaxNumberField.setPrefWidth(width);
        zipCodeField.setPrefWidth(width);
        cityField.setPrefWidth(width);
        addressField.setPrefWidth(width);
        countryComboBox.setPrefWidth(width);
        emailField.setPrefWidth(width);
        phoneField.setPrefWidth(width);
        contactPersonField.setPrefWidth(width);
        billingAddressField.setPrefWidth(width);
        billingZipCodeField.setPrefWidth(width);
        billingCityField.setPrefWidth(width);
        billingCountryComboBox.setPrefWidth(width);
    }
}
