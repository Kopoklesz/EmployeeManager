package com.employeemanager.dialog;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.repository.CompanySettingsRepository;
import com.employeemanager.util.AlertHelper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Számla tétel szerkesztő dialógus
 *
 * Funkciók:
 * - Tétel megnevezése, mennyiség, egységár
 * - ÁFA kulcs
 * - Kedvezmény százalék
 * - ÁFA mentességi ok
 * - Automatikus összeg számítás (nettó, ÁFA, bruttó)
 */
@Slf4j
public class InvoiceItemDialog extends Dialog<InvoiceItem> {

    private final CompanySettingsRepository settingsRepository;
    private final InvoiceItem existingItem;
    private final boolean isEditMode;

    private final TextField descriptionField = new TextField();
    private final Spinner<Double> quantitySpinner = new Spinner<>(0.01, 999999.0, 1.0, 0.01);
    private final ComboBox<String> unitComboBox = new ComboBox<>();
    private final Spinner<Double> unitPriceSpinner = new Spinner<>(0.0, 999999999.0, 0.0, 1.0);
    private final Spinner<Double> vatRateSpinner = new Spinner<>(0.0, 50.0, 27.0, 0.5);
    private final Spinner<Double> discountPercentSpinner = new Spinner<>(0.0, 100.0, 0.0, 1.0);
    private final TextField vatExemptionReasonField = new TextField();
    private final TextArea notesArea = new TextArea();

    // Számított értékek
    private final Label netAmountLabel = new Label("0");
    private final Label vatAmountLabel = new Label("0");
    private final Label grossAmountLabel = new Label("0");

    public InvoiceItemDialog(CompanySettingsRepository settingsRepository) {
        this(settingsRepository, null);
    }

