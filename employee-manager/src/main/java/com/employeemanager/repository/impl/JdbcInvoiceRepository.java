package com.employeemanager.repository.impl;

import com.employeemanager.model.Customer;
import com.employeemanager.model.Invoice;
import com.employeemanager.model.InvoiceItem;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;
import com.employeemanager.repository.interfaces.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * JDBC alapú Invoice repository implementáció
 * Kezeli az invoice_items kapcsolatot is
 */
@Slf4j
@Repository("jdbcInvoiceRepository")
@RequiredArgsConstructor
public class JdbcInvoiceRepository implements InvoiceRepository {

    protected final DataSource dataSource;

    @Override
    public Invoice save(Invoice invoice) throws ExecutionException, InterruptedException {
        if (invoice.getId() == null || invoice.getId().isEmpty()) {
            invoice.setId(UUID.randomUUID().toString());
            return insert(invoice);
        } else {
            Optional<Invoice> existing = findById(invoice.getId());
            if (existing.isPresent()) {
                return update(invoice);
            } else {
                return insert(invoice);
            }
        }
    }

    @Override
    public Invoice saveWithItems(Invoice invoice) throws ExecutionException, InterruptedException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Számla mentése
                Invoice savedInvoice = save(invoice);

                // Régi tételek törlése
                deleteInvoiceItems(conn, savedInvoice.getId());

