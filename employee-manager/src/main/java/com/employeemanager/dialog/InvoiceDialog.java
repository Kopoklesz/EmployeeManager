package com.employeemanager.dialog;

import com.employeemanager.model.Customer;
import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.model.CompanySettings;
import com.employeemanager.service.CustomerService;
import com.employeemanager.service.InvoiceService;
import com.employeemanager.repository.CompanySettingsRepository;
import com.employeemanager.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Számla létrehozás/szerkesztés dialógus
 *
 * Funkciók:
 * - Vevő választás
 * - Számla alapadatok (dátumok, fizetési mód, pénznem)
 * - Tételek kezelése (hozzáadás, törlés, szerkesztés)
 * - Automatikus összegszámítás
 * - Fordított adózás, pénzforgalmi elszámolás
 * - Megjegyzések, lábléc
 */
@Slf4j
public class InvoiceDialog extends Dialog<Invoice> {

    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final CompanySettingsRepository settingsRepository;
    private final Invoice existingInvoice;
    private final boolean isEditMode;

    // Számla adatok
    private final ComboBox<Customer> customerComboBox = new ComboBox<>();
    private final DatePicker invoiceDatePicker = new DatePicker();
    private final DatePicker deliveryDatePicker = new DatePicker();
    private final DatePicker paymentDeadlinePicker = new DatePicker();
    private final ComboBox<Invoice.PaymentMethod> paymentMethodComboBox = new ComboBox<>();
    private final ComboBox<String> currencyComboBox = new ComboBox<>();
    private final Spinner<Double> exchangeRateSpinner = new Spinner<>(0.01, 10000.0, 1.0, 0.01);
    private final CheckBox isReverseChargeCheckBox = new CheckBox("Fordított adózás");
    private final CheckBox isCashAccountingCheckBox = new CheckBox("Pénzforgalmi elszámolás");

    // Tételek
    private final TableView<InvoiceItem> itemsTable = new TableView<>();
    private final ObservableList<InvoiceItem> items = FXCollections.observableArrayList();

    // Összesítő
    private final Label netAmountLabel = new Label("0 HUF");
    private final Label vatAmountLabel = new Label("0 HUF");
    private final Label grossAmountLabel = new Label("0 HUF");

    // Megjegyzések
    private final TextArea notesArea = new TextArea();
    private final TextArea footerTextArea = new TextArea();

    public InvoiceDialog(CustomerService customerService, InvoiceService invoiceService,
                         CompanySettingsRepository settingsRepository) {
        this(customerService, invoiceService, settingsRepository, null);
    }