    public InvoiceItemDialog(CompanySettingsRepository settingsRepository, InvoiceItem existingItem) {
        this.settingsRepository = settingsRepository;
        this.existingItem = existingItem;
        this.isEditMode = (existingItem != null);

        setTitle(isEditMode ? "Tétel szerkesztése" : "Új tétel hozzáadása");
        setHeaderText(isEditMode ? "Tétel adatainak módosítása" : "Új tétel hozzáadása a számlához");

        setupDialog();

        if (isEditMode) {
            loadItem();
        } else {
            setDefaults();
        }

        // Automatikus számítás
        unitPriceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> calculateAmounts());
        quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> calculateAmounts());
        vatRateSpinner.valueProperty().addListener((obs, oldVal, newVal) -> calculateAmounts());
        discountPercentSpinner.valueProperty().addListener((obs, oldVal, newVal) -> calculateAmounts());
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Megnevezés
        grid.add(new Label("Megnevezés:*"), 0, row);
        descriptionField.setPromptText("Termék vagy szolgáltatás megnevezése");
        grid.add(descriptionField, 1, row++);

        // Mennyiség
        grid.add(new Label("Mennyiség:*"), 0, row);
        quantitySpinner.setEditable(true);
        grid.add(quantitySpinner, 1, row++);

        // Mértékegység
        grid.add(new Label("Mértékegység:*"), 0, row);
        unitComboBox.getItems().addAll("db", "óra", "kg", "m", "m²", "m³", "liter", "csomag", "doboz");
        unitComboBox.setEditable(true);
        grid.add(unitComboBox, 1, row++);

        // Egységár
        grid.add(new Label("Nettó egységár:*"), 0, row);
        unitPriceSpinner.setEditable(true);
        grid.add(unitPriceSpinner, 1, row++);

        // ÁFA kulcs
        grid.add(new Label("ÁFA kulcs (%):*"), 0, row);
        vatRateSpinner.setEditable(true);
        grid.add(vatRateSpinner, 1, row++);

        // Kedvezmény
        grid.add(new Label("Kedvezmény (%):"), 0, row);
        discountPercentSpinner.setEditable(true);
        grid.add(discountPercentSpinner, 1, row++);

        // ÁFA mentességi ok
        grid.add(new Label("ÁFA mentességi ok:"), 0, row);
        vatExemptionReasonField.setPromptText("Pl.: TAM - Adómentes (ha van)");
        grid.add(vatExemptionReasonField, 1, row++);

        // Megjegyzés
        grid.add(new Label("Megjegyzés:"), 0, row);
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        grid.add(notesArea, 1, row++);

        // Elválasztó
        grid.add(new Separator(), 0, row++, 2, 1);

        // Számított értékek
        Label summaryLabel = new Label("Számított összegek:");
        summaryLabel.setStyle("-fx-font-weight: bold;");
        grid.add(summaryLabel, 0, row++, 2, 1);

        grid.add(new Label("Nettó összeg:"), 0, row);
        netAmountLabel.setStyle("-fx-font-weight: bold;");
        grid.add(netAmountLabel, 1, row++);

        grid.add(new Label("ÁFA összeg:"), 0, row);
        vatAmountLabel.setStyle("-fx-font-weight: bold;");
        grid.add(vatAmountLabel, 1, row++);

        grid.add(new Label("Bruttó összeg:"), 0, row);
        grossAmountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        grid.add(grossAmountLabel, 1, row++);

        // Mezők szélességének beállítása
        descriptionField.setPrefWidth(400);
        quantitySpinner.setPrefWidth(400);
        unitComboBox.setPrefWidth(400);
        unitPriceSpinner.setPrefWidth(400);
        vatRateSpinner.setPrefWidth(400);
        discountPercentSpinner.setPrefWidth(400);
        vatExemptionReasonField.setPrefWidth(400);
        notesArea.setPrefWidth(400);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        dialogPane.setContent(scrollPane);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return saveItem();
            }
            return null;
        });
    }

    /**
     * Alapértelmezett értékek beállítása
     */
    private void setDefaults() {
        try {
            CompanySettings settings = settingsRepository.get();

            quantitySpinner.getValueFactory().setValue(1.0);
            unitComboBox.setValue("db");

            if (settings.getDefaultVatRate() != null) {
                vatRateSpinner.getValueFactory().setValue(settings.getDefaultVatRate().doubleValue());
            }

        } catch (Exception e) {
            log.error("Failed to load default settings", e);
        }

        calculateAmounts();
    }

    /**
     * Tétel betöltése (szerkesztés mód)
     */
    private void loadItem() {
        if (existingItem == null) return;

        descriptionField.setText(existingItem.getDescription());
        quantitySpinner.getValueFactory().setValue(existingItem.getQuantity().doubleValue());
        unitComboBox.setValue(existingItem.getUnit());
        unitPriceSpinner.getValueFactory().setValue(existingItem.getUnitPrice().doubleValue());
        vatRateSpinner.getValueFactory().setValue(existingItem.getVatRate().doubleValue());

        if (existingItem.getDiscountPercent() != null) {
            discountPercentSpinner.getValueFactory().setValue(existingItem.getDiscountPercent().doubleValue());
        }

        vatExemptionReasonField.setText(existingItem.getVatExemptionReason());
        notesArea.setText(existingItem.getNotes());

        calculateAmounts();
    }

    /**
     * Összegek automatikus számítása
     */
    private void calculateAmounts() {
        try {
            BigDecimal quantity = BigDecimal.valueOf(quantitySpinner.getValue());
            BigDecimal unitPrice = BigDecimal.valueOf(unitPriceSpinner.getValue());
            BigDecimal vatRate = BigDecimal.valueOf(vatRateSpinner.getValue());
            BigDecimal discountPercent = BigDecimal.valueOf(discountPercentSpinner.getValue());

            // Nettó összeg (mennyiség × egységár)
            BigDecimal netBeforeDiscount = quantity.multiply(unitPrice);

            // Kedvezmény összege
            BigDecimal discountAmount = netBeforeDiscount
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Nettó összeg kedvezmény után
            BigDecimal netAmount = netBeforeDiscount.subtract(discountAmount);

            // ÁFA összeg
            BigDecimal vatAmount = netAmount
                .multiply(vatRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Bruttó összeg
            BigDecimal grossAmount = netAmount.add(vatAmount);

            // Címkék frissítése
            netAmountLabel.setText(String.format("%,.2f", netAmount.doubleValue()));
            vatAmountLabel.setText(String.format("%,.2f", vatAmount.doubleValue()));
            grossAmountLabel.setText(String.format("%,.2f", grossAmount.doubleValue()));

        } catch (Exception e) {
            log.error("Failed to calculate amounts", e);
        }
    }

    /**
     * Tétel mentése
     */
    private InvoiceItem saveItem() {
        try {
            // Validálás
            if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) {
                AlertHelper.showWarning("Figyelem", "A megnevezés megadása kötelező!");
                return null;
            }

            if (unitComboBox.getValue() == null || unitComboBox.getValue().trim().isEmpty()) {
                AlertHelper.showWarning("Figyelem", "A mértékegység megadása kötelező!");
                return null;
            }

            // InvoiceItem objektum létrehozása/frissítése
            InvoiceItem item = isEditMode ? existingItem : new InvoiceItem();

            if (!isEditMode) {
                item.setId(UUID.randomUUID().toString());
            }

            item.setDescription(descriptionField.getText().trim());
            item.setQuantity(BigDecimal.valueOf(quantitySpinner.getValue()));
            item.setUnit(unitComboBox.getValue());
            item.setUnitPrice(BigDecimal.valueOf(unitPriceSpinner.getValue()));
            item.setVatRate(BigDecimal.valueOf(vatRateSpinner.getValue()));
            item.setDiscountPercent(BigDecimal.valueOf(discountPercentSpinner.getValue()));
            item.setVatExemptionReason(vatExemptionReasonField.getText());
            item.setNotes(notesArea.getText());

            // Összegek számítása
            item.calculateAmounts();

            log.info("Invoice item saved: {}", item.getDescription());

            return item;

        } catch (Exception e) {
            log.error("Failed to save invoice item", e);
            AlertHelper.showError("Hiba", "Nem sikerült menteni a tételt", e.getMessage());
            return null;
        }
    }
}
