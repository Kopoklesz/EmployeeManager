package com.employeemanager.dialog;

import com.employeemanager.model.CompanySettings;
import com.employeemanager.repository.CompanySettingsRepository;
import com.employeemanager.service.InvoicingService;
import com.employeemanager.util.AlertHelper;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

/**
 * Céges beállítások dialógus - Számlázó modul
 *
 * Funkciók:
 * - Céges adatok szerkesztése
 * - NAV beállítások
 * - Számlázó backend választó (NAV Export, Szamlazz.hu, Billingo)
 * - API kulcsok kezelése
 */
@Slf4j
public class CompanySettingsDialog extends Dialog<CompanySettings> {

    private final CompanySettingsRepository settingsRepository;

    // Céges adatok
    private final TextField companyNameField = new TextField();
    private final TextField companyAddressField = new TextField();
    private final TextField companyZipCodeField = new TextField();
    private final TextField companyCityField = new TextField();
    private final TextField companyTaxNumberField = new TextField();
    private final TextField companyEuTaxNumberField = new TextField();
    private final TextField companyBankAccountField = new TextField();
    private final TextField companyBankNameField = new TextField();
    private final TextField companyEmailField = new TextField();
    private final TextField companyPhoneField = new TextField();
    private final TextField companyWebsiteField = new TextField();

    // Számla beállítások
    private final TextField invoicePrefixField = new TextField();
    private final Spinner<Integer> invoiceNextNumberSpinner = new Spinner<>(1, 999999, 1);
    private final Spinner<Integer> defaultPaymentDeadlineDaysSpinner = new Spinner<>(1, 365, 8);
    private final ComboBox<String> defaultCurrencyComboBox = new ComboBox<>();
    private final Spinner<Double> defaultVatRateSpinner = new Spinner<>(0.0, 50.0, 27.0, 0.5);
    private final TextArea invoiceFooterTextArea = new TextArea();

    // NAV beállítások
    private final TextField navTechnicalUserField = new TextField();
    private final PasswordField navSignatureKeyField = new PasswordField();
    private final PasswordField navReplacementKeyField = new PasswordField();
    private final CheckBox navTestModeCheckBox = new CheckBox("Teszt mód");

    // Számlázó backend
    private final ComboBox<InvoicingService.BackendType> invoicingBackendComboBox = new ComboBox<>();
    private final PasswordField szamlazzAgentKeyField = new PasswordField();
    private final PasswordField billingoApiKeyField = new PasswordField();

