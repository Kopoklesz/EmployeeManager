package com.employeemanager.service.interfaces;

import com.employeemanager.model.Customer;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Customer Service interface
 * Vevők kezelése üzleti logikával
 */
public interface CustomerService {

    /**
     * Új vevő létrehozása
     * Validálja az adószámot és egyéb kötelező mezőket
     */
    Customer createCustomer(Customer customer) throws ExecutionException, InterruptedException;

    /**
     * Vevő frissítése
     */
    Customer updateCustomer(Customer customer) throws ExecutionException, InterruptedException;

    /**
     * Vevő törlése
     */
    void deleteCustomer(String id) throws ExecutionException, InterruptedException;

    /**
     * Vevő lekérdezése ID alapján
     */
    Optional<Customer> getCustomerById(String id) throws ExecutionException, InterruptedException;

    /**
     * Összes vevő lekérdezése
     */
    List<Customer> getAllCustomers() throws ExecutionException, InterruptedException;

    /**
     * Vevők lapozva
     */
    Page<Customer> getCustomers(PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Vevő keresése adószám alapján
     */
    Optional<Customer> findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException;

    /**
     * Vevők keresése név szerint (részleges egyezés)
     */
    List<Customer> searchByName(String name) throws ExecutionException, InterruptedException;

    /**
     * Aktív vevők lekérdezése
     */
    List<Customer> getActiveCustomers() throws ExecutionException, InterruptedException;

    /**
     * Aktív vevők lapozva
     */
    Page<Customer> getActiveCustomers(PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Céges vevők lekérdezése
     */
    List<Customer> getCompanyCustomers() throws ExecutionException, InterruptedException;

    /**
     * Vevő aktiválása/deaktiválása
     */
    Customer setCustomerActive(String id, boolean active) throws ExecutionException, InterruptedException;

    /**
     * Adószám validálása
     * Magyar adószám formátum: 8 számjegy
     */
    boolean isValidTaxNumber(String taxNumber);

    /**
     * Email cím validálása
     */
    boolean isValidEmail(String email);

    /**
     * Vevő létezik-e az adószámmal
     */
    boolean existsByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException;
}
