package com.employeemanager.model.fx;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WarningLevel;
import javafx.beans.property.*;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Employee JavaFX wrapper osztály a táblázathoz
 */
public class EmployeeFX {
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty birthPlace = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
    private final StringProperty motherName = new SimpleStringProperty();
    private final StringProperty taxNumber = new SimpleStringProperty();
    private final StringProperty socialSecurityNumber = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    
    // Figyelmeztetési tulajdonságok
    private final ObjectProperty<WarningLevel> warningLevel = new SimpleObjectProperty<>(WarningLevel.NONE);
    private final StringProperty warningMessage = new SimpleStringProperty("");
    
    @Getter
    private Employee originalEmployee;
    
    public EmployeeFX() {
        // Üres konstruktor új alkalmazotthoz
    }
    
    public EmployeeFX(Employee employee) {
        this.originalEmployee = employee;
        
        setId(employee.getId());
        setName(employee.getName());
        setBirthPlace(employee.getBirthPlace());
        setBirthDate(employee.getBirthDate());
        setMotherName(employee.getMotherName());
        setTaxNumber(employee.getTaxNumber());
        setSocialSecurityNumber(employee.getSocialSecurityNumber());
        setAddress(employee.getAddress());
    }
    
    /**
     * Konvertálás Employee objektummá
     */
    public Employee toEmployee() {
        Employee employee = originalEmployee != null ? originalEmployee : new Employee();
        
        employee.setId(getId());
        employee.setName(getName());
        employee.setBirthPlace(getBirthPlace());
        employee.setBirthDate(getBirthDate());
        employee.setMotherName(getMotherName());
        employee.setTaxNumber(getTaxNumber());
        employee.setSocialSecurityNumber(getSocialSecurityNumber());
        employee.setAddress(getAddress());
        
        return employee;
    }
    
    // Getter/Setter és Property metódusok
    
    public String getId() {
        return id.get();
    }
    
    public void setId(String id) {
        this.id.set(id);
    }
    
    public StringProperty idProperty() {
        return id;
    }
    
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    public String getBirthPlace() {
        return birthPlace.get();
    }
    
    public void setBirthPlace(String birthPlace) {
        this.birthPlace.set(birthPlace);
    }
    
    public StringProperty birthPlaceProperty() {
        return birthPlace;
    }
    
    public LocalDate getBirthDate() {
        return birthDate.get();
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate.set(birthDate);
    }
    
    public ObjectProperty<LocalDate> birthDateProperty() {
        return birthDate;
    }
    
    public String getMotherName() {
        return motherName.get();
    }
    
    public void setMotherName(String motherName) {
        this.motherName.set(motherName);
    }
    
    public StringProperty motherNameProperty() {
        return motherName;
    }
    
    public String getTaxNumber() {
        return taxNumber.get();
    }
    
    public void setTaxNumber(String taxNumber) {
        this.taxNumber.set(taxNumber);
    }
    
    public StringProperty taxNumberProperty() {
        return taxNumber;
    }
    
    public String getSocialSecurityNumber() {
        return socialSecurityNumber.get();
    }
    
    public void setSocialSecurityNumber(String socialSecurityNumber) {
        this.socialSecurityNumber.set(socialSecurityNumber);
    }
    
    public StringProperty socialSecurityNumberProperty() {
        return socialSecurityNumber;
    }
    
    public String getAddress() {
        return address.get();
    }
    
    public void setAddress(String address) {
        this.address.set(address);
    }
    
    public StringProperty addressProperty() {
        return address;
    }
    
    // Figyelmeztetési tulajdonságok getter/setter metódusai
    
    public WarningLevel getWarningLevel() {
        return warningLevel.get();
    }
    
    public void setWarningLevel(WarningLevel level) {
        this.warningLevel.set(level);
    }
    
    public ObjectProperty<WarningLevel> warningLevelProperty() {
        return warningLevel;
    }
    
    public String getWarningMessage() {
        return warningMessage.get();
    }
    
    public void setWarningMessage(String message) {
        this.warningMessage.set(message);
    }
    
    public StringProperty warningMessageProperty() {
        return warningMessage;
    }
    
    /**
     * Figyelmeztetés beállítása
     */
    public void setWarning(WarningLevel level, String message) {
        setWarningLevel(level);
        setWarningMessage(message != null ? message : "");
    }
    
    /**
     * Figyelmeztetés törlése
     */
    public void clearWarning() {
        setWarningLevel(WarningLevel.NONE);
        setWarningMessage("");
    }
}