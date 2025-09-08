package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
// import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
// import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * JDBC alapú WorkRecord repository implementáció
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcWorkRecordRepository implements WorkRecordRepository {
    
    protected final DataSource dataSource;
    protected final EmployeeRepository employeeRepository;
    
    @Override
    public WorkRecord save(WorkRecord workRecord) throws ExecutionException, InterruptedException {
        // Ha nincs ID, generálunk egyet
        if (workRecord.getId() == null || workRecord.getId().isEmpty()) {
            workRecord.setId(UUID.randomUUID().toString());
            return insert(workRecord);
        } else {
            // Ellenőrizzük, hogy létezik-e már
            Optional<WorkRecord> existing = findById(workRecord.getId());
            if (existing.isPresent()) {
                return update(workRecord);
            } else {
                return insert(workRecord);
            }
        }
    }
    
    private WorkRecord insert(WorkRecord workRecord) throws ExecutionException {
        String sql = """
            INSERT INTO work_records 
            (id, employee_id, notification_date, notification_time, ebev_serial_number, 
             work_date, payment, hours_worked)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, workRecord.getId());
            ps.setString(2, workRecord.getEmployee().getId());
            ps.setDate(3, Date.valueOf(workRecord.getNotificationDate()));
            ps.setTime(4, workRecord.getNotificationTime() != null ? 
                Time.valueOf(workRecord.getNotificationTime()) : null);
            ps.setString(5, workRecord.getEbevSerialNumber());
            ps.setDate(6, Date.valueOf(workRecord.getWorkDate()));
            ps.setBigDecimal(7, workRecord.getPayment());
            ps.setInt(8, workRecord.getHoursWorked());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Inserted work record with ID: {}", workRecord.getId());
                return workRecord;
            } else {
                throw new ExecutionException("Failed to insert work record", null);
            }
            
        } catch (SQLException e) {
            log.error("Error inserting work record", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    private WorkRecord update(WorkRecord workRecord) throws ExecutionException {
        String sql = """
            UPDATE work_records SET 
            employee_id = ?, notification_date = ?, notification_time = ?, 
            ebev_serial_number = ?, work_date = ?, payment = ?, hours_worked = ?
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, workRecord.getEmployee().getId());
            ps.setDate(2, Date.valueOf(workRecord.getNotificationDate()));
            ps.setTime(3, workRecord.getNotificationTime() != null ? 
                Time.valueOf(workRecord.getNotificationTime()) : null);
            ps.setString(4, workRecord.getEbevSerialNumber());
            ps.setDate(5, Date.valueOf(workRecord.getWorkDate()));
            ps.setBigDecimal(6, workRecord.getPayment());
            ps.setInt(7, workRecord.getHoursWorked());
            ps.setString(8, workRecord.getId());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Updated work record with ID: {}", workRecord.getId());
                return workRecord;
            } else {
                throw new ExecutionException("Work record not found for update", null);
            }
            
        } catch (SQLException e) {
            log.error("Error updating work record", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public Optional<WorkRecord> findById(String id) throws ExecutionException, InterruptedException {
        String sql = """
            SELECT wr.*, e.name as employee_name, e.social_security_number 
            FROM work_records wr
            JOIN employees e ON wr.employee_id = e.id
            WHERE wr.id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToWorkRecord(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            log.error("Error finding work record by id: {}", id, e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<WorkRecord> findAll() throws ExecutionException, InterruptedException {
        String sql = """
            SELECT wr.*, e.name as employee_name, e.social_security_number 
            FROM work_records wr
            JOIN employees e ON wr.employee_id = e.id
            ORDER BY wr.work_date DESC, e.name
            """;
        
        List<WorkRecord> workRecords = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                workRecords.add(mapResultSetToWorkRecord(rs));
            }
            
            log.debug("Found {} work records", workRecords.size());
            return workRecords;
            
        } catch (SQLException e) {
            log.error("Error finding all work records", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        String sql = "DELETE FROM work_records WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, id);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                log.debug("Deleted work record with ID: {}", id);
            } else {
                log.warn("No work record found with ID: {}", id);
            }
            
        } catch (SQLException e) {
            log.error("Error deleting work record with id: {}", id, e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<WorkRecord> saveAll(List<WorkRecord> entities) throws ExecutionException, InterruptedException {
        List<WorkRecord> savedRecords = new ArrayList<>();
        
        String insertSql = """
            INSERT INTO work_records 
            (id, employee_id, notification_date, notification_time, ebev_serial_number, 
             work_date, payment, hours_worked)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Tranzakció kezdete
            
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (WorkRecord record : entities) {
                    // Ha nincs ID, generálunk
                    if (record.getId() == null || record.getId().isEmpty()) {
                        record.setId(UUID.randomUUID().toString());
                    }
                    
                    ps.setString(1, record.getId());
                    ps.setString(2, record.getEmployee().getId());
                    ps.setDate(3, Date.valueOf(record.getNotificationDate()));
                    ps.setTime(4, record.getNotificationTime() != null ? 
                        Time.valueOf(record.getNotificationTime()) : null);
                    ps.setString(5, record.getEbevSerialNumber());
                    ps.setDate(6, Date.valueOf(record.getWorkDate()));
                    ps.setBigDecimal(7, record.getPayment());
                    ps.setInt(8, record.getHoursWorked());
                    
                    ps.addBatch();
                    savedRecords.add(record);
                }
                
                ps.executeBatch();
                conn.commit(); // Tranzakció véglegesítése
                
                log.debug("Saved {} work records in batch", savedRecords.size());
                
            } catch (SQLException e) {
                conn.rollback(); // Hiba esetén rollback
                throw e;
            }
            
            return savedRecords;
            
        } catch (SQLException e) {
            log.error("Error saving work records in batch", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<WorkRecord> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        
        String sql = """
            SELECT wr.*, e.name as employee_name, e.social_security_number 
            FROM work_records wr
            JOIN employees e ON wr.employee_id = e.id
            WHERE wr.employee_id = ? AND wr.work_date BETWEEN ? AND ?
            ORDER BY wr.work_date DESC
            """;
        
        List<WorkRecord> workRecords = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, employeeId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workRecords.add(mapResultSetToWorkRecord(rs));
                }
            }
            
            log.debug("Found {} work records for employee {} between {} and {}", 
                workRecords.size(), employeeId, startDate, endDate);
            return workRecords;
            
        } catch (SQLException e) {
            log.error("Error finding work records by employee and date range", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<WorkRecord> findByWorkDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        
        String sql = """
            SELECT wr.*, e.name as employee_name, e.social_security_number 
            FROM work_records wr
            JOIN employees e ON wr.employee_id = e.id
            WHERE wr.work_date BETWEEN ? AND ?
            ORDER BY wr.work_date DESC, e.name
            """;
        
        List<WorkRecord> workRecords = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workRecords.add(mapResultSetToWorkRecord(rs));
                }
            }
            
            log.debug("Found {} work records between {} and {}", 
                workRecords.size(), startDate, endDate);
            return workRecords;
            
        } catch (SQLException e) {
            log.error("Error finding work records by date range", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<WorkRecord> findByNotificationDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {
        
        String sql = """
            SELECT wr.*, e.name as employee_name, e.social_security_number 
            FROM work_records wr
            JOIN employees e ON wr.employee_id = e.id
            WHERE wr.notification_date BETWEEN ? AND ?
            ORDER BY wr.notification_date DESC, e.name
            """;
        
        List<WorkRecord> workRecords = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workRecords.add(mapResultSetToWorkRecord(rs));
                }
            }
            
            log.debug("Found {} work records by notification date between {} and {}", 
                workRecords.size(), startDate, endDate);
            return workRecords;
            
        } catch (SQLException e) {
            log.error("Error finding work records by notification date range", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    @Override
    public List<WorkRecord> findByNotificationDateAndWorkDateBetween(
            LocalDate notifStart, LocalDate notifEnd,
            LocalDate workStart, LocalDate workEnd)
            throws ExecutionException, InterruptedException {
        
        String sql = """
            SELECT wr.*, e.name as employee_name, e.social_security_number 
            FROM work_records wr
            JOIN employees e ON wr.employee_id = e.id
            WHERE wr.notification_date BETWEEN ? AND ?
              AND wr.work_date BETWEEN ? AND ?
            ORDER BY wr.work_date DESC, e.name
            """;
        
        List<WorkRecord> workRecords = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(notifStart));
            ps.setDate(2, Date.valueOf(notifEnd));
            ps.setDate(3, Date.valueOf(workStart));
            ps.setDate(4, Date.valueOf(workEnd));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workRecords.add(mapResultSetToWorkRecord(rs));
                }
            }
            
            log.debug("Found {} work records by both date ranges", workRecords.size());
            return workRecords;
            
        } catch (SQLException e) {
            log.error("Error finding work records by both date ranges", e);
            throw new ExecutionException("Database error", e);
        }
    }
    
    /**
     * ResultSet-ből WorkRecord objektum létrehozása
     */
    protected WorkRecord mapResultSetToWorkRecord(ResultSet rs) throws SQLException, ExecutionException, InterruptedException {
        WorkRecord record = new WorkRecord();
        record.setId(rs.getString("id"));
        
        // Employee betöltése
        String employeeId = rs.getString("employee_id");
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        if (employee.isPresent()) {
            record.setEmployee(employee.get());
        } else {
            // Minimális employee adatok, ha nem találjuk a teljes rekordot
            Employee minimalEmployee = new Employee();
            minimalEmployee.setId(employeeId);
            minimalEmployee.setName(rs.getString("employee_name"));
            minimalEmployee.setSocialSecurityNumber(rs.getString("social_security_number"));
            record.setEmployee(minimalEmployee);
        }
        
        record.setNotificationDate(rs.getDate("notification_date").toLocalDate());
        
        Time notificationTime = rs.getTime("notification_time");
        if (notificationTime != null) {
            record.setNotificationTime(notificationTime.toLocalTime());
        }
        
        record.setEbevSerialNumber(rs.getString("ebev_serial_number"));
        record.setWorkDate(rs.getDate("work_date").toLocalDate());
        record.setPayment(rs.getBigDecimal("payment"));
        record.setHoursWorked(rs.getInt("hours_worked"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return record;
    }
}