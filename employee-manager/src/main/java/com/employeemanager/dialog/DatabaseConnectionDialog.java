package com.employeemanager.dialog;

import com.employeemanager.database.config.ConnectionConfig;
import com.employeemanager.database.config.DatabaseType;
import com.employeemanager.service.impl.DatabaseConnectionService;
import com.employeemanager.util.AlertHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Modern adatbázis kapcsolat kezelő dialógus
 */
public class DatabaseConnectionDialog extends Dialog<Void> {

    private final DatabaseConnectionService connectionService;
    private Runnable onDatabaseChanged;

    // Mentett kapcsolatok fül komponensei
    private ListView<ConnectionConfig> connectionsList;
    private Button connectButton;
    private Button editButton;
    private Button deleteButton;

    // Új kapcsolat fül komponensei
    private ComboBox<DatabaseType> typeComboBox;
    private TextField nameField;
    private VBox connectionFormContainer;
    private TabPane mainTabPane;
    private Tab newConnectionTab;

    // Firebase mezők
    private TextField projectIdField;
    private TextField databaseUrlField;
    private TextField serviceAccountField;
    private Button browseButton;

    // SQL mezők
    private TextField hostField;
    private TextField portField;
    private TextField databaseField;
    private TextField usernameField;
    private PasswordField passwordField;

    // Szerkesztési állapot
    private ConnectionConfig editingConfig;
    private boolean isEditMode = false;

    public DatabaseConnectionDialog(DatabaseConnectionService connectionService) {
        this.connectionService = connectionService;
        setupDialog();
        loadConnections();
    }

    public DatabaseConnectionDialog(DatabaseConnectionService connectionService, Runnable onDatabaseChanged) {
        this.connectionService = connectionService;
        this.onDatabaseChanged = onDatabaseChanged;
        setupDialog();
        loadConnections();
    }

    public void setOnDatabaseChanged(Runnable callback) {
        this.onDatabaseChanged = callback;
    }

    private void setupDialog() {
        setTitle("Adatbázis Kapcsolat Kezelő");
        setHeaderText("Adatbázis kapcsolatok kezelése és új kapcsolat létrehozása");

        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.CLOSE);
        
        // CSS fájl betöltése
        dialogPane.getStylesheets().add(
            getClass().getResource("/css/database-dialog.css").toExternalForm()
        );
        
        // Dialog stílus alkalmazása
        dialogPane.getStyleClass().add("db-dialog");

        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainTabPane.getStyleClass().add("db-main-tabpane");

        // Tab 1: Mentett kapcsolatok
        Tab savedConnectionsTab = new Tab("📋 Mentett Kapcsolatok");
        savedConnectionsTab.setContent(createSavedConnectionsPane());
        savedConnectionsTab.getStyleClass().add("db-tab");

        // Tab 2: Új kapcsolat
        newConnectionTab = new Tab("➕ Új Kapcsolat");
        newConnectionTab.setContent(createNewConnectionPane());
        newConnectionTab.getStyleClass().add("db-tab");

