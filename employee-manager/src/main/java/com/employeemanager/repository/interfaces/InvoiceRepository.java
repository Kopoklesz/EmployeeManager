package com.employeemanager.repository.interfaces;

import com.employeemanager.model.Invoice;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Invoice repository interface - számlák adatkezelése
 */
public interface InvoiceRepository extends BaseRepository<Invoice, String> {

    /**
     * Számla keresése számlaszám alapján
     */
    Invoice findByInvoiceNumber(String invoiceNumber) throws ExecutionException, InterruptedException;

    /**
     * Vevő számláinak lekérdezése
     */
    List<Invoice> findByCustomerId(String customerId) throws ExecutionException, InterruptedException;

    /**
     * Vevő számláinak lekérdezése lapozva
     */
    Page<Invoice> findByCustomerId(String customerId, PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Számlák keresése státusz szerint
     */
    List<Invoice> findByStatus(Invoice.InvoiceStatus status) throws ExecutionException, InterruptedException;

    /**
     * Számlák keresése státusz szerint lapozva
     */
    Page<Invoice> findByStatus(Invoice.InvoiceStatus status, PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Fizetett számlák lekérdezése
     */
    List<Invoice> findByIsPaid(boolean isPaid) throws ExecutionException, InterruptedException;

    /**
     * Számlák dátum tartomány szerint
     */
    List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate) throws ExecutionException, InterruptedException;

    /**
     * NAV-ba nem küldött számlák
     */
    List<Invoice> findByIsSentToNav(boolean isSentToNav) throws ExecutionException, InterruptedException;

    /**
     * Lejárt számlák (nincs kifizetve és határidő lejárt)
     */
    List<Invoice> findOverdueInvoices() throws ExecutionException, InterruptedException;

    /**
     * Számlák száma státusz szerint
     */
    long countByStatus(Invoice.InvoiceStatus status) throws ExecutionException, InterruptedException;

    /**
     * Számlák száma vevő szerint
     */
    long countByCustomerId(String customerId) throws ExecutionException, InterruptedException;

    /**
     * Számla mentése tételekkel együtt
     */
    Invoice saveWithItems(Invoice invoice) throws ExecutionException, InterruptedException;
}