    public CompanySettingsDialog(CompanySettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;

        setTitle("Céges beállítások");
        setHeaderText("Céges adatok és számlázási beállítások");
        setWidth(700);
        setHeight(600);

        setupDialog();
        loadSettings();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 1. Céges adatok tab
        Tab companyTab = new Tab("Céges adatok");
        companyTab.setContent(createCompanyTab());

        // 2. Számla beállítások tab
        Tab invoiceTab = new Tab("Számla beállítások");
        invoiceTab.setContent(createInvoiceTab());

        // 3. NAV beállítások tab
        Tab navTab = new Tab("NAV beállítások");
        navTab.setContent(createNavTab());

        // 4. Számlázó backend tab
        Tab backendTab = new Tab("Számlázó backend");
        backendTab.setContent(createBackendTab());

        tabPane.getTabs().addAll(companyTab, invoiceTab, navTab, backendTab);

        ScrollPane scrollPane = new ScrollPane(tabPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        dialogPane.setContent(scrollPane);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return saveSettings();
            }
            return null;
        });
    }

    /**
     * Céges adatok tab
     */
    private VBox createCompanyTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Cégnév:*"), 0, row);
        grid.add(companyNameField, 1, row++);

        grid.add(new Label("Irányítószám:"), 0, row);
        grid.add(companyZipCodeField, 1, row++);

        grid.add(new Label("Város:"), 0, row);
        grid.add(companyCityField, 1, row++);

        grid.add(new Label("Cím:"), 0, row);
        grid.add(companyAddressField, 1, row++);

        grid.add(new Label("Adószám:*"), 0, row);
        grid.add(companyTaxNumberField, 1, row++);

        grid.add(new Label("EU adószám:"), 0, row);
        grid.add(companyEuTaxNumberField, 1, row++);

        grid.add(new Label("Bankszámlaszám:"), 0, row);
        grid.add(companyBankAccountField, 1, row++);

        grid.add(new Label("Bank neve:"), 0, row);
        grid.add(companyBankNameField, 1, row++);

        grid.add(new Label("Email:"), 0, row);
        grid.add(companyEmailField, 1, row++);

        grid.add(new Label("Telefon:"), 0, row);
        grid.add(companyPhoneField, 1, row++);

        grid.add(new Label("Weboldal:"), 0, row);
        grid.add(companyWebsiteField, 1, row++);

        // Mezők szélességének beállítása
        companyNameField.setPrefWidth(400);
        companyAddressField.setPrefWidth(400);
        companyZipCodeField.setPrefWidth(400);
        companyCityField.setPrefWidth(400);
        companyTaxNumberField.setPrefWidth(400);
        companyEuTaxNumberField.setPrefWidth(400);
        companyBankAccountField.setPrefWidth(400);
        companyBankNameField.setPrefWidth(400);
        companyEmailField.setPrefWidth(400);
        companyPhoneField.setPrefWidth(400);
        companyWebsiteField.setPrefWidth(400);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Számla beállítások tab
     */
    private VBox createInvoiceTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Számla előtag:"), 0, row);
        grid.add(invoicePrefixField, 1, row++);

        grid.add(new Label("Következő számlaszám:"), 0, row);
        grid.add(invoiceNextNumberSpinner, 1, row++);
        invoiceNextNumberSpinner.setEditable(true);

        grid.add(new Label("Alapértelmezett fizetési határidő (nap):"), 0, row);
        grid.add(defaultPaymentDeadlineDaysSpinner, 1, row++);
        defaultPaymentDeadlineDaysSpinner.setEditable(true);

        grid.add(new Label("Alapértelmezett pénznem:"), 0, row);
        defaultCurrencyComboBox.getItems().addAll("HUF", "EUR", "USD");
        grid.add(defaultCurrencyComboBox, 1, row++);

        grid.add(new Label("Alapértelmezett ÁFA kulcs (%):"), 0, row);
        grid.add(defaultVatRateSpinner, 1, row++);
        defaultVatRateSpinner.setEditable(true);

        grid.add(new Label("Számla lábléc szöveg:"), 0, row);
        invoiceFooterTextArea.setPrefRowCount(4);
        invoiceFooterTextArea.setWrapText(true);
        grid.add(invoiceFooterTextArea, 1, row++);

        // Mezők szélességének beállítása
        invoicePrefixField.setPrefWidth(400);
        invoiceNextNumberSpinner.setPrefWidth(400);
        defaultPaymentDeadlineDaysSpinner.setPrefWidth(400);
        defaultCurrencyComboBox.setPrefWidth(400);
        defaultVatRateSpinner.setPrefWidth(400);
        invoiceFooterTextArea.setPrefWidth(400);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * NAV beállítások tab
     */
    private VBox createNavTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        Label infoLabel = new Label(
            "NAV Online Számla 3.0 beállítások\n" +
            "Csak akkor szükséges, ha a NAV API közvetlen használatát tervezed.\n" +
            "NAV Export módban nem szükséges."
        );
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        grid.add(infoLabel, 0, row++, 2, 1);

        grid.add(new Separator(), 0, row++, 2, 1);

        grid.add(new Label("Technikai felhasználó:"), 0, row);
        grid.add(navTechnicalUserField, 1, row++);

        grid.add(new Label("Aláírási kulcs:"), 0, row);
        grid.add(navSignatureKeyField, 1, row++);

        grid.add(new Label("Cserekulcs:"), 0, row);
        grid.add(navReplacementKeyField, 1, row++);

        grid.add(navTestModeCheckBox, 1, row++);

        // Mezők szélességének beállítása
        navTechnicalUserField.setPrefWidth(400);
        navSignatureKeyField.setPrefWidth(400);
        navReplacementKeyField.setPrefWidth(400);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Számlázó backend tab
     */
    private VBox createBackendTab() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        Label infoLabel = new Label(
            "Válaszd ki a számlázó backend-et:\n" +
            "• NAV Export: Helyi XML generálás, NAV portálra kézi feltöltéshez\n" +
            "• Szamlazz.hu: Online számlázás Szamlazz.hu-n keresztül (Agent kulcs szükséges)\n" +
            "• Billingo: Online számlázás Billingo-n keresztül (API kulcs szükséges)"
        );
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
        grid.add(infoLabel, 0, row++, 2, 1);

        grid.add(new Separator(), 0, row++, 2, 1);

        grid.add(new Label("Számlázó backend:*"), 0, row);
        invoicingBackendComboBox.getItems().addAll(InvoicingService.BackendType.values());
        grid.add(invoicingBackendComboBox, 1, row++);

        grid.add(new Separator(), 0, row++, 2, 1);

        // Szamlazz.hu beállítások
        Label szamlazzLabel = new Label("Szamlazz.hu API beállítások:");
        szamlazzLabel.setStyle("-fx-font-weight: bold;");
        grid.add(szamlazzLabel, 0, row++, 2, 1);

        grid.add(new Label("Agent kulcs:"), 0, row);
        szamlazzAgentKeyField.setPromptText("Szamlazz.hu Agent kulcs (csak SZAMLAZZ_HU módban)");
        grid.add(szamlazzAgentKeyField, 1, row++);

        grid.add(new Separator(), 0, row++, 2, 1);

        // Billingo beállítások
        Label billingoLabel = new Label("Billingo API beállítások:");
        billingoLabel.setStyle("-fx-font-weight: bold;");
        grid.add(billingoLabel, 0, row++, 2, 1);

        grid.add(new Label("API kulcs:"), 0, row);
        billingoApiKeyField.setPromptText("Billingo API kulcs (csak BILLINGO módban)");
        grid.add(billingoApiKeyField, 1, row++);

        // Mezők szélességének beállítása
        invoicingBackendComboBox.setPrefWidth(400);
        szamlazzAgentKeyField.setPrefWidth(400);
        billingoApiKeyField.setPrefWidth(400);

        VBox vbox = new VBox(10);
        vbox.getChildren().add(grid);
        vbox.setPadding(new Insets(10));

        return vbox;
    }

    /**
     * Beállítások betöltése
     */
    private void loadSettings() {
        try {
            CompanySettings settings = settingsRepository.get();

            // Céges adatok
            companyNameField.setText(settings.getCompanyName());
            companyAddressField.setText(settings.getCompanyAddress());
            companyZipCodeField.setText(settings.getCompanyZipCode());
            companyCityField.setText(settings.getCompanyCity());
            companyTaxNumberField.setText(settings.getCompanyTaxNumber());
            companyEuTaxNumberField.setText(settings.getCompanyEuTaxNumber());
            companyBankAccountField.setText(settings.getBankAccountNumber());
            companyBankNameField.setText(settings.getBankName());
            companyEmailField.setText(settings.getCompanyEmail());
            companyPhoneField.setText(settings.getCompanyPhone());
            companyWebsiteField.setText(settings.getCompanyWebsite());

            // Számla beállítások
            invoicePrefixField.setText(settings.getInvoicePrefix());
            invoiceNextNumberSpinner.getValueFactory().setValue(settings.getInvoiceNextNumber());
            defaultPaymentDeadlineDaysSpinner.getValueFactory().setValue(settings.getDefaultPaymentDeadlineDays());
            defaultCurrencyComboBox.setValue(settings.getDefaultCurrency());
            defaultVatRateSpinner.getValueFactory().setValue(settings.getDefaultVatRate().doubleValue());
            invoiceFooterTextArea.setText(settings.getInvoiceFooterText());

            // NAV beállítások
            navTechnicalUserField.setText(settings.getNavTechnicalUser());
            navSignatureKeyField.setText(settings.getNavSignatureKey());
            navReplacementKeyField.setText(settings.getNavReplacementKey());
            navTestModeCheckBox.setSelected(settings.getNavTestMode() != null && settings.getNavTestMode());

            // Számlázó backend
            invoicingBackendComboBox.setValue(
                InvoicingService.BackendType.fromCode(settings.getInvoicing_backend())
            );
            szamlazzAgentKeyField.setText(settings.getSzamlazzAgentKey());
            billingoApiKeyField.setText(settings.getBillingoApiKey());

        } catch (Exception e) {
            log.error("Failed to load company settings", e);
            AlertHelper.showError("Hiba", "Nem sikerült betölteni a beállításokat", e.getMessage());
        }
    }

    /**
     * Beállítások mentése
     */
    private CompanySettings saveSettings() {
        try {
            CompanySettings settings = settingsRepository.get();

            // Validálás
            if (companyNameField.getText() == null || companyNameField.getText().trim().isEmpty()) {
                AlertHelper.showWarning("Figyelem", "Hiányzó adat", "A cégnév megadása kötelező!");
                return null;
            }

            if (companyTaxNumberField.getText() == null || companyTaxNumberField.getText().trim().isEmpty()) {
                AlertHelper.showWarning("Figyelem", "Hiányzó adat", "Az adószám megadása kötelező!");
                return null;
            }

            // Céges adatok
            settings.setCompanyName(companyNameField.getText());
            settings.setCompanyAddress(companyAddressField.getText());
            settings.setCompanyZipCode(companyZipCodeField.getText());
            settings.setCompanyCity(companyCityField.getText());
            settings.setCompanyTaxNumber(companyTaxNumberField.getText());
            settings.setCompanyEuTaxNumber(companyEuTaxNumberField.getText());
            settings.setBankAccountNumber(companyBankAccountField.getText());
            settings.setBankName(companyBankNameField.getText());
            settings.setCompanyEmail(companyEmailField.getText());
            settings.setCompanyPhone(companyPhoneField.getText());
            settings.setCompanyWebsite(companyWebsiteField.getText());

            // Számla beállítások
            settings.setInvoicePrefix(invoicePrefixField.getText());
            settings.setInvoiceNextNumber(invoiceNextNumberSpinner.getValue());
            settings.setDefaultPaymentDeadlineDays(defaultPaymentDeadlineDaysSpinner.getValue());
            settings.setDefaultCurrency(defaultCurrencyComboBox.getValue());
            settings.setDefaultVatRate(java.math.BigDecimal.valueOf(defaultVatRateSpinner.getValue()));
            settings.setInvoiceFooterText(invoiceFooterTextArea.getText());

            // NAV beállítások
            settings.setNavTechnicalUser(navTechnicalUserField.getText());
            settings.setNavSignatureKey(navSignatureKeyField.getText());
            settings.setNavReplacementKey(navReplacementKeyField.getText());
            settings.setNavTestMode(navTestModeCheckBox.isSelected());

            // Számlázó backend
            settings.setInvoicing_backend(invoicingBackendComboBox.getValue().getCode());
            settings.setSzamlazzAgentKey(szamlazzAgentKeyField.getText());
            settings.setBillingoApiKey(billingoApiKeyField.getText());

            // Mentés
            CompanySettings savedSettings = settingsRepository.save(settings);

            AlertHelper.showInfo("Siker", "Beállítások mentve", "A céges beállítások sikeresen elmentve!");

            return savedSettings;

        } catch (Exception e) {
            log.error("Failed to save company settings", e);
            AlertHelper.showError("Hiba", "Nem sikerült menteni a beállításokat", e.getMessage());
            return null;
        }
    }
}
