package com.employeemanager.service.interfaces;

import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Invoice Service interface
 * Számlák kezelése üzleti logikával
 */
public interface InvoiceService {

    /**
     * Új számla létrehozása
     * Automatikusan generálja a számlaszámot és kiszámítja az összegeket
     */
    Invoice createInvoice(Invoice invoice) throws ExecutionException, InterruptedException;

    /**
     * Számla frissítése
     * Újraszámolja az összegeket
     */
    Invoice updateInvoice(Invoice invoice) throws ExecutionException, InterruptedException;

    /**
     * Számla törlése
     */
    void deleteInvoice(String id) throws ExecutionException, InterruptedException;

    /**
     * Számla lekérdezése ID alapján (tételekkel együtt)
     */
    Optional<Invoice> getInvoiceById(String id) throws ExecutionException, InterruptedException;

    /**
     * Összes számla lekérdezése
     */
    List<Invoice> getAllInvoices() throws ExecutionException, InterruptedException;

    /**
     * Számlák lapozva
     */
    Page<Invoice> getInvoices(PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Számla keresése számlaszám alapján
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber) throws ExecutionException, InterruptedException;

    /**
     * Vevő számláinak lekérdezése
     */
    List<Invoice> getInvoicesByCustomer(String customerId) throws ExecutionException, InterruptedException;

    /**
     * Vevő számláinak lekérdezése lapozva
     */
    Page<Invoice> getInvoicesByCustomer(String customerId, PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Számlák keresése státusz szerint
     */
    List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) throws ExecutionException, InterruptedException;

    /**
     * Számlák státusz szerint lapozva
     */
    Page<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status, PageRequest pageRequest) throws ExecutionException, InterruptedException;

    /**
     * Fizetett/fizetetlen számlák
     */
    List<Invoice> getInvoicesByPaymentStatus(boolean isPaid) throws ExecutionException, InterruptedException;

    /**
     * Számlák dátum tartomány szerint
     */
    List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) throws ExecutionException, InterruptedException;

    /**
     * NAV-ba nem küldött számlák
     */
    List<Invoice> getInvoicesNotSentToNav() throws ExecutionException, InterruptedException;

    /**
     * Lejárt számlák
     */
    List<Invoice> getOverdueInvoices() throws ExecutionException, InterruptedException;

    /**
     * Számla státusz változtatása
     */
    Invoice changeInvoiceStatus(String id, Invoice.InvoiceStatus newStatus) throws ExecutionException, InterruptedException;

    /**
     * Számla kiállítása (DRAFT → ISSUED)
     * Validálja az összes kötelező mezőt
     */
    Invoice issueInvoice(String id) throws ExecutionException, InterruptedException;

    /**
     * Számla fizetettként jelölése
     */
    Invoice markAsPaid(String id, LocalDate paymentDate) throws ExecutionException, InterruptedException;

    /**
     * Számla sztornózása
     */
    Invoice cancelInvoice(String id, String reason) throws ExecutionException, InterruptedException;

    /**
     * Tétel hozzáadása számlához
     * Újraszámolja az összegeket
     */
    Invoice addItemToInvoice(String invoiceId, InvoiceItem item) throws ExecutionException, InterruptedException;

    /**
     * Tétel eltávolítása számlából
     */
    Invoice removeItemFromInvoice(String invoiceId, String itemId) throws ExecutionException, InterruptedException;

    /**
     * Számla összegek újraszámítása
     */
    Invoice recalculateInvoiceAmounts(String invoiceId) throws ExecutionException, InterruptedException;

    /**
     * Számla validálása kiállítás előtt
     * Ellenőrzi a jogszabályi követelményeket
     */
    boolean validateInvoiceForIssue(Invoice invoice);

    /**
     * NAV-ba küldött státusz beállítása
     */
    Invoice markAsSentToNav(String id, String transactionId) throws ExecutionException, InterruptedException;

    /**
     * Statisztikák - összesített értékek
     */
    InvoiceStatistics getStatistics() throws ExecutionException, InterruptedException;

    /**
     * Statisztikák vevőnként
     */
    InvoiceStatistics getStatisticsByCustomer(String customerId) throws ExecutionException, InterruptedException;

    /**
     * StatisztikaDTO
     */
    record InvoiceStatistics(
            long totalCount,
            long draftCount,
            long issuedCount,
            long paidCount,
            long overdueCount,
            java.math.BigDecimal totalNetAmount,
            java.math.BigDecimal totalGrossAmount,
            java.math.BigDecimal unpaidAmount
    ) {}
}