                // Új tételek mentése
                if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
                    for (int i = 0; i < invoice.getItems().size(); i++) {
                        InvoiceItem item = invoice.getItems().get(i);
                        item.setId(UUID.randomUUID().toString());
                        item.setLineNumber(i + 1);
                        item.setInvoice(savedInvoice);
                        saveInvoiceItem(conn, item);
                    }
                }

                conn.commit();
                log.debug("Saved invoice with {} items: {}", invoice.getItems().size(), invoice.getId());

                // Újra betöltjük tételekkel együtt
                return findById(savedInvoice.getId()).orElse(savedInvoice);

            } catch (Exception e) {
                conn.rollback();
                log.error("Error saving invoice with items", e);
                throw new ExecutionException("Failed to save invoice with items", e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            log.error("Error in transaction", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private Invoice insert(Invoice invoice) throws ExecutionException {
        String sql = """
            INSERT INTO invoices
            (id, invoice_number, customer_id, invoice_date, payment_deadline,
             delivery_date, payment_date, payment_method, currency, exchange_rate,
             net_amount, vat_amount, gross_amount, status, is_paid, is_sent_to_nav,
             nav_transaction_id, nav_sent_at, is_reverse_charge, is_cash_accounting,
             footer_text, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setInvoiceParameters(ps, invoice);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(23, now);
            ps.setTimestamp(24, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Inserted invoice with ID: {}", invoice.getId());
                return invoice;
            } else {
                throw new ExecutionException("Failed to insert invoice", null);
            }

        } catch (SQLException e) {
            log.error("Error inserting invoice", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private Invoice update(Invoice invoice) throws ExecutionException {
        String sql = """
            UPDATE invoices SET
            invoice_number = ?, customer_id = ?, invoice_date = ?, payment_deadline = ?,
            delivery_date = ?, payment_date = ?, payment_method = ?, currency = ?,
            exchange_rate = ?, net_amount = ?, vat_amount = ?, gross_amount = ?,
            status = ?, is_paid = ?, is_sent_to_nav = ?, nav_transaction_id = ?,
            nav_sent_at = ?, is_reverse_charge = ?, is_cash_accounting = ?,
            footer_text = ?, notes = ?, updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, invoice.getInvoiceNumber());
            ps.setString(2, invoice.getCustomer() != null ? invoice.getCustomer().getId() : null);
            ps.setDate(3, Date.valueOf(invoice.getInvoiceDate()));
            ps.setDate(4, invoice.getPaymentDeadline() != null ? Date.valueOf(invoice.getPaymentDeadline()) : null);
            ps.setDate(5, invoice.getDeliveryDate() != null ? Date.valueOf(invoice.getDeliveryDate()) : null);
            ps.setDate(6, invoice.getPaymentDate() != null ? Date.valueOf(invoice.getPaymentDate()) : null);
            ps.setString(7, invoice.getPaymentMethod());
            ps.setString(8, invoice.getCurrency());
            ps.setBigDecimal(9, invoice.getExchangeRate() != null ? invoice.getExchangeRate() : BigDecimal.ONE);
            ps.setBigDecimal(10, invoice.getNetAmount());
            ps.setBigDecimal(11, invoice.getVatAmount());
            ps.setBigDecimal(12, invoice.getGrossAmount());
            ps.setString(13, invoice.getStatus().name());
            ps.setBoolean(14, invoice.getIsPaid() != null ? invoice.getIsPaid() : false);
            ps.setBoolean(15, invoice.getIsSentToNav() != null ? invoice.getIsSentToNav() : false);
            ps.setString(16, invoice.getNavTransactionId());
            ps.setTimestamp(17, invoice.getNavSentAt() != null ? Timestamp.valueOf(invoice.getNavSentAt()) : null);
            ps.setBoolean(18, invoice.getIsReverseCharge() != null ? invoice.getIsReverseCharge() : false);
            ps.setBoolean(19, invoice.getIsCashAccounting() != null ? invoice.getIsCashAccounting() : false);
            ps.setString(20, invoice.getFooterText());
            ps.setString(21, invoice.getNotes());
            ps.setTimestamp(22, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(23, invoice.getId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Updated invoice with ID: {}", invoice.getId());
                return invoice;
            } else {
                throw new ExecutionException("Invoice not found for update", null);
            }

        } catch (SQLException e) {
            log.error("Error updating invoice", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private void setInvoiceParameters(PreparedStatement ps, Invoice invoice) throws SQLException {
        ps.setString(1, invoice.getId());
        ps.setString(2, invoice.getInvoiceNumber());
        ps.setString(3, invoice.getCustomer() != null ? invoice.getCustomer().getId() : null);
        ps.setDate(4, Date.valueOf(invoice.getInvoiceDate()));
        ps.setDate(5, invoice.getPaymentDeadline() != null ? Date.valueOf(invoice.getPaymentDeadline()) : null);
        ps.setDate(6, invoice.getDeliveryDate() != null ? Date.valueOf(invoice.getDeliveryDate()) : null);
        ps.setDate(7, invoice.getPaymentDate() != null ? Date.valueOf(invoice.getPaymentDate()) : null);
        ps.setString(8, invoice.getPaymentMethod());
        ps.setString(9, invoice.getCurrency());
        ps.setBigDecimal(10, invoice.getExchangeRate() != null ? invoice.getExchangeRate() : BigDecimal.ONE);
        ps.setBigDecimal(11, invoice.getNetAmount());
        ps.setBigDecimal(12, invoice.getVatAmount());
        ps.setBigDecimal(13, invoice.getGrossAmount());
        ps.setString(14, invoice.getStatus().name());
        ps.setBoolean(15, invoice.getIsPaid() != null ? invoice.getIsPaid() : false);
        ps.setBoolean(16, invoice.getIsSentToNav() != null ? invoice.getIsSentToNav() : false);
        ps.setString(17, invoice.getNavTransactionId());
        ps.setTimestamp(18, invoice.getNavSentAt() != null ? Timestamp.valueOf(invoice.getNavSentAt()) : null);
        ps.setBoolean(19, invoice.getIsReverseCharge() != null ? invoice.getIsReverseCharge() : false);
        ps.setBoolean(20, invoice.getIsCashAccounting() != null ? invoice.getIsCashAccounting() : false);
        ps.setString(21, invoice.getFooterText());
        ps.setString(22, invoice.getNotes());
    }

    private void saveInvoiceItem(Connection conn, InvoiceItem item) throws SQLException {
        String sql = """
            INSERT INTO invoice_items
            (id, invoice_id, line_number, description, unit_of_measure, quantity,
             unit_price, vat_rate, net_amount, vat_amount, gross_amount,
             discount_percent, discount_amount, vat_exemption_reason, notes, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getInvoice().getId());
            ps.setInt(3, item.getLineNumber());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getUnitOfMeasure());
            ps.setBigDecimal(6, item.getQuantity());
            ps.setBigDecimal(7, item.getUnitPrice());
            ps.setBigDecimal(8, item.getVatRate());
            ps.setBigDecimal(9, item.getNetAmount());
            ps.setBigDecimal(10, item.getVatAmount());
            ps.setBigDecimal(11, item.getGrossAmount());
            ps.setBigDecimal(12, item.getDiscountPercent() != null ? item.getDiscountPercent() : BigDecimal.ZERO);
            ps.setBigDecimal(13, item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO);
            ps.setString(14, item.getVatExemptionReason());
            ps.setString(15, item.getNotes());
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
        }
    }

    private void deleteInvoiceItems(Connection conn, String invoiceId) throws SQLException {
        String sql = "DELETE FROM invoice_items WHERE invoice_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceId);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Invoice> findById(String id) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    // Tételek betöltése
                    invoice.setItems(loadInvoiceItems(conn, id, invoice));
                    return Optional.of(invoice);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            log.error("Error finding invoice by id: {}", id, e);
            throw new ExecutionException("Database error", e);
        }
    }

    private List<InvoiceItem> loadInvoiceItems(Connection conn, String invoiceId, Invoice invoice) throws SQLException {
        String sql = "SELECT * FROM invoice_items WHERE invoice_id = ? ORDER BY line_number";
        List<InvoiceItem> items = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = mapResultSetToInvoiceItem(rs);
                    item.setInvoice(invoice);
                    items.add(item);
                }
            }
        }

        return items;
    }

    @Override
    public List<Invoice> findAll() throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices ORDER BY invoice_date DESC, invoice_number";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Invoice invoice = mapResultSetToInvoice(rs);
                invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                invoices.add(invoice);
            }

            log.debug("Found {} invoices", invoices.size());
            return invoices;

        } catch (SQLException e) {
            log.error("Error finding all invoices", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        String sql = "DELETE FROM invoices WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            int affected = ps.executeUpdate();

            if (affected > 0) {
                log.debug("Deleted invoice with ID: {}", id);
            } else {
                log.warn("No invoice found with ID: {}", id);
            }

        } catch (SQLException e) {
            log.error("Error deleting invoice", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Invoice> saveAll(List<Invoice> entities) throws ExecutionException, InterruptedException {
        List<Invoice> savedInvoices = new ArrayList<>();
        for (Invoice invoice : entities) {
            savedInvoices.add(saveWithItems(invoice));
        }
        return savedInvoices;
    }

    @Override
    public Page<Invoice> findAll(PageRequest pageRequest) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices ORDER BY invoice_date DESC, invoice_number LIMIT ? OFFSET ?";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pageRequest.getPageSize());
            ps.setInt(2, pageRequest.getOffset());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            long totalElements = count();

            return new Page<>(
                    invoices,
                    pageRequest.getPage(),
                    pageRequest.getPageSize(),
                    totalElements
            );

        } catch (SQLException e) {
            log.error("Error finding invoices with pagination", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public long count() throws ExecutionException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM invoices";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            log.error("Error counting invoices", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public Invoice findByInvoiceNumber(String invoiceNumber) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE invoice_number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, invoiceNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    return invoice;
                }
                return null;
            }

        } catch (SQLException e) {
            log.error("Error finding invoice by number: {}", invoiceNumber, e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Invoice> findByCustomerId(String customerId) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE customer_id = ? ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            return invoices;

        } catch (SQLException e) {
            log.error("Error finding invoices by customer", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public Page<Invoice> findByCustomerId(String customerId, PageRequest pageRequest) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE customer_id = ? ORDER BY invoice_date DESC LIMIT ? OFFSET ?";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId);
            ps.setInt(2, pageRequest.getPageSize());
            ps.setInt(3, pageRequest.getOffset());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            long totalElements = countByCustomerId(customerId);

            return new Page<>(
                    invoices,
                    pageRequest.getPage(),
                    pageRequest.getPageSize(),
                    totalElements
            );

        } catch (SQLException e) {
            log.error("Error finding invoices by customer with pagination", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Invoice> findByStatus(Invoice.InvoiceStatus status) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE status = ? ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            return invoices;

        } catch (SQLException e) {
            log.error("Error finding invoices by status", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public Page<Invoice> findByStatus(Invoice.InvoiceStatus status, PageRequest pageRequest) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE status = ? ORDER BY invoice_date DESC LIMIT ? OFFSET ?";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());
            ps.setInt(2, pageRequest.getPageSize());
            ps.setInt(3, pageRequest.getOffset());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            long totalElements = countByStatus(status);

            return new Page<>(
                    invoices,
                    pageRequest.getPage(),
                    pageRequest.getPageSize(),
                    totalElements
            );

        } catch (SQLException e) {
            log.error("Error finding invoices by status with pagination", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Invoice> findByIsPaid(boolean isPaid) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE is_paid = ? ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isPaid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            return invoices;

        } catch (SQLException e) {
            log.error("Error finding invoices by paid status", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE invoice_date BETWEEN ? AND ? ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            return invoices;

        } catch (SQLException e) {
            log.error("Error finding invoices by date range", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Invoice> findByIsSentToNav(boolean isSentToNav) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE is_sent_to_nav = ? ORDER BY invoice_date DESC";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isSentToNav);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                    invoices.add(invoice);
                }
            }

            return invoices;

        } catch (SQLException e) {
            log.error("Error finding invoices by NAV sent status", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Invoice> findOverdueInvoices() throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM invoices WHERE is_paid = FALSE AND payment_deadline < CURRENT_DATE ORDER BY payment_deadline";
        List<Invoice> invoices = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Invoice invoice = mapResultSetToInvoice(rs);
                invoice.setItems(loadInvoiceItems(conn, invoice.getId(), invoice));
                invoices.add(invoice);
            }

            return invoices;

        } catch (SQLException e) {
            log.error("Error finding overdue invoices", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public long countByStatus(Invoice.InvoiceStatus status) throws ExecutionException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM invoices WHERE status = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            log.error("Error counting invoices by status", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public long countByCustomerId(String customerId) throws ExecutionException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM invoices WHERE customer_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            log.error("Error counting invoices by customer", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getString("id"));
        invoice.setInvoiceNumber(rs.getString("invoice_number"));

        // Customer ID-t tárolunk, a tényleges Customer objektumot nem töltjük be (lazy loading)
        String customerId = rs.getString("customer_id");
        if (customerId != null) {
            Customer customer = new Customer();
            customer.setId(customerId);
            invoice.setCustomer(customer);
        }

        Date invoiceDate = rs.getDate("invoice_date");
        if (invoiceDate != null) {
            invoice.setInvoiceDate(invoiceDate.toLocalDate());
        }

        Date paymentDeadline = rs.getDate("payment_deadline");
        if (paymentDeadline != null) {
            invoice.setPaymentDeadline(paymentDeadline.toLocalDate());
        }

        Date deliveryDate = rs.getDate("delivery_date");
        if (deliveryDate != null) {
            invoice.setDeliveryDate(deliveryDate.toLocalDate());
        }

        Date paymentDate = rs.getDate("payment_date");
        if (paymentDate != null) {
            invoice.setPaymentDate(paymentDate.toLocalDate());
        }

        invoice.setPaymentMethod(rs.getString("payment_method"));
        invoice.setCurrency(rs.getString("currency"));
        invoice.setExchangeRate(rs.getBigDecimal("exchange_rate"));
        invoice.setNetAmount(rs.getBigDecimal("net_amount"));
        invoice.setVatAmount(rs.getBigDecimal("vat_amount"));
        invoice.setGrossAmount(rs.getBigDecimal("gross_amount"));
        invoice.setStatus(Invoice.InvoiceStatus.valueOf(rs.getString("status")));
        invoice.setIsPaid(rs.getBoolean("is_paid"));
        invoice.setIsSentToNav(rs.getBoolean("is_sent_to_nav"));
        invoice.setNavTransactionId(rs.getString("nav_transaction_id"));

        Timestamp navSentAt = rs.getTimestamp("nav_sent_at");
        if (navSentAt != null) {
            invoice.setNavSentAt(navSentAt.toLocalDateTime());
        }

        invoice.setIsReverseCharge(rs.getBoolean("is_reverse_charge"));
        invoice.setIsCashAccounting(rs.getBoolean("is_cash_accounting"));
        invoice.setFooterText(rs.getString("footer_text"));
        invoice.setNotes(rs.getString("notes"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            invoice.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            invoice.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return invoice;
    }

    private InvoiceItem mapResultSetToInvoiceItem(ResultSet rs) throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.setId(rs.getString("id"));
        item.setLineNumber(rs.getInt("line_number"));
        item.setDescription(rs.getString("description"));
        item.setUnitOfMeasure(rs.getString("unit_of_measure"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setVatRate(rs.getBigDecimal("vat_rate"));
        item.setNetAmount(rs.getBigDecimal("net_amount"));
        item.setVatAmount(rs.getBigDecimal("vat_amount"));
        item.setGrossAmount(rs.getBigDecimal("gross_amount"));
        item.setDiscountPercent(rs.getBigDecimal("discount_percent"));
        item.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        item.setVatExemptionReason(rs.getString("vat_exemption_reason"));
        item.setNotes(rs.getString("notes"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            item.setCreatedAt(createdAt.toLocalDateTime());
        }

        return item;
    }
}