    public InvoiceDialog(CustomerService customerService, InvoiceService invoiceService,
                         CompanySettingsRepository settingsRepository, Invoice existingInvoice) {
        this.customerService = customerService;
        this.invoiceService = invoiceService;
        this.settingsRepository = settingsRepository;
        this.existingInvoice = existingInvoice;
        this.isEditMode = (existingInvoice != null);

        setTitle(isEditMode ? "Számla szerkesztése" : "Új számla létrehozása");
        setHeaderText(isEditMode ? "Számla adatainak módosítása" : "Új számla kiállítása");
        setWidth(900);
        setHeight(700);

        setupDialog();

        if (isEditMode) {
            loadInvoice();
        } else {
            setDefaults();
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

        // 2. Tételek tab
        Tab itemsTab = new Tab("Tételek");
        itemsTab.setContent(createItemsTab());

        // 3. Megjegyzések tab
        Tab notesTab = new Tab("Megjegyzések");
        notesTab.setContent(createNotesTab());

        tabPane.getTabs().addAll(basicTab, itemsTab, notesTab);

        ScrollPane scrollPane = new ScrollPane(tabPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        dialogPane.setContent(scrollPane);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return saveInvoice();
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

        // Vevő választás
        grid.add(new Label("Vevő:*"), 0, row);
        try {
            List<Customer> customers = customerService.findByIsActive(true);
            customerComboBox.setItems(FXCollections.observableArrayList(customers));
            customerComboBox.setConverter(new javafx.util.StringConverter<Customer>() {
                @Override
                public String toString(Customer customer) {
                    return customer != null ? customer.getName() : "";
                }

                @Override
                public Customer fromString(String string) {
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Failed to load customers", e);
        }

        HBox customerBox = new HBox(10);
        customerBox.getChildren().add(customerComboBox);
        Button newCustomerButton = new Button("+");
        newCustomerButton.setOnAction(e -> createNewCustomer());
        customerBox.getChildren().add(newCustomerButton);

        grid.add(customerBox, 1, row++);

        // Dátumok
        grid.add(new Label("Kiállítás dátuma:*"), 0, row);
        grid.add(invoiceDatePicker, 1, row++);

        grid.add(new Label("Teljesítés dátuma:*"), 0, row);
        grid.add(deliveryDatePicker, 1, row++);

        grid.add(new Label("Fizetési határidő:*"), 0, row);
        grid.add(paymentDeadlinePicker, 1, row++);

        // Fizetési mód
        grid.add(new Label("Fizetési mód:*"), 0, row);
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(Invoice.PaymentMethod.values()));
        paymentMethodComboBox.setConverter(new javafx.util.StringConverter<Invoice.PaymentMethod>() {
            @Override
            public String toString(Invoice.PaymentMethod method) {
                return method != null ? method.getDisplayName() : "";
            }

            @Override
            public Invoice.PaymentMethod fromString(String string) {
                return null;
            }
        });
        grid.add(paymentMethodComboBox, 1, row++);

        // Pénznem és árfolyam
        grid.add(new Label("Pénznem:*"), 0, row);
        currencyComboBox.getItems().addAll("HUF", "EUR", "USD");
        grid.add(currencyComboBox, 1, row++);

        grid.add(new Label("Árfolyam:"), 0, row);
        exchangeRateSpinner.setEditable(true);
        grid.add(exchangeRateSpinner, 1, row++);

        // Speciális jelzések
        grid.add(isReverseChargeCheckBox, 1, row++);
        grid.add(isCashAccountingCheckBox, 1, row++);

        // Mezők szélességének beállítása
        customerComboBox.setPrefWidth(350);
        invoiceDatePicker.setPrefWidth(400);
        deliveryDatePicker.setPrefWidth(400);
        paymentDeadlinePicker.setPrefWidth(400);
        paymentMethodComboBox.setPrefWidth(400);
        currencyComboBox.setPrefWidth(400);
        exchangeRateSpinner.setPrefWidth(400);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Tételek tab
     */
    private VBox createItemsTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        // Tételek táblázat
        itemsTable.setItems(items);
        itemsTable.setPrefHeight(350);

        TableColumn<InvoiceItem, Integer> lineNumberCol = new TableColumn<>("Sor");
        lineNumberCol.setCellValueFactory(new PropertyValueFactory<>("lineNumber"));
        lineNumberCol.setPrefWidth(50);

        TableColumn<InvoiceItem, String> descriptionCol = new TableColumn<>("Megnevezés");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setPrefWidth(200);

        TableColumn<InvoiceItem, BigDecimal> quantityCol = new TableColumn<>("Menny.");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(70);

        TableColumn<InvoiceItem, String> unitCol = new TableColumn<>("ME");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(60);

        TableColumn<InvoiceItem, BigDecimal> unitPriceCol = new TableColumn<>("Egységár");
        unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        unitPriceCol.setPrefWidth(100);

        TableColumn<InvoiceItem, BigDecimal> vatRateCol = new TableColumn<>("ÁFA%");
        vatRateCol.setCellValueFactory(new PropertyValueFactory<>("vatRate"));
        vatRateCol.setPrefWidth(70);

        TableColumn<InvoiceItem, BigDecimal> netAmountCol = new TableColumn<>("Nettó");
        netAmountCol.setCellValueFactory(new PropertyValueFactory<>("netAmount"));
        netAmountCol.setPrefWidth(100);

        TableColumn<InvoiceItem, BigDecimal> grossAmountCol = new TableColumn<>("Bruttó");
        grossAmountCol.setCellValueFactory(new PropertyValueFactory<>("grossAmount"));
        grossAmountCol.setPrefWidth(100);

        itemsTable.getColumns().addAll(lineNumberCol, descriptionCol, quantityCol, unitCol,
            unitPriceCol, vatRateCol, netAmountCol, grossAmountCol);

        vbox.getChildren().add(itemsTable);

        // Gombok
        HBox buttonBox = new HBox(10);
        Button addButton = new Button("Tétel hozzáadása");
        addButton.setOnAction(e -> addItem());
        Button editButton = new Button("Szerkesztés");
        editButton.setOnAction(e -> editItem());
        Button deleteButton = new Button("Törlés");
        deleteButton.setOnAction(e -> deleteItem());
        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);

        vbox.getChildren().add(buttonBox);

        // Összesítő
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(10);
        summaryGrid.setVgap(10);
        summaryGrid.setPadding(new Insets(10));

        int row = 0;
        summaryGrid.add(new Label("Nettó összesen:"), 0, row);
        netAmountLabel.setStyle("-fx-font-weight: bold;");
        summaryGrid.add(netAmountLabel, 1, row++);

        summaryGrid.add(new Label("ÁFA összesen:"), 0, row);
        vatAmountLabel.setStyle("-fx-font-weight: bold;");
        summaryGrid.add(vatAmountLabel, 1, row++);

        summaryGrid.add(new Label("Bruttó összesen:"), 0, row);
        grossAmountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        summaryGrid.add(grossAmountLabel, 1, row++);

        vbox.getChildren().add(summaryGrid);

        return vbox;
    }

    /**
     * Megjegyzések tab
     */
    private VBox createNotesTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Megjegyzések:"), 0, row);
        notesArea.setPrefRowCount(6);
        notesArea.setWrapText(true);
        grid.add(notesArea, 1, row++);

        grid.add(new Label("Lábléc szöveg:"), 0, row);
        footerTextArea.setPrefRowCount(6);
        footerTextArea.setWrapText(true);
        grid.add(footerTextArea, 1, row++);

        // Mezők szélességének beállítása
        notesArea.setPrefWidth(600);
        footerTextArea.setPrefWidth(600);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Alapértelmezett értékek beállítása (új számla esetén)
     */
    private void setDefaults() {
        try {
            CompanySettings settings = settingsRepository.get();

            invoiceDatePicker.setValue(LocalDate.now());
            deliveryDatePicker.setValue(LocalDate.now());

            int paymentDeadlineDays = settings.getDefaultPaymentDeadlineDays() != null ?
                settings.getDefaultPaymentDeadlineDays() : 8;
            paymentDeadlinePicker.setValue(LocalDate.now().plusDays(paymentDeadlineDays));

            paymentMethodComboBox.setValue(Invoice.PaymentMethod.BANK_TRANSFER);
            currencyComboBox.setValue(settings.getDefaultCurrency() != null ?
                settings.getDefaultCurrency() : "HUF");
            exchangeRateSpinner.getValueFactory().setValue(1.0);

            footerTextArea.setText(settings.getInvoiceFooterText());

        } catch (Exception e) {
            log.error("Failed to load default settings", e);
        }
    }

    /**
     * Számla betöltése (szerkesztés mód)
     */
    private void loadInvoice() {
        if (existingInvoice == null) return;

        customerComboBox.setValue(existingInvoice.getCustomer());
        invoiceDatePicker.setValue(existingInvoice.getInvoiceDate());
        deliveryDatePicker.setValue(existingInvoice.getDeliveryDate());
        paymentDeadlinePicker.setValue(existingInvoice.getPaymentDeadline());
        paymentMethodComboBox.setValue(existingInvoice.getPaymentMethod());
        currencyComboBox.setValue(existingInvoice.getCurrency());
        exchangeRateSpinner.getValueFactory().setValue(
            existingInvoice.getExchangeRate() != null ?
                existingInvoice.getExchangeRate().doubleValue() : 1.0
        );
        isReverseChargeCheckBox.setSelected(
            existingInvoice.getIsReverseCharge() != null && existingInvoice.getIsReverseCharge()
        );
        isCashAccountingCheckBox.setSelected(
            existingInvoice.getIsCashAccounting() != null && existingInvoice.getIsCashAccounting()
        );

        if (existingInvoice.getItems() != null) {
            items.addAll(existingInvoice.getItems());
        }

        notesArea.setText(existingInvoice.getNotes());
        footerTextArea.setText(existingInvoice.getFooterText());

        updateSummary();
    }

    /**
     * Új tétel hozzáadása
     */
    private void addItem() {
        InvoiceItemDialog dialog = new InvoiceItemDialog(settingsRepository);
        dialog.showAndWait().ifPresent(item -> {
            item.setLineNumber(items.size() + 1);
            items.add(item);
            updateSummary();
        });
    }

    /**
     * Tétel szerkesztése
     */
    private void editItem() {
        InvoiceItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            AlertHelper.showWarning("Figyelem", "Válassz ki egy tételt a szerkesztéshez!");
            return;
        }

        InvoiceItemDialog dialog = new InvoiceItemDialog(settingsRepository, selectedItem);
        dialog.showAndWait().ifPresent(item -> {
            int index = items.indexOf(selectedItem);
            items.set(index, item);
            updateSummary();
        });
    }

    /**
     * Tétel törlése
     */
    private void deleteItem() {
        InvoiceItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            AlertHelper.showWarning("Figyelem", "Válassz ki egy tételt a törléshez!");
            return;
        }

        boolean confirmed = AlertHelper.showConfirmation("Megerősítés",
            "Biztosan törölni szeretnéd ezt a tételt?",
            selectedItem.getDescription());

        if (confirmed) {
            items.remove(selectedItem);
            // Sorszámok frissítése
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setLineNumber(i + 1);
            }
            updateSummary();
        }
    }