        mainTabPane.getTabs().addAll(savedConnectionsTab, newConnectionTab);
        dialogPane.setContent(mainTabPane);
    }

    private VBox createSavedConnectionsPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(25));
        pane.getStyleClass().add("db-tab-content");

        // Header
        Label headerLabel = new Label("💾 Mentett Adatbázis Kapcsolatok");
        headerLabel.getStyleClass().add("db-header-label");

        Label descLabel = new Label("Válassza ki a használni kívánt adatbázis kapcsolatot");
        descLabel.getStyleClass().add("db-description-label");

        // Kapcsolatok lista
        connectionsList = new ListView<>();
        connectionsList.setPrefHeight(350);
        connectionsList.setCellFactory(listView -> new ModernConnectionListCell());
        connectionsList.getStyleClass().add("db-connection-list");

        connectionsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateButtonStates()
        );

        // Akció gombok
        HBox buttonBox = createActionButtons();

        pane.getChildren().addAll(headerLabel, descLabel, connectionsList, buttonBox);
        return pane;
    }

    private HBox createActionButtons() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        // Kapcsolódás gomb (zöld)
        connectButton = new Button("🔌 Kapcsolódás");
        connectButton.getStyleClass().add("db-button-primary");
        connectButton.setOnAction(e -> connectToSelected());

        // Szerkesztés gomb (kék)
        editButton = new Button("✏️ Szerkesztés");
        editButton.getStyleClass().add("db-button-secondary");
        editButton.setOnAction(e -> editSelected());

        // Törlés gomb (piros)
        deleteButton = new Button("🗑️ Törlés");
        deleteButton.getStyleClass().add("db-button-danger");
        deleteButton.setOnAction(e -> deleteSelected());

        // Kezdetben letiltva
        updateButtonStates();

        buttonBox.getChildren().addAll(connectButton, editButton, deleteButton);
        return buttonBox;
    }

    private VBox createNewConnectionPane() {
        VBox outerContainer = new VBox(20);
        outerContainer.setPadding(new Insets(25));
        outerContainer.getStyleClass().add("db-tab-content");

        // Header
        Label headerLabel = new Label("➕ Új Adatbázis Kapcsolat");
        headerLabel.getStyleClass().add("db-header-label");

        // Scrollable tartalom konténer
        VBox scrollContent = new VBox(20);

        // Kapcsolat típusa
        VBox typeSection = createTypeSection();

        // Kapcsolat neve
        VBox nameSection = createNameSection();

        // Dinamikus form konténer
        connectionFormContainer = new VBox(15);

        // Akció gombok (új kapcsolathoz)
        HBox newConnectionButtons = createNewConnectionButtons();

        scrollContent.getChildren().addAll(typeSection, nameSection, connectionFormContainer, newConnectionButtons);

        // ScrollPane wrapper a form tartalom körül
        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.getStyleClass().addAll("db-scroll-pane", "db-scroll-form", "db-scroll-container-form");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        outerContainer.getChildren().addAll(headerLabel, scrollPane);

        // Alapértelmezett típus beállítása
        typeComboBox.setValue(DatabaseType.FIREBASE);
        updateConnectionForm();

        return outerContainer;
    }

    private VBox createTypeSection() {
        VBox section = new VBox(8);

        Label label = new Label("🗄️ Adatbázis Típusa");
        label.getStyleClass().add("db-section-label");

        typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll(
                DatabaseType.FIREBASE,
                DatabaseType.MYSQL,
                DatabaseType.POSTGRESQL
                // DatabaseType.H2 
        );
        typeComboBox.getStyleClass().add("db-combobox");

        // Szép megjelenítés a ComboBox elemekhez
        typeComboBox.setCellFactory(combo -> new ListCell<DatabaseType>() {
            @Override
            protected void updateItem(DatabaseType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getTypeIcon(item) + " " + item.getDisplayName());
                }
            }
        });

        typeComboBox.setButtonCell(new ListCell<DatabaseType>() {
            @Override
            protected void updateItem(DatabaseType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(getTypeIcon(item) + " " + item.getDisplayName());
                }
            }
        });

        typeComboBox.setOnAction(e -> updateConnectionForm());

        section.getChildren().addAll(label, typeComboBox);
        return section;
    }

    private VBox createNameSection() {
        VBox section = new VBox(8);

        Label label = new Label("📝 Kapcsolat Neve");
        label.getStyleClass().add("db-section-label");

        nameField = new TextField();
        nameField.setPromptText("pl. Teszt szerver, Éles környezet...");
        nameField.getStyleClass().add("db-input-name");

        section.getChildren().addAll(label, nameField);
        return section;
    }

    private void updateConnectionForm() {
        connectionFormContainer.getChildren().clear();

        DatabaseType selectedType = typeComboBox.getValue();
        if (selectedType == null) return;

        switch (selectedType) {
            case FIREBASE -> connectionFormContainer.getChildren().add(createFirebaseForm());
            case MYSQL, POSTGRESQL -> connectionFormContainer.getChildren().add(createSqlForm());
            default -> throw new IllegalArgumentException("Unexpected value: " + selectedType);
        }
    }

    private VBox createFirebaseForm() {
        VBox form = new VBox(15);
        form.getStyleClass().add("db-form-firebase");

        Label headerLabel = new Label("🔥 Firebase Konfiguráció");
        headerLabel.getStyleClass().add("db-form-header-firebase");

        // Project ID
        VBox projectSection = createFieldSection("📋 Project ID",
                projectIdField = new TextField(), "your-project-id");

        // Database URL
        VBox urlSection = createFieldSection("🌐 Database URL",
                databaseUrlField = new TextField(), "https://your-project.firebaseio.com");

        // Service Account fájl
        VBox fileSection = createFileSection();

        form.getChildren().addAll(headerLabel, projectSection, urlSection, fileSection);
        return form;
    }

    private VBox createSqlForm() {
        VBox form = new VBox(15);
        form.getStyleClass().add("db-form-sql");

        Label headerLabel = new Label("🗄️ SQL Adatbázis Konfiguráció");
        headerLabel.getStyleClass().add("db-form-header-sql");

        // Host és Port egy sorban
        HBox hostPortBox = new HBox(10);
        VBox hostSection = createFieldSection("🖥️ Host",
                hostField = new TextField(), "localhost");
        VBox portSection = createFieldSection("🔌 Port",
                portField = new TextField(), "3306/5432");
        portField.getStyleClass().add("db-input-port");
        hostPortBox.getChildren().addAll(hostSection, portSection);

        // Database
        VBox dbSection = createFieldSection("💾 Adatbázis",
                databaseField = new TextField(), "database_name");

        // Username
        VBox userSection = createFieldSection("👤 Felhasználónév",
                usernameField = new TextField(), "username");

        // Password
        VBox passSection = createFieldSection("🔒 Jelszó",
                passwordField = new PasswordField(), "••••••••");

        // Alapértelmezett port beállítása típus szerint
        DatabaseType type = typeComboBox.getValue();
        if (type == DatabaseType.MYSQL) {
            portField.setText("3306");
        } else if (type == DatabaseType.POSTGRESQL) {
            portField.setText("5432");
        }

        form.getChildren().addAll(headerLabel, hostPortBox, dbSection, userSection, passSection);
        return form;
    }

    private VBox createFieldSection(String labelText, TextField field, String promptText) {
        VBox section = new VBox(5);

        Label label = new Label(labelText);
        label.getStyleClass().add("db-field-label");

        field.setPromptText(promptText);
        field.getStyleClass().add("db-input");

        section.getChildren().addAll(label, field);
        return section;
    }

    private VBox createFileSection() {
        VBox section = new VBox(5);

        Label label = new Label("📄 Service Account JSON");
        label.getStyleClass().add("db-field-label");

        HBox fileBox = new HBox(10);
        serviceAccountField = new TextField();
        serviceAccountField.setPromptText("Válassza ki a JSON fájlt...");
        serviceAccountField.getStyleClass().add("db-input-file");

        browseButton = new Button("📁 Tallózás");
        browseButton.getStyleClass().add("db-button-browse");
        browseButton.setOnAction(e -> browseServiceAccount());

        fileBox.getChildren().addAll(serviceAccountField, browseButton);
        section.getChildren().addAll(label, fileBox);
        return section;
    }

    private HBox createNewConnectionButtons() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        // Teszt gomb
        Button testButton = new Button("🔍 Kapcsolat Tesztelése");
        testButton.getStyleClass().add("db-button-test");
        testButton.setOnAction(e -> testConnection());

        // Mentés gomb
        Button saveButton = new Button("💾 Mentés");
        saveButton.getStyleClass().add("db-button-save");
        saveButton.setOnAction(e -> saveConnection());

        buttonBox.getChildren().addAll(testButton, saveButton);
        return buttonBox;
    }

    private String getTypeIcon(DatabaseType type) {
        return switch (type) {
            case FIREBASE -> "🔥";
            case MYSQL -> "🐬";
            case POSTGRESQL -> "🐘";
            case H2 -> "🗄️";
        };
    }

    private void updateButtonStates() {
        ConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null;
        @SuppressWarnings("null")
        boolean isActive = hasSelection && selected.isActive();

        connectButton.setDisable(!hasSelection || isActive);
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection || isActive);
    }

    private void loadConnections() {
        connectionsList.getItems().clear();
        connectionsList.getItems().addAll(connectionService.getAllConnections());

        // Aktív kapcsolat kijelölése
        connectionService.getActiveConnection().ifPresent(active ->
                connectionsList.getSelectionModel().select(active));
    }

    private void connectToSelected() {
        ConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Progress dialog
        Alert progressAlert = createProgressAlert("Kapcsolódás...", "Aktiválás folyamatban");

        CompletableFuture.supplyAsync(() ->
                connectionService.activateConnection(selected.getName())
        ).thenAcceptAsync(success -> {
            Platform.runLater(() -> {
                progressAlert.close();
                if (success) {
                    loadConnections();
                    if (onDatabaseChanged != null) {
                        onDatabaseChanged.run();
                    }
                    AlertHelper.showInformation("Sikeres kapcsolódás",
                            "Kapcsolat aktiválva",
                            "Az adatbázis kapcsolat sikeresen aktiválva: " + selected.getName());
                } else {
                    AlertHelper.showError("Kapcsolódási hiba",
                            "Nem sikerült aktiválni",
                            "Ellenőrizze a kapcsolat beállításokat.");
                }
            });
        });

        progressAlert.show();
    }

    private void editSelected() {
        ConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Váltás az új kapcsolat fülre
        mainTabPane.getSelectionModel().select(newConnectionTab);

        // Szerkesztési mód aktiválása
        isEditMode = true;
        editingConfig = selected;
        newConnectionTab.setText("✏️ Kapcsolat Szerkesztése");

        // Alapadatok kitöltése
        nameField.setText(selected.getName());
        typeComboBox.setValue(selected.getType());

        // Form frissítése a típus alapján (fontos: ez először történjen meg!)
        updateConnectionForm();

        // Platform.runLater használata, hogy a mezők már létezzenek amikor kitöltjük őket
        Platform.runLater(() -> {
            switch (selected.getType()) {
                case FIREBASE -> {
                    if (projectIdField != null) {
                        projectIdField.setText(selected.getFirebaseProjectId() != null ? selected.getFirebaseProjectId() : "");
                    }
                    if (databaseUrlField != null) {
                        databaseUrlField.setText(selected.getFirebaseDatabaseUrl() != null ? selected.getFirebaseDatabaseUrl() : "");
                    }
                    if (serviceAccountField != null) {
                        serviceAccountField.setText(selected.getFirebaseServiceAccountPath() != null ? selected.getFirebaseServiceAccountPath() : "");
                    }
                }
                case MYSQL, POSTGRESQL -> {
                    if (hostField != null) {
                        hostField.setText(selected.getHost() != null ? selected.getHost() : "");
                    }
                    if (portField != null) {
                        portField.setText(String.valueOf(selected.getPort()));
                    }
                    if (databaseField != null) {
                        databaseField.setText(selected.getDatabase() != null ? selected.getDatabase() : "");
                    }
                    if (usernameField != null) {
                        usernameField.setText(selected.getUsername() != null ? selected.getUsername() : "");
                    }
                    if (passwordField != null) {
                        passwordField.setText(selected.getPassword() != null ? selected.getPassword() : "");
                    }
                }
                default -> throw new IllegalArgumentException("Unexpected value: " + selected.getType());
            }
        });
    }

    private void deleteSelected() {
        ConnectionConfig selected = connectionsList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.isActive()) {
            AlertHelper.showWarning("Törlés nem lehetséges",
                    "Az aktív kapcsolat nem törölhető!");
            return;
        }

        if (AlertHelper.showConfirmation("Törlés megerősítése",
                "Biztosan törli ezt a kapcsolatot?",
                selected.getName())) {

            connectionService.removeConnection(selected.getName());
            loadConnections();
            AlertHelper.showInformation("Sikeres törlés",
                    "Kapcsolat törölve",
                    selected.getName() + " sikeresen törölve.");
        }
    }

    private void testConnection() {
        if (!validateForm()) return;

        ConnectionConfig config = buildConfigFromForm();
        Alert progressAlert = createProgressAlert("Tesztelés...", "Kapcsolat tesztelése folyamatban");

        CompletableFuture.supplyAsync(() ->
                connectionService.testConnectionWithDetails(config)
        ).thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                progressAlert.close();
                if (result.startsWith("Sikeres")) {
                    AlertHelper.showInformation("Teszt sikeres",
                            "Kapcsolat működik",
                            "A kapcsolat tesztelése sikeres volt!");
                } else {
                    AlertHelper.showError("Teszt sikertelen",
                            "Kapcsolódási hiba",
                            result);
                }
            });
        });

        progressAlert.show();
    }

    private void saveConnection() {
        if (!validateForm()) return;

        ConnectionConfig config = buildConfigFromForm();

        // Név egyediség ellenőrzése (kivéve szerkesztési mód)
        if (!isEditMode && connectionService.getAllConnections().stream()
                .anyMatch(c -> c.getName().equals(config.getName()))) {
            AlertHelper.showWarning("Duplikált név",
                    "Már létezik kapcsolat ezzel a névvel!");
            return;
        }

        try {
            if (isEditMode) {
                connectionService.updateConnection(editingConfig.getName(), config);
                AlertHelper.showInformation("Sikeres módosítás",
                        "Kapcsolat frissítve",
                        config.getName());
                exitEditMode();
            } else {
                connectionService.addConnection(config);
                AlertHelper.showInformation("Sikeres mentés",
                        "Kapcsolat elmentve",
                        config.getName());
                clearForm();
            }

            loadConnections();
            mainTabPane.getSelectionModel().selectFirst();

        } catch (Exception e) {
            AlertHelper.showError("Mentési hiba",
                    "Nem sikerült menteni",
                    e.getMessage());
        }
    }

    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty()) {
            AlertHelper.showWarning("Hiányzó adat",
                    "Adjon meg egy nevet a kapcsolatnak!");
            return false;
        }

        DatabaseType type = typeComboBox.getValue();
        if (type == null) {
            AlertHelper.showWarning("Hiányzó adat",
                    "Válasszon adatbázis típust!");
            return false;
        }

        switch (type) {
            case FIREBASE -> {
                if (projectIdField.getText().trim().isEmpty() ||
                        databaseUrlField.getText().trim().isEmpty()) {
                    AlertHelper.showWarning("Hiányzó adatok",
                            "Töltse ki az összes Firebase mezőt!");
                    return false;
                }
            }
            case MYSQL, POSTGRESQL -> {
                if (hostField.getText().trim().isEmpty() ||
                        portField.getText().trim().isEmpty() ||
                        databaseField.getText().trim().isEmpty() ||
                        usernameField.getText().trim().isEmpty()) {
                    AlertHelper.showWarning("Hiányzó adatok",
                            "Töltse ki az összes adatbázis mezőt!");
                    return false;
                }

                // Port szám validálás
                try {
                    Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException e) {
                    AlertHelper.showWarning("Hibás port szám",
                            "A port mezőben csak számot adjon meg!");
                    return false;
                }
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + type);
        }

        return true;
    }

    private ConnectionConfig buildConfigFromForm() {
        ConnectionConfig config = ConnectionConfig.builder()
                .name(nameField.getText().trim())
                .type(typeComboBox.getValue())
                .build();

        switch (typeComboBox.getValue()) {
            case FIREBASE -> {
                config.setFirebaseProjectId(projectIdField.getText().trim());
                config.setFirebaseDatabaseUrl(databaseUrlField.getText().trim());
                config.setFirebaseServiceAccountPath(serviceAccountField.getText().trim());
            }
            case MYSQL, POSTGRESQL -> {
                config.setHost(hostField.getText().trim());
                config.setPort(Integer.parseInt(portField.getText().trim()));
                config.setDatabase(databaseField.getText().trim());
                config.setUsername(usernameField.getText().trim());
                config.setPassword(passwordField.getText());
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + typeComboBox.getValue());
        }

        return config;
    }

    private Alert createProgressAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.getButtonTypes().clear();

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.getStyleClass().add("db-progress");
        alert.setGraphic(progressIndicator);

        return alert;
    }

    private void clearForm() {
        nameField.clear();
        typeComboBox.setValue(DatabaseType.FIREBASE);

        if (projectIdField != null) projectIdField.clear();
        if (databaseUrlField != null) databaseUrlField.clear();
        if (serviceAccountField != null) serviceAccountField.clear();
        if (hostField != null) hostField.clear();
        if (portField != null) portField.clear();
        if (databaseField != null) databaseField.clear();
        if (usernameField != null) usernameField.clear();
        if (passwordField != null) passwordField.clear();

        updateConnectionForm();
    }

    private void exitEditMode() {
        isEditMode = false;
        editingConfig = null;
        newConnectionTab.setText("➕ Új Kapcsolat");
        clearForm();
    }

    private void browseServiceAccount() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Service Account JSON fájl");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON fájlok", "*.json"));

        File file = fileChooser.showOpenDialog(getDialogPane().getScene().getWindow());
        if (file != null) {
            serviceAccountField.setText(file.getAbsolutePath());
        }
    }

    // Custom ListCell a szép megjelenítéshez
    private class ModernConnectionListCell extends ListCell<ConnectionConfig> {
        @Override
        protected void updateItem(ConnectionConfig item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("db-connection-list");
            } else {
                HBox container = new HBox(15);
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(12));

                // Ikon
                Label iconLabel = new Label(getTypeIcon(item.getType()));
                iconLabel.getStyleClass().add("db-list-cell-icon");

                // Szöveg
                VBox textBox = new VBox(2);
                Label nameLabel = new Label(item.getName());
                nameLabel.getStyleClass().add("db-list-cell-name");

                Label typeLabel = new Label(item.getType().getDisplayName());
                typeLabel.getStyleClass().add("db-list-cell-type");

                textBox.getChildren().addAll(nameLabel, typeLabel);
                container.getChildren().addAll(iconLabel, textBox);

                // Aktív státusz
                if (item.isActive()) {
                    Label activeLabel = new Label("✅ AKTÍV");
                    activeLabel.getStyleClass().add("db-active-badge");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    container.getChildren().addAll(spacer, activeLabel);
                }

                setGraphic(container);
                setText(null);
            }
        }
    }
}