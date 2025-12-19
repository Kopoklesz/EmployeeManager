package com.employeemanager.service.impl;

import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;
import com.employeemanager.repository.interfaces.CompanySettingsRepository;
import com.employeemanager.repository.interfaces.InvoiceRepository;
import com.employeemanager.service.exception.ValidationException;
import com.employeemanager.service.interfaces.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Invoice Service implementáció
 * Számlák kezelése üzleti logikával
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CompanySettingsRepository companySettingsRepository;

    @Override
    public Invoice createInvoice(Invoice invoice) throws ExecutionException, InterruptedException {
        log.info("Creating new invoice for customer: {}", invoice.getCustomer() != null ? invoice.getCustomer().getId() : "null");

        // Validálás
        validateInvoice(invoice);

        // ID generálás
        if (invoice.getId() == null || invoice.getId().isEmpty()) {
            invoice.setId(UUID.randomUUID().toString());
        }

        // Számlaszám generálás
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            String invoiceNumber = companySettingsRepository.generateNextInvoiceNumber();
            invoice.setInvoiceNumber(invoiceNumber);
            log.info("Generated invoice number: {}", invoiceNumber);
        }

        // Alapértelmezett értékek
        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        }

        if (invoice.getIsPaid() == null) {
            invoice.setIsPaid(false);
        }

        if (invoice.getIsSentToNav() == null) {
            invoice.setIsSentToNav(false);
        }

        if (invoice.getIsReverseCharge() == null) {
            invoice.setIsReverseCharge(false);
        }

        if (invoice.getIsCashAccounting() == null) {
            invoice.setIsCashAccounting(false);
        }

        if (invoice.getExchangeRate() == null) {
            invoice.setExchangeRate(BigDecimal.ONE); // NAV követelmény!
        }

        // Tételek ID generálás
        if (invoice.getItems() != null) {
            for (int i = 0; i < invoice.getItems().size(); i++) {
                InvoiceItem item = invoice.getItems().get(i);
                if (item.getId() == null || item.getId().isEmpty()) {
                    item.setId(UUID.randomUUID().toString());
                }
                item.setLineNumber(i + 1);
                item.setInvoice(invoice);
                item.calculateAmounts();
            }
        }

        // Összegek újraszámítása
        invoice.recalculateAmounts();

        // Mentés tételekkel együtt
        Invoice savedInvoice = invoiceRepository.saveWithItems(invoice);
        log.info("Invoice created successfully: {} - {}", savedInvoice.getInvoiceNumber(), savedInvoice.getId());

        return savedInvoice;
    }

    @Override
    public Invoice updateInvoice(Invoice invoice) throws ExecutionException, InterruptedException {
        log.info("Updating invoice: {}", invoice.getId());

        // Ellenőrizzük, hogy létezik-e
        Optional<Invoice> existing = invoiceRepository.findById(invoice.getId());
        if (existing.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + invoice.getId());
        }

        Invoice existingInvoice = existing.get();

        // Ha a számla már ki van állítva vagy befizetve, csak korlátozottan módosítható
        if (existingInvoice.getStatus() == Invoice.InvoiceStatus.ISSUED ||
            existingInvoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            log.warn("Attempting to modify issued/paid invoice: {}", invoice.getId());
            // Csak a fizetési adatok és megjegyzések módosíthatók
        }

        // Validálás
        validateInvoice(invoice);

        // Tételek frissítése
        if (invoice.getItems() != null) {
            for (int i = 0; i < invoice.getItems().size(); i++) {
                InvoiceItem item = invoice.getItems().get(i);
                if (item.getId() == null || item.getId().isEmpty()) {
                    item.setId(UUID.randomUUID().toString());
                }
                item.setLineNumber(i + 1);
                item.setInvoice(invoice);
                item.calculateAmounts();
            }
        }

        // Összegek újraszámítása
        invoice.recalculateAmounts();

        // Mentés
        Invoice updatedInvoice = invoiceRepository.saveWithItems(invoice);
        log.info("Invoice updated successfully: {}", updatedInvoice.getId());

        return updatedInvoice;
    }

    @Override
    public void deleteInvoice(String id) throws ExecutionException, InterruptedException {
        log.info("Deleting invoice: {}", id);

        Optional<Invoice> invoice = invoiceRepository.findById(id);
        if (invoice.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + id);
        }

        Invoice existingInvoice = invoice.get();

        // Csak piszkozat számla törölhető
        if (existingInvoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new ValidationException("Csak piszkozat számla törölhető. Használja a sztornózást!");
        }

        invoiceRepository.deleteById(id);
        log.info("Invoice deleted successfully: {}", id);
    }

    @Override
    public Optional<Invoice> getInvoiceById(String id) throws ExecutionException, InterruptedException {
        log.debug("Getting invoice by ID: {}", id);
        return invoiceRepository.findById(id);
    }

    @Override
    public List<Invoice> getAllInvoices() throws ExecutionException, InterruptedException {
        log.debug("Getting all invoices");
        return invoiceRepository.findAll();
    }

    @Override
    public Page<Invoice> getInvoices(PageRequest pageRequest) throws ExecutionException, InterruptedException {
        log.debug("Getting invoices with pagination");
        return invoiceRepository.findAll(pageRequest);
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) throws ExecutionException, InterruptedException {
        log.debug("Finding invoice by number: {}", invoiceNumber);
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber);
        return Optional.ofNullable(invoice);
    }

    @Override
    public List<Invoice> getInvoicesByCustomer(String customerId) throws ExecutionException, InterruptedException {
        log.debug("Getting invoices for customer: {}", customerId);
        return invoiceRepository.findByCustomerId(customerId);
    }

    @Override
    public Page<Invoice> getInvoicesByCustomer(String customerId, PageRequest pageRequest) throws ExecutionException, InterruptedException {
        log.debug("Getting invoices for customer with pagination: {}", customerId);
        return invoiceRepository.findByCustomerId(customerId, pageRequest);
    }

    @Override
    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) throws ExecutionException, InterruptedException {
        log.debug("Getting invoices by status: {}", status);
        return invoiceRepository.findByStatus(status);
    }

    @Override
    public Page<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status, PageRequest pageRequest) throws ExecutionException, InterruptedException {
        log.debug("Getting invoices by status with pagination: {}", status);
        return invoiceRepository.findByStatus(status, pageRequest);
    }

    @Override
    public List<Invoice> getInvoicesByPaymentStatus(boolean isPaid) throws ExecutionException, InterruptedException {
        log.debug("Getting invoices by payment status: {}", isPaid);
        return invoiceRepository.findByIsPaid(isPaid);
    }

    @Override
    public List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) throws ExecutionException, InterruptedException {
        log.debug("Getting invoices between {} and {}", startDate, endDate);
        return invoiceRepository.findByInvoiceDateBetween(startDate, endDate);
    }

    @Override
    public List<Invoice> getInvoicesNotSentToNav() throws ExecutionException, InterruptedException {
        log.debug("Getting invoices not sent to NAV");
        return invoiceRepository.findByIsSentToNav(false);
    }

    @Override
    public List<Invoice> getOverdueInvoices() throws ExecutionException, InterruptedException {
        log.debug("Getting overdue invoices");
        return invoiceRepository.findOverdueInvoices();
    }

    @Override
    public Invoice changeInvoiceStatus(String id, Invoice.InvoiceStatus newStatus) throws ExecutionException, InterruptedException {
        log.info("Changing invoice {} status to: {}", id, newStatus);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + id);
        }

        Invoice invoice = invoiceOpt.get();
        Invoice.InvoiceStatus oldStatus = invoice.getStatus();

        // Státusz váltás validálása
        validateStatusChange(oldStatus, newStatus);

        invoice.setStatus(newStatus);

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice issueInvoice(String id) throws ExecutionException, InterruptedException {
        log.info("Issuing invoice: {}", id);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + id);
        }

        Invoice invoice = invoiceOpt.get();

        // Csak piszkozat számlát lehet kiállítani
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new ValidationException("Csak piszkozat számla állítható ki!");
        }

        // Validálás
        if (!validateInvoiceForIssue(invoice)) {
            throw new ValidationException("A számla nem felel meg a kiállítási követelményeknek!");
        }

        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice markAsPaid(String id, LocalDate paymentDate) throws ExecutionException, InterruptedException {
        log.info("Marking invoice {} as paid on: {}", id, paymentDate);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + id);
        }

        Invoice invoice = invoiceOpt.get();

        invoice.setIsPaid(true);
        invoice.setPaymentDate(paymentDate);
        invoice.setStatus(Invoice.InvoiceStatus.PAID);

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice cancelInvoice(String id, String reason) throws ExecutionException, InterruptedException {
        log.info("Cancelling invoice: {} - Reason: {}", id, reason);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + id);
        }

        Invoice invoice = invoiceOpt.get();

        // Csak kiállított számlát lehet sztornózni
        if (invoice.getStatus() != Invoice.InvoiceStatus.ISSUED &&
            invoice.getStatus() != Invoice.InvoiceStatus.SENT) {
            throw new ValidationException("Csak kiállított számla sztornózható!");
        }

        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + "\n" : "") +
                         "Sztornózva: " + reason);

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice addItemToInvoice(String invoiceId, InvoiceItem item) throws ExecutionException, InterruptedException {
        log.info("Adding item to invoice: {}", invoiceId);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + invoiceId);
        }

        Invoice invoice = invoiceOpt.get();

        // Csak piszkozat számlához adható tétel
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new ValidationException("Csak piszkozat számlához adható új tétel!");
        }

        // Tétel validálás
        if (item.getDescription() == null || item.getDescription().isEmpty()) {
            throw new ValidationException("Tétel megnevezése kötelező!");
        }

        if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Érvénytelen egységár!");
        }

        if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Érvénytelen mennyiség!");
        }

        // ID generálás
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }

        // Tétel hozzáadása
        invoice.addItem(item);

        return invoiceRepository.saveWithItems(invoice);
    }

    @Override
    public Invoice removeItemFromInvoice(String invoiceId, String itemId) throws ExecutionException, InterruptedException {
        log.info("Removing item {} from invoice: {}", itemId, invoiceId);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + invoiceId);
        }

        Invoice invoice = invoiceOpt.get();

        // Csak piszkozat számlából törölhető tétel
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new ValidationException("Csak piszkozat számlából törölhető tétel!");
        }

        // Tétel keresése és törlése
        InvoiceItem itemToRemove = invoice.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Tétel nem található ID-val: " + itemId));

        invoice.removeItem(itemToRemove);

        return invoiceRepository.saveWithItems(invoice);
    }

    @Override
    public Invoice recalculateInvoiceAmounts(String invoiceId) throws ExecutionException, InterruptedException {
        log.info("Recalculating amounts for invoice: {}", invoiceId);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + invoiceId);
        }

        Invoice invoice = invoiceOpt.get();

        // Tételek újraszámítása
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                item.calculateAmounts();
            }
        }

        // Számla összegek újraszámítása
        invoice.recalculateAmounts();

        return invoiceRepository.save(invoice);
    }

    @Override
    public boolean validateInvoiceForIssue(Invoice invoice) {
        if (invoice == null) {
            return false;
        }

        // Kötelező mezők ellenőrzése (2007. évi CXXVII. törvény)
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            log.warn("Validation failed: Missing invoice number");
            return false;
        }

        if (invoice.getCustomer() == null) {
            log.warn("Validation failed: Missing customer");
            return false;
        }

        if (invoice.getInvoiceDate() == null) {
            log.warn("Validation failed: Missing invoice date");
            return false;
        }

        if (invoice.getDeliveryDate() == null) {
            log.warn("Validation failed: Missing delivery date");
            return false;
        }

        if (invoice.getPaymentDeadline() == null) {
            log.warn("Validation failed: Missing payment deadline");
            return false;
        }

        if (invoice.getPaymentMethod() == null) {
            log.warn("Validation failed: Missing payment method");
            return false;
        }

        if (invoice.getCurrency() == null || invoice.getCurrency().isEmpty()) {
            log.warn("Validation failed: Missing currency");
            return false;
        }

        // NAV követelmény: exchange_rate kötelező!
        if (invoice.getExchangeRate() == null) {
            log.warn("Validation failed: Missing exchange rate (NAV requirement!)");
            return false;
        }

        // Legalább 1 tétel szükséges
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            log.warn("Validation failed: No items");
            return false;
        }

        // Összegek ellenőrzése
        if (invoice.getNetAmount() == null || invoice.getNetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Validation failed: Invalid net amount");
            return false;
        }

        return true;
    }

    @Override
    public Invoice markAsSentToNav(String id, String transactionId) throws ExecutionException, InterruptedException {
        log.info("Marking invoice {} as sent to NAV with transaction ID: {}", id, transactionId);

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(id);
        if (invoiceOpt.isEmpty()) {
            throw new ValidationException("Számla nem található ID-val: " + id);
        }

        Invoice invoice = invoiceOpt.get();

        invoice.setIsSentToNav(true);
        invoice.setNavTransactionId(transactionId);
        invoice.setNavSentAt(LocalDateTime.now());

        if (invoice.getStatus() == Invoice.InvoiceStatus.ISSUED) {
            invoice.setStatus(Invoice.InvoiceStatus.SENT);
        }

        return invoiceRepository.save(invoice);
    }

    @Override
    public InvoiceStatistics getStatistics() throws ExecutionException, InterruptedException {
        log.debug("Calculating invoice statistics");

        List<Invoice> allInvoices = invoiceRepository.findAll();

        long totalCount = allInvoices.size();
        long draftCount = invoiceRepository.countByStatus(Invoice.InvoiceStatus.DRAFT);
        long issuedCount = invoiceRepository.countByStatus(Invoice.InvoiceStatus.ISSUED);
        long paidCount = invoiceRepository.countByStatus(Invoice.InvoiceStatus.PAID);

        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices();
        long overdueCount = overdueInvoices.size();

        BigDecimal totalNetAmount = allInvoices.stream()
                .map(Invoice::getNetAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGrossAmount = allInvoices.stream()
                .map(Invoice::getGrossAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unpaidAmount = allInvoices.stream()
                .filter(invoice -> !invoice.getIsPaid())
                .map(Invoice::getGrossAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InvoiceStatistics(
                totalCount,
                draftCount,
                issuedCount,
                paidCount,
                overdueCount,
                totalNetAmount,
                totalGrossAmount,
                unpaidAmount
        );
    }

    @Override
    public InvoiceStatistics getStatisticsByCustomer(String customerId) throws ExecutionException, InterruptedException {
        log.debug("Calculating invoice statistics for customer: {}", customerId);

        List<Invoice> customerInvoices = invoiceRepository.findByCustomerId(customerId);

        long totalCount = customerInvoices.size();
        long draftCount = customerInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.DRAFT)
                .count();
        long issuedCount = customerInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.ISSUED)
                .count();
        long paidCount = customerInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PAID)
                .count();
        long overdueCount = customerInvoices.stream()
                .filter(Invoice::isOverdue)
                .count();

        BigDecimal totalNetAmount = customerInvoices.stream()
                .map(Invoice::getNetAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGrossAmount = customerInvoices.stream()
                .map(Invoice::getGrossAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unpaidAmount = customerInvoices.stream()
                .filter(invoice -> !invoice.getIsPaid())
                .map(Invoice::getGrossAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InvoiceStatistics(
                totalCount,
                draftCount,
                issuedCount,
                paidCount,
                overdueCount,
                totalNetAmount,
                totalGrossAmount,
                unpaidAmount
        );
    }

    /**
     * Számla validálása
     */
    private void validateInvoice(Invoice invoice) {
        if (invoice == null) {
            throw new ValidationException("Számla nem lehet null");
        }

        if (invoice.getCustomer() == null) {
            throw new ValidationException("Vevő kötelező");
        }

        if (invoice.getInvoiceDate() == null) {
            throw new ValidationException("Számla dátuma kötelező");
        }

        if (invoice.getCurrency() == null || invoice.getCurrency().isEmpty()) {
            throw new ValidationException("Pénznem kötelező");
        }
    }

    /**
     * Státusz váltás validálása
     */
    private void validateStatusChange(Invoice.InvoiceStatus oldStatus, Invoice.InvoiceStatus newStatus) {
        // DRAFT → bármi
        if (oldStatus == Invoice.InvoiceStatus.DRAFT) {
            return; // Minden engedélyezett
        }

        // ISSUED → SENT, PAID, CANCELLED
        if (oldStatus == Invoice.InvoiceStatus.ISSUED) {
            if (newStatus != Invoice.InvoiceStatus.SENT &&
                newStatus != Invoice.InvoiceStatus.PAID &&
                newStatus != Invoice.InvoiceStatus.CANCELLED) {
                throw new ValidationException("Érvénytelen státusz váltás: " + oldStatus + " → " + newStatus);
            }
            return;
        }

        // SENT → PAID, CANCELLED
        if (oldStatus == Invoice.InvoiceStatus.SENT) {
            if (newStatus != Invoice.InvoiceStatus.PAID &&
                newStatus != Invoice.InvoiceStatus.CANCELLED) {
                throw new ValidationException("Érvénytelen státusz váltás: " + oldStatus + " → " + newStatus);
            }
            return;
        }

        // PAID és CANCELLED végállapotok
        if (oldStatus == Invoice.InvoiceStatus.PAID || oldStatus == Invoice.InvoiceStatus.CANCELLED) {
            throw new ValidationException("Befejezett számla státusza nem változtatható!");
        }
    }
}