    /**
     * Összesítő frissítése
     */
    private void updateSummary() {
        BigDecimal netTotal = items.stream()
            .map(InvoiceItem::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vatTotal = items.stream()
            .map(InvoiceItem::getVatAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossTotal = items.stream()
            .map(InvoiceItem::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = currencyComboBox.getValue() != null ? currencyComboBox.getValue() : "HUF";

        netAmountLabel.setText(String.format("%,.0f %s", netTotal.doubleValue(), currency));
        vatAmountLabel.setText(String.format("%,.0f %s", vatTotal.doubleValue(), currency));
        grossAmountLabel.setText(String.format("%,.0f %s", grossTotal.doubleValue(), currency));
    }

    /**
     * Új vevő létrehozása
     */
    private void createNewCustomer() {
        CustomerDialog dialog = new CustomerDialog(customerService);
        dialog.showAndWait().ifPresent(customer -> {
            // Frissítjük a vevő listát
            try {
                List<Customer> customers = customerService.findByIsActive(true);
                customerComboBox.setItems(FXCollections.observableArrayList(customers));
                customerComboBox.setValue(customer);
            } catch (Exception e) {
                log.error("Failed to reload customers", e);
            }
        });
    }

    /**
     * Számla mentése
     */
    private Invoice saveInvoice() {
        try {
            // Validálás
            if (customerComboBox.getValue() == null) {
                AlertHelper.showWarning("Figyelem", "Válassz ki egy vevőt!");
                return null;
            }

            if (invoiceDatePicker.getValue() == null || deliveryDatePicker.getValue() == null ||
                paymentDeadlinePicker.getValue() == null) {
                AlertHelper.showWarning("Figyelem", "Minden dátum megadása kötelező!");
                return null;
            }

            if (items.isEmpty()) {
                AlertHelper.showWarning("Figyelem", "Adj hozzá legalább egy tételt a számlához!");
                return null;
            }

            // Invoice objektum létrehozása/frissítése
            Invoice invoice = isEditMode ? existingInvoice : new Invoice();

            if (!isEditMode) {
                invoice.setId(UUID.randomUUID().toString());
                invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
                invoice.setIsPaid(false);
                invoice.setIsSentToNav(false);
                invoice.setCreatedAt(LocalDateTime.now());
            }

            invoice.setCustomer(customerComboBox.getValue());
            invoice.setInvoiceDate(invoiceDatePicker.getValue());
            invoice.setDeliveryDate(deliveryDatePicker.getValue());
            invoice.setPaymentDeadline(paymentDeadlinePicker.getValue());
            invoice.setPaymentMethod(paymentMethodComboBox.getValue());
            invoice.setCurrency(currencyComboBox.getValue());
            invoice.setExchangeRate(BigDecimal.valueOf(exchangeRateSpinner.getValue()));
            invoice.setIsReverseCharge(isReverseChargeCheckBox.isSelected());
            invoice.setIsCashAccounting(isCashAccountingCheckBox.isSelected());
            invoice.setNotes(notesArea.getText());
            invoice.setFooterText(footerTextArea.getText());
            invoice.setUpdatedAt(LocalDateTime.now());

            // Tételek hozzáadása
            invoice.setItems(new ArrayList<>(items));

            // Összegek számítása
            invoice.recalculateAmounts();

            // Mentés
            Invoice savedInvoice;
            if (isEditMode) {
                savedInvoice = invoiceService.updateInvoice(invoice);
                log.info("Invoice updated: {}", savedInvoice.getInvoiceNumber());
            } else {
                savedInvoice = invoiceService.createInvoice(invoice);
                log.info("Invoice created: {}", savedInvoice.getInvoiceNumber());
            }

            return savedInvoice;

        } catch (Exception e) {
            log.error("Failed to save invoice", e);
            AlertHelper.showError("Hiba", "Nem sikerült menteni a számlát", e.getMessage());
            return null;
        }
    }
}
