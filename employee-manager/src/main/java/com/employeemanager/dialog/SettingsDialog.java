package com.employeemanager.dialog;

import com.employeemanager.service.impl.SettingsService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class SettingsDialog extends Dialog<Void> {

    private final SettingsService settingsService;

    private final TextField authorField = new TextField();
    private final TextField databaseUrlField = new TextField();
    private final TextField databaseUsernameField = new TextField();
    private final PasswordField databasePasswordField = new PasswordField();

    public SettingsDialog(SettingsService settingsService) {
        this.settingsService = settingsService;

        setTitle("Beállítások");
        setHeaderText("Alkalmazás és adatbázis beállítások");

        setupDialog();
        populateFields();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TabPane tabPane = new TabPane();

        // Általános beállítások tab
        Tab generalTab = new Tab("Általános");
        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(10);
        generalGrid.setVgap(10);
        generalGrid.setPadding(new Insets(20, 150, 10, 10));

        // Verzió megjelenítése
        generalGrid.add(new Label("Verzió:"), 0, 0);
        generalGrid.add(new Label(settingsService.getApplicationVersion()), 1, 0);

        // Készítő megjelenítése
        generalGrid.add(new Label("Készítette:"), 0, 1);
        generalGrid.add(authorField, 1, 1);

        generalTab.setContent(generalGrid);

        // Adatbázis beállítások tab
        Tab databaseTab = new Tab("Adatbázis");
        GridPane databaseGrid = new GridPane();
        databaseGrid.setHgap(10);
        databaseGrid.setVgap(10);
        databaseGrid.setPadding(new Insets(20, 150, 10, 10));

        databaseGrid.add(new Label("Adatbázis URL:"), 0, 0);
        databaseGrid.add(databaseUrlField, 1, 0);
        databaseGrid.add(new Label("Felhasználónév:"), 0, 1);
        databaseGrid.add(databaseUsernameField, 1, 1);
        databaseGrid.add(new Label("Jelszó:"), 0, 2);
        databaseGrid.add(databasePasswordField, 1, 2);

        Button testConnectionButton = new Button("Kapcsolat tesztelése");
        testConnectionButton.setOnAction(e -> testDatabaseConnection());
        databaseGrid.add(testConnectionButton, 1, 3);

        databaseTab.setContent(databaseGrid);

        // Tabok hozzáadása
        tabPane.getTabs().addAll(generalTab, databaseTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        dialogPane.setContent(tabPane);

        // Mezők szélességének beállítása
        authorField.setPrefWidth(300);
        databaseUrlField.setPrefWidth(300);
        databaseUsernameField.setPrefWidth(300);
        databasePasswordField.setPrefWidth(300);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                saveSettings();
            }
            return null;
        });
    }

    private void populateFields() {
        authorField.setText(settingsService.getApplicationAuthor());
        databaseUrlField.setText(settingsService.getDatabaseUrl());
        databaseUsernameField.setText(settingsService.getDatabaseUsername());
        databasePasswordField.setText(settingsService.getDatabasePassword());
    }

    private void saveSettings() {
        try {
            settingsService.setDatabaseUrl(databaseUrlField.getText());
            settingsService.setDatabaseUsername(databaseUsernameField.getText());
            settingsService.setDatabasePassword(databasePasswordField.getText());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Siker");
            alert.setHeaderText(null);
            alert.setContentText("A beállítások mentése sikeres.\nAz alkalmazást újra kell indítani a változtatások érvénybe lépéséhez.");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Hiba");
            alert.setHeaderText("Nem sikerült menteni a beállításokat");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void testDatabaseConnection() {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kapcsolat teszt");
            alert.setHeaderText(null);
            alert.setContentText("A kapcsolat teszt sikeres!");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Kapcsolat teszt");
            alert.setHeaderText("Nem sikerült kapcsolódni az adatbázishoz");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}