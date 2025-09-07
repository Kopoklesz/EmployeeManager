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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        dialogPane.setPrefWidth(900);
        dialogPane.setPrefHeight(700);

        // Alkalmazz modern stílusokat
        dialogPane.setStyle("""
            -fx-background-color: #f8fafc;
            -fx-background-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0.0, 0, 5);
        """);

        mainTabPane = new TabPane();
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainTabPane.setStyle("""
            -fx-background-color: transparent;
            -fx-border-color: transparent;
        """);

        // Tab 1: Mentett kapcsolatok
        Tab savedConnectionsTab = new Tab("📋 Mentett Kapcsolatok");
        savedConnectionsTab.setContent(createSavedConnectionsPane());
        savedConnectionsTab.setStyle("""
            -fx-background-color: #ffffff;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
        """);

        // Tab 2: Új kapcsolat
        newConnectionTab = new Tab("➕ Új Kapcsolat");
        newConnectionTab.setContent(createNewConnectionPane());
        newConnectionTab.setStyle("""
            -fx-background-color: #ffffff;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
        """);

        mainTabPane.getTabs().addAll(savedConnectionsTab, newConnectionTab);
        dialogPane.setContent(mainTabPane);
    }

    private VBox createSavedConnectionsPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(25));
        pane.setStyle("-fx-background-color: #ffffff;");

        // Header
        Label headerLabel = new Label("💾 Mentett Adatbázis Kapcsolatok");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerLabel.setTextFill(Color.web("#1e293b"));

        Label descLabel = new Label("Válassza ki a használni kívánt adatbázis kapcsolatot");
        descLabel.setFont(Font.font("System", 14));
        descLabel.setTextFill(Color.web("#64748b"));

        // Kapcsolatok lista
        connectionsList = new ListView<>();
        connectionsList.setPrefHeight(350);
        connectionsList.setCellFactory(listView -> new ModernConnectionListCell());
        connectionsList.setStyle("""
            -fx-background-color: #f8fafc;
            -fx-border-color: #e2e8f0;
            -fx-border-width: 1;
            -fx-border-radius: 8;
            -fx-background-radius: 8;
        """);

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
        connectButton.setPrefWidth(160);
        connectButton.setStyle("""
            -fx-background-color: #22c55e;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(34,197,94,0.3), 8, 0.0, 0, 2);
        """);
        connectButton.setOnMouseEntered(e -> connectButton.setStyle("""
            -fx-background-color: #16a34a;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(34,197,94,0.4), 10, 0.0, 0, 3);
        """));
        connectButton.setOnMouseExited(e -> connectButton.setStyle("""
            -fx-background-color: #22c55e;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(34,197,94,0.3), 8, 0.0, 0, 2);
        """));
        connectButton.setOnAction(e -> connectToSelected());

        // Szerkesztés gomb (kék)
        editButton = new Button("✏️ Szerkesztés");
        editButton.setPrefWidth(160);
        editButton.setStyle("""
            -fx-background-color: #3b82f6;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 8, 0.0, 0, 2);
        """);
        editButton.setOnMouseEntered(e -> editButton.setStyle("""
            -fx-background-color: #2563eb;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 10, 0.0, 0, 3);
        """));
        editButton.setOnMouseExited(e -> editButton.setStyle("""
            -fx-background-color: #3b82f6;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 8, 0.0, 0, 2);
        """));
        editButton.setOnAction(e -> editSelected());

        // Törlés gomb (piros)
        deleteButton = new Button("🗑️ Törlés");
        deleteButton.setPrefWidth(160);
        deleteButton.setStyle("""
            -fx-background-color: #ef4444;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(239,68,68,0.3), 8, 0.0, 0, 2);
        """);
        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle("""
            -fx-background-color: #dc2626;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(239,68,68,0.4), 10, 0.0, 0, 3);
        """));
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle("""
            -fx-background-color: #ef4444;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(239,68,68,0.3), 8, 0.0, 0, 2);
        """));
        deleteButton.setOnAction(e -> deleteSelected());

        // Kezdetben letiltva
        updateButtonStates();

        buttonBox.getChildren().addAll(connectButton, editButton, deleteButton);
        return buttonBox;
    }

    private VBox createNewConnectionPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(25));
        pane.setStyle("-fx-background-color: #ffffff;");

        // Header
        Label headerLabel = new Label("➕ Új Adatbázis Kapcsolat");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerLabel.setTextFill(Color.web("#1e293b"));

        // Kapcsolat típusa
        VBox typeSection = createTypeSection();

        // Kapcsolat neve
        VBox nameSection = createNameSection();

        // Dinamikus form konténer
        connectionFormContainer = new VBox(15);

        // Akció gombok (új kapcsolathoz)
        HBox newConnectionButtons = createNewConnectionButtons();

        pane.getChildren().addAll(headerLabel, typeSection, nameSection, connectionFormContainer, newConnectionButtons);

        // Alapértelmezett típus beállítása
        typeComboBox.setValue(DatabaseType.FIREBASE);
        updateConnectionForm();

        return pane;
    }

    private VBox createTypeSection() {
        VBox section = new VBox(8);

        Label label = new Label("🗄️ Adatbázis Típusa");
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#374151"));

        typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll(DatabaseType.values());
        typeComboBox.setPrefWidth(300);
        typeComboBox.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #d1d5db;
            -fx-border-width: 1;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 10;
            -fx-font-size: 14px;
        """);

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
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#374151"));

        nameField = new TextField();
        nameField.setPromptText("pl. Teszt szerver, Éles környezet...");
        nameField.setPrefWidth(400);
        nameField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #d1d5db;
            -fx-border-width: 1;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 10;
            -fx-font-size: 14px;
        """);

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
        }
    }

    private VBox createFirebaseForm() {
        VBox form = new VBox(15);
        form.setStyle("""
            -fx-background-color: #fef3c7;
            -fx-background-radius: 10;
            -fx-padding: 20;
            -fx-border-color: #f59e0b;
            -fx-border-width: 1;
            -fx-border-radius: 10;
        """);

        Label headerLabel = new Label("🔥 Firebase Konfiguráció");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setTextFill(Color.web("#92400e"));

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
        form.setStyle("""
            -fx-background-color: #dbeafe;
            -fx-background-radius: 10;
            -fx-padding: 20;
            -fx-border-color: #3b82f6;
            -fx-border-width: 1;
            -fx-border-radius: 10;
        """);

        Label headerLabel = new Label("🗄️ SQL Adatbázis Konfiguráció");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setTextFill(Color.web("#1e40af"));

        // Host és Port egy sorban
        HBox hostPortBox = new HBox(10);
        VBox hostSection = createFieldSection("🖥️ Host",
                hostField = new TextField(), "localhost");
        VBox portSection = createFieldSection("🔌 Port",
                portField = new TextField(), "3306/5432");
        portField.setPrefWidth(100);
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
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#374151"));

        field.setPromptText(promptText);
        field.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #d1d5db;
            -fx-border-width: 1;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 8;
            -fx-font-size: 13px;
        """);

        section.getChildren().addAll(label, field);
        return section;
    }

    private VBox createFileSection() {
        VBox section = new VBox(5);

        Label label = new Label("📁 Service Account JSON");
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#374151"));

        HBox fileBox = new HBox(10);
        serviceAccountField = new TextField();
        serviceAccountField.setPromptText("Válassza ki a JSON fájlt...");
        serviceAccountField.setPrefWidth(300);
        serviceAccountField.setStyle("""
            -fx-background-color: white;
            -fx-border-color: #d1d5db;
            -fx-border-width: 1;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 8;
            -fx-font-size: 13px;
        """);

        browseButton = new Button("📁 Tallózás");
        browseButton.setStyle("""
            -fx-background-color: #f59e0b;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-padding: 8 15;
            -fx-background-radius: 6;
            -fx-cursor: hand;
        """);
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
        testButton.setPrefWidth(180);
        testButton.setStyle("""
            -fx-background-color: #8b5cf6;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(139,92,246,0.3), 8, 0.0, 0, 2);
        """);
        testButton.setOnAction(e -> testConnection());

        // Mentés gomb
        Button saveButton = new Button("💾 Mentés");
        saveButton.setPrefWidth(180);
        saveButton.setStyle("""
            -fx-background-color: #22c55e;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12 20;
            -fx-background-radius: 8;
            -fx-cursor: hand;
            -fx-effect: dropshadow(gaussian, rgba(34,197,94,0.3), 8, 0.0, 0, 2);
        """);
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

        // Mezők kitöltése
        nameField.setText(selected.getName());
        typeComboBox.setValue(selected.getType());

        switch (selected.getType()) {
            case FIREBASE -> {
                projectIdField.setText(selected.getFirebaseProjectId());
                databaseUrlField.setText(selected.getFirebaseDatabaseUrl());
                serviceAccountField.setText(selected.getFirebaseServiceAccountPath());
            }
            case MYSQL, POSTGRESQL -> {
                hostField.setText(selected.getHost());
                portField.setText(String.valueOf(selected.getPort()));
                databaseField.setText(selected.getDatabase());
                usernameField.setText(selected.getUsername());
                passwordField.setText(selected.getPassword());
            }
        }

        updateConnectionForm();
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
        }

        return config;
    }

    private Alert createProgressAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.getButtonTypes().clear();

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
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
                setStyle("-fx-background-color: transparent;");
            } else {
                HBox container = new HBox(15);
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(12));

                // Ikon
                Label iconLabel = new Label(getTypeIcon(item.getType()));
                iconLabel.setFont(Font.font(20));

                // Szöveg
                VBox textBox = new VBox(2);
                Label nameLabel = new Label(item.getName());
                nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                nameLabel.setTextFill(Color.web("#1e293b"));

                Label typeLabel = new Label(item.getType().getDisplayName());
                typeLabel.setFont(Font.font(12));
                typeLabel.setTextFill(Color.web("#64748b"));

                textBox.getChildren().addAll(nameLabel, typeLabel);

                container.getChildren().addAll(iconLabel, textBox);

                // Aktív státusz
                if (item.isActive()) {
                    Label activeLabel = new Label("✅ AKTÍV");
                    activeLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
                    activeLabel.setTextFill(Color.web("#22c55e"));
                    activeLabel.setStyle("""
                        -fx-background-color: #dcfce7;
                        -fx-background-radius: 12;
                        -fx-padding: 4 8;
                    """);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    container.getChildren().addAll(spacer, activeLabel);
                }

                setGraphic(container);
                setText(null);

                // Hover effekt
                setStyle("""
                    -fx-background-color: #f8fafc;
                    -fx-background-radius: 8;
                    -fx-border-color: #e2e8f0;
                    -fx-border-width: 1;
                    -fx-border-radius: 8;
                """);

                setOnMouseEntered(e -> setStyle("""
                    -fx-background-color: #f1f5f9;
                    -fx-background-radius: 8;
                    -fx-border-color: #3b82f6;
                    -fx-border-width: 2;
                    -fx-border-radius: 8;
                """));

                setOnMouseExited(e -> setStyle("""
                    -fx-background-color: #f8fafc;
                    -fx-background-radius: 8;
                    -fx-border-color: #e2e8f0;
                    -fx-border-width: 1;
                    -fx-border-radius: 8;
                """));
            }
        }
    }
}