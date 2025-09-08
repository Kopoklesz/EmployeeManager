package com.employeemanager.dialog;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class UserGuideDialog extends Dialog<Void> {

    public UserGuideDialog() {
        setTitle("Használati útmutató");
        setHeaderText("Alkalmazott Nyilvántartó Rendszer - Használati útmutató");

        setupDialog();
    }

    private void setupDialog() {
        DialogPane dialogPane = getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Áttekintés tab
        Tab overviewTab = new Tab("Áttekintés");
        VBox overviewContent = createOverviewContent();
        overviewTab.setContent(overviewContent);

        // Alkalmazottak kezelése tab
        Tab employeesTab = new Tab("Alkalmazottak kezelése");
        VBox employeesContent = createEmployeesContent();
        employeesTab.setContent(employeesContent);

        // Munkanaplók kezelése tab
        Tab workRecordsTab = new Tab("Munkanaplók kezelése");
        VBox workRecordsContent = createWorkRecordsContent();
        workRecordsTab.setContent(workRecordsContent);

        // Riportok tab
        Tab reportsTab = new Tab("Riportok");
        VBox reportsContent = createReportsContent();
        reportsTab.setContent(reportsContent);

        tabPane.getTabs().addAll(overviewTab, employeesTab, workRecordsTab, reportsTab);

        dialogPane.setContent(tabPane);
        dialogPane.setPrefWidth(800);
        dialogPane.setPrefHeight(600);
    }

    private VBox createOverviewContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextArea textArea = new TextArea(
                "Az Alkalmazott Nyilvántartó Rendszer segítségével könnyedén kezelheti " +
                        "alkalmazottainak adatait és munkanaplóit.\n\n" +
                        "Főbb funkciók:\n" +
                        "- Alkalmazottak adatainak kezelése\n" +
                        "- Munkanaplók rögzítése és követése\n" +
                        "- Riportok készítése\n" +
                        "- Excel exportálás\n\n" +
                        "A program használatához válassza ki a megfelelő fület a részletes információkért."
        );
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefRowCount(20);

        content.getChildren().add(textArea);
        return content;
    }

    private VBox createEmployeesContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextArea textArea = new TextArea(
                "Alkalmazottak kezelése:\n\n" +
                        "1. Új alkalmazott felvétele:\n" +
                        "   - Kattintson az 'Új alkalmazott' gombra\n" +
                        "   - Töltse ki a kötelező mezőket\n" +
                        "   - A mentéshez kattintson az OK gombra\n\n" +
                        "2. Alkalmazott szerkesztése:\n" +
                        "   - Válassza ki az alkalmazottat a táblázatból\n" +
                        "   - Kattintson a 'Szerkesztés' gombra\n" +
                        "   - Módosítsa az adatokat\n" +
                        "   - Mentse a változtatásokat\n\n" +
                        "3. Alkalmazott törlése:\n" +
                        "   - Válassza ki az alkalmazottat\n" +
                        "   - Kattintson a 'Törlés' gombra\n" +
                        "   - Erősítse meg a műveletet\n\n" +
                        "4. Keresés:\n" +
                        "   - Használja a kereső mezőt a táblázat felett\n" +
                        "   - A keresés azonnal szűri a találatokat"
        );
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefRowCount(20);

        content.getChildren().add(textArea);
        return content;
    }

    private VBox createWorkRecordsContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextArea textArea = new TextArea(
                "Munkanaplók kezelése:\n\n" +
                        "1. Új munkanapló rögzítése:\n" +
                        "   - Válassza ki az alkalmazottat\n" +
                        "   - Adja meg a dátumot és az időtartamot\n" +
                        "   - Rögzítse a bérezést\n" +
                        "   - Mentse az adatokat\n\n" +
                        "2. Munkanaplók szűrése:\n" +
                        "   - Állítsa be az időszakot\n" +
                        "   - Használja a 'Szűrés' gombot\n\n" +
                        "3. Összesítések megtekintése:\n" +
                        "   - Az időszak összesített adatai a táblázat alatt láthatók\n" +
                        "   - Teljes munkaidő és kifizetések\n\n" +
                        "4. Exportálás:\n" +
                        "   - Az 'Exportálás' gomb az aktuális szűrés eredményét menti\n" +
                        "   - Excel formátumban kerül mentésre"
        );
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefRowCount(20);

        content.getChildren().add(textArea);
        return content;
    }

    private VBox createReportsContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextArea textArea = new TextArea(
                "Riportok készítése:\n\n" +
                        "1. Időszaki jelentés:\n" +
                        "   - Válassza ki az időszakot\n" +
                        "   - Jelölje be a szükséges részleteket\n" +
                        "   - Kattintson a 'Riport generálása' gombra\n\n" +
                        "2. Riport típusok:\n" +
                        "   - Alkalmazotti adatok\n" +
                        "   - Munkanaplók\n" +
                        "   - Összesítések\n\n" +
                        "3. Riportok kezelése:\n" +
                        "   - A generált riportok listája megtekinthető\n" +
                        "   - Megnyitás PDF formátumban\n" +
                        "   - Exportálás különböző formátumokba\n\n" +
                        "4. Automatikus mentés:\n" +
                        "   - A riportok automatikusan mentésre kerülnek\n" +
                        "   - A 'reports' mappában találhatók"
        );
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefRowCount(20);

        content.getChildren().add(textArea);
        return content;
    }
}