package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Date;
// import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * JDBC alapú Employee repository implementáció
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcEmployeeRepository implements EmployeeRepository {
    
    protected final DataSource dataSource;
    
    @Override
    public Employee save(Employee employee) throws ExecutionException, InterruptedException {
        // Ha nincs ID, generálunk egyet
        if (employee.getId() == null || employee.getId().isEmpty()) {
            employee.setId(UUID.randomUUID().toString());
            return insert(employee);
        } else {
            // Ellenőrizzük, hogy létezik-e már
            Optional<Employee> existing = findById(employee.getId());
            if (existing.isPresent()) {
                return update(employee);
            } else {
                return insert(employee);
            }
        }
    }
    
    private Employee insert(Employee employee) throws ExecutionException {
        String sql = """
            INSERT INTO employees 
            (id, name, birth_place, birth_date, mother_name, tax_number, social_security_number, address)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, employee.getId());
            ps.setString(2, employee.getName());
            ps.setString(3, employee.getBirthPlace());
            ps.setDate(4, employee.getBirthDate() != null ? 
                Date.valueOf(employee.getBirthDate()) : null);
            ps.setString(5, employee.getMotherName());
            ps.setString(6, employee.getTaxNumber());
            ps.setString(7, employee.getSocialSecurityNumber());
            ps.setString(8, employee.getAddress());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Inserted employee with ID: {}", employee.getId());
                return employee;
            } else {
                throw new ExecutionException("Failed to insert employee", null);
            }
            
        } catch (SQLException e) {
            log.error("Error inserting employee", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    private Employee update(Employee employee) throws ExecutionException {
        String sql = """
            UPDATE employees SET 
            name = ?, birth_place = ?, birth_date = ?, mother_name = ?,
            tax_number = ?, social_security_number = ?, address = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getBirthPlace());
            ps.setDate(3, employee.getBirthDate() != null ? 
                Date.valueOf(employee.getBirthDate()) : null);
            ps.setString(4, employee.getMotherName());
            ps.setString(5, employee.getTaxNumber());
            ps.setString(6, employee.getSocialSecurityNumber());
            ps.setString(7, employee.getAddress());
            ps.setString(8, employee.getId());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Updated employee with ID: {}", employee.getId());
                return employee;
            } else {
                throw new ExecutionException("Employee not found for update", null);
            }
            
        } catch (SQLException e) {
            log.error("Error updating employee", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public Optional<Employee> findById(String id) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM employees WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            log.error("Error finding employee by id: {}", id, e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<Employee> findAll() throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM employees ORDER BY name";
        List<Employee> employees = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
            
            log.debug("Found {} employees", employees.size());
            return employees;
            
        } catch (SQLException e) {
            log.error("Error finding all employees", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        String sql = "DELETE FROM employees WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, id);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Deleted employee with ID: {}", id);
            } else {
                log.warn("No employee found with ID: {}", id);
            }
            
        } catch (SQLException e) {
            log.error("Error deleting employee with id: {}", id, e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<Employee> saveAll(List<Employee> entities) throws ExecutionException, InterruptedException {
        List<Employee> savedEmployees = new ArrayList<>();
        
        // Batch insert használata a teljesítmény érdekében
        String insertSql = """
            INSERT INTO employees 
            (id, name, birth_place, birth_date, mother_name, tax_number, social_security_number, address)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Tranzakció kezdete
            
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Employee employee : entities) {
                    // Ha nincs ID, generálunk
                    if (employee.getId() == null || employee.getId().isEmpty()) {
                        employee.setId(UUID.randomUUID().toString());
                    }
                    
                    ps.setString(1, employee.getId());
                    ps.setString(2, employee.getName());
                    ps.setString(3, employee.getBirthPlace());
                    ps.setDate(4, employee.getBirthDate() != null ? 
                        Date.valueOf(employee.getBirthDate()) : null);
                    ps.setString(5, employee.getMotherName());
                    ps.setString(6, employee.getTaxNumber());
                    ps.setString(7, employee.getSocialSecurityNumber());
                    ps.setString(8, employee.getAddress());
                    
                    ps.addBatch();
                    savedEmployees.add(employee);
                }
                
                ps.executeBatch();
                conn.commit(); // Tranzakció véglegesítése
                
                log.debug("Saved {} employees in batch", savedEmployees.size());
                
            } catch (SQLException e) {
                conn.rollback(); // Hiba esetén rollback
                throw e;
            }
            
            return savedEmployees;
            
        } catch (SQLException e) {
            log.error("Error saving employees in batch", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public Optional<Employee> findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM employees WHERE tax_number = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, taxNumber);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            log.error("Error finding employee by tax number: {}", taxNumber, e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public Optional<Employee> findBySocialSecurityNumber(String ssn) throws ExecutionException, InterruptedException {
        String sql = "SELECT * FROM employees WHERE social_security_number = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, ssn);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEmployee(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            log.error("Error finding employee by SSN: {}", ssn, e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    /**
     * ResultSet-ből Employee objektum létrehozása
     */
    protected Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        employee.setId(rs.getString("id"));
        employee.setName(rs.getString("name"));
        employee.setBirthPlace(rs.getString("birth_place"));
        
        Date birthDate = rs.getDate("birth_date");
        if (birthDate != null) {
            employee.setBirthDate(birthDate.toLocalDate());
        }
        
        employee.setMotherName(rs.getString("mother_name"));
        employee.setTaxNumber(rs.getString("tax_number"));
        employee.setSocialSecurityNumber(rs.getString("social_security_number"));
        employee.setAddress(rs.getString("address"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            employee.setCreatedAt(createdAt.toLocalDateTime().toLocalDate());
        }
        
        return employee;
    }
}