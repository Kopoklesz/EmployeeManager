package com.employeemanager.repository.impl;

import com.employeemanager.model.Customer;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;
import com.employeemanager.repository.interfaces.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * JDBC alapú Customer repository implementáció
 */
@Slf4j
@Repository("jdbcCustomerRepository")
@RequiredArgsConstructor
public class JdbcCustomerRepository implements CustomerRepository {

    protected final DataSource dataSource;

    @Override
    public Customer save(Customer customer) throws ExecutionException, InterruptedException {
        if (customer.getId() == null || customer.getId().isEmpty()) {
            customer.setId(UUID.randomUUID().toString());
            return insert(customer);
        } else {
            Optional<Customer> existing = findById(customer.getId());
            if (existing.isPresent()) {
                return update(customer);
            } else {
                return insert(customer);
            }
        }
    }

    private Customer insert(Customer customer) throws ExecutionException {
        String sql = """
            INSERT INTO customers
            (id, name, tax_number, eu_tax_number, zip_code, city, address, country,
             email, phone, contact_person, billing_address, billing_zip_code,
             billing_city, billing_country, payment_deadline_days, notes,
             is_active, is_company, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setCustomerParameters(ps, customer);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(20, now);
            ps.setTimestamp(21, now);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Inserted customer with ID: {}", customer.getId());
                return customer;
            } else {
                throw new ExecutionException("Failed to insert customer", null);
            }

        } catch (SQLException e) {
            log.error("Error inserting customer", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private Customer update(Customer customer) throws ExecutionException {
        String sql = """
            UPDATE customers SET
            name = ?, tax_number = ?, eu_tax_number = ?, zip_code = ?,
            city = ?, address = ?, country = ?, email = ?, phone = ?,
            contact_person = ?, billing_address = ?, billing_zip_code = ?,
            billing_city = ?, billing_country = ?, payment_deadline_days = ?,
            notes = ?, is_active = ?, is_company = ?, updated_at = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, customer.getName());
            ps.setString(2, customer.getTaxNumber());
            ps.setString(3, customer.getEuTaxNumber());
            ps.setString(4, customer.getZipCode());
            ps.setString(5, customer.getCity());
            ps.setString(6, customer.getAddress());
            ps.setString(7, customer.getCountry());
            ps.setString(8, customer.getEmail());
            ps.setString(9, customer.getPhone());
            ps.setString(10, customer.getContactPerson());
            ps.setString(11, customer.getBillingAddress());
            ps.setString(12, customer.getBillingZipCode());
            ps.setString(13, customer.getBillingCity());
            ps.setString(14, customer.getBillingCountry());
            ps.setInt(15, customer.getPaymentDeadlineDays() != null ? customer.getPaymentDeadlineDays() : 8);
            ps.setString(16, customer.getNotes());
            ps.setBoolean(17, customer.getIsActive() != null ? customer.getIsActive() : true);
            ps.setBoolean(18, customer.getIsCompany() != null ? customer.getIsCompany() : true);
            ps.setTimestamp(19, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(20, customer.getId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Updated customer with ID: {}", customer.getId());
                return customer;
            } else {
                throw new ExecutionException("Customer not found for update", null);
            }

        } catch (SQLException e) {
            log.error("Error updating customer", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private void setCustomerParameters(PreparedStatement ps, Customer customer) throws SQLException {
        ps.setString(1, customer.getId());
        ps.setString(2, customer.getName());
        ps.setString(3, customer.getTaxNumber());
        ps.setString(4, customer.getEuTaxNumber());
        ps.setString(5, customer.getZipCode());
        ps.setString(6, customer.getCity());
        ps.setString(7, customer.getAddress());
        ps.setString(8, customer.getCountry() != null ? customer.getCountry() : "Magyarország");
        ps.setString(9, customer.getEmail());
        ps.setString(10, customer.getPhone());
        ps.setString(11, customer.getContactPerson());
        ps.setString(12, customer.getBillingAddress());
        ps.setString(13, customer.getBillingZipCode());
        ps.setString(14, customer.getBillingCity());
        ps.setString(15, customer.getBillingCountry());
        ps.setInt(16, customer.getPaymentDeadlineDays() != null ? customer.getPaymentDeadlineDays() : 8);
        ps.setString(17, customer.getNotes());
        ps.setBoolean(18, customer.getIsActive() != null ? customer.getIsActive() : true);
        ps.setBoolean(19, customer.getIsCompany() != null ? customer.getIsCompany() : true);
    }

    @Override
    public Optional<Customer> findById(String id) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCustomer(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            log.error("Error finding customer by id: {}", id, e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Customer> findAll() throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers ORDER BY name";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }

            log.debug("Found {} customers", customers.size());
            return customers;

        } catch (SQLException e) {
            log.error("Error finding all customers", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        String sql = "DELETE FROM customers WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            int affected = ps.executeUpdate();

            if (affected > 0) {
                log.debug("Deleted customer with ID: {}", id);
            } else {
                log.warn("No customer found with ID: {}", id);
            }

        } catch (SQLException e) {
            log.error("Error deleting customer", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Customer> saveAll(List<Customer> entities) throws ExecutionException, InterruptedException {
        List<Customer> savedCustomers = new ArrayList<>();
        for (Customer customer : entities) {
            savedCustomers.add(save(customer));
        }
        return savedCustomers;
    }

    @Override
    public Page<Customer> findAll(PageRequest pageRequest) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers ORDER BY name LIMIT ? OFFSET ?";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pageRequest.getPageSize());
            ps.setInt(2, pageRequest.getOffset());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }

            long totalElements = count();

            return new Page<>(
                    customers,
                    pageRequest.getPage(),
                    pageRequest.getPageSize(),
                    totalElements
            );

        } catch (SQLException e) {
            log.error("Error finding customers with pagination", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public long count() throws ExecutionException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM customers";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            log.error("Error counting customers", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public Customer findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers WHERE tax_number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, taxNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCustomer(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            log.error("Error finding customer by tax number: {}", taxNumber, e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Customer> findByNameContaining(String name) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers WHERE LOWER(name) LIKE LOWER(?) ORDER BY name";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + name + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }

            log.debug("Found {} customers matching name: {}", customers.size(), name);
            return customers;

        } catch (SQLException e) {
            log.error("Error finding customers by name", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Customer> findByIsActive(boolean isActive) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers WHERE is_active = ? ORDER BY name";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }

            log.debug("Found {} {} customers", customers.size(), isActive ? "active" : "inactive");
            return customers;

        } catch (SQLException e) {
            log.error("Error finding customers by active status", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public Page<Customer> findByIsActive(boolean isActive, PageRequest pageRequest) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers WHERE is_active = ? ORDER BY name LIMIT ? OFFSET ?";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);
            ps.setInt(2, pageRequest.getPageSize());
            ps.setInt(3, pageRequest.getOffset());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }

            long totalElements = countByIsActive(isActive);

            return new Page<>(
                    customers,
                    pageRequest.getPage(),
                    pageRequest.getPageSize(),
                    totalElements
            );

        } catch (SQLException e) {
            log.error("Error finding customers by active status with pagination", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public List<Customer> findByIsCompany(boolean isCompany) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM customers WHERE is_company = ? ORDER BY name";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isCompany);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }

            log.debug("Found {} {} customers", customers.size(), isCompany ? "company" : "individual");
            return customers;

        } catch (SQLException e) {
            log.error("Error finding customers by company status", e);
            throw new ExecutionException("Database error", e);
        }
    }

    @Override
    public long countByIsActive(boolean isActive) throws ExecutionException, InterruptedException {
        String sql = "SELECT COUNT(*) FROM customers WHERE is_active = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, isActive);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            log.error("Error counting customers by active status", e);
            throw new ExecutionException("Database error", e);
        }
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setId(rs.getString("id"));
        customer.setName(rs.getString("name"));
        customer.setTaxNumber(rs.getString("tax_number"));
        customer.setEuTaxNumber(rs.getString("eu_tax_number"));
        customer.setZipCode(rs.getString("zip_code"));
        customer.setCity(rs.getString("city"));
        customer.setAddress(rs.getString("address"));
        customer.setCountry(rs.getString("country"));
        customer.setEmail(rs.getString("email"));
        customer.setPhone(rs.getString("phone"));
        customer.setContactPerson(rs.getString("contact_person"));
        customer.setBillingAddress(rs.getString("billing_address"));
        customer.setBillingZipCode(rs.getString("billing_zip_code"));
        customer.setBillingCity(rs.getString("billing_city"));
        customer.setBillingCountry(rs.getString("billing_country"));
        customer.setPaymentDeadlineDays(rs.getInt("payment_deadline_days"));
        customer.setNotes(rs.getString("notes"));
        customer.setIsActive(rs.getBoolean("is_active"));
        customer.setIsCompany(rs.getBoolean("is_company"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            customer.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            customer.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return customer;
    }
}
