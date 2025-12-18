package com.employeemanager.repository.interfaces;

import com.employeemanager.model.Customer;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Customer repository interface - vevők adatkezelése
 */
public interface CustomerRepository extends BaseRepository<Customer, String> {

    /**
     * Vevő keresése adószám alapján
     */
    Customer findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException;

    /**
     * Vevők keresése név szerint (részleges egyezés)
     */
    List<Customer> findByNameContaining(String name) throws ExecutionException, InterruptedException;

    /**
     * Aktív vevők lekérdezése
     */
    List<Customer> findByIsActive(boolean isActive) throws ExecutionException, InterruptedException;

    /**
     * Aktív vevők lapozva
     */
    Page<Customer> findByIsActive(boolean isActive, PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Céges vevők lekérdezése
     */
    List<Customer> findByIsCompany(boolean isCompany) throws ExecutionException, InterruptedException;

    /**
     * Vevők száma aktív státusz szerint
     */
    long countByIsActive(boolean isActive) throws ExecutionException, InterruptedException;
}
