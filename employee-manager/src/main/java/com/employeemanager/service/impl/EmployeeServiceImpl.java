package com.employeemanager.service.impl;

import com.employeemanager.database.factory.RepositoryFactory;
import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.employeemanager.service.exception.ServiceException;
import com.employeemanager.service.interfaces.EmployeeService;
import com.employeemanager.service.interfaces.WorkRecordService;
import com.employeemanager.util.ValidationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    // VÁLTOZÁS: RepositoryFactory injektálása a statikus repository-k helyett
    private final RepositoryFactory repositoryFactory;
    private final WorkRecordService workRecordService;

    /**
     * Dinamikus EmployeeRepository lekérése
     */
    private EmployeeRepository getEmployeeRepository() {
        EmployeeRepository repo = repositoryFactory.getEmployeeRepository();
        logger.debug("Using EmployeeRepository: {}", repo.getClass().getSimpleName());
        return repo;
    }

    /**
     * Dinamikus WorkRecordRepository lekérése
     */
    private WorkRecordRepository getWorkRecordRepository() {
        WorkRecordRepository repo = repositoryFactory.getWorkRecordRepository();
        logger.debug("Using WorkRecordRepository: {}", repo.getClass().getSimpleName());
        return repo;
    }

    @Override
    public Employee save(Employee employee) throws ServiceException {
        try {
            if (!validateEmployee(employee)) {
                throw new ServiceException("Invalid employee data");
            }

            // Ellenőrizzük a unique mezőket új alkalmazott esetén
            if (employee.getId() == null || employee.getId().isEmpty()) {
                // Ellenőrizzük az adószámot
                Optional<Employee> existingByTax = getEmployeeRepository().findByTaxNumber(employee.getTaxNumber());
                if (existingByTax.isPresent()) {
                    throw new ServiceException("Az adószám már létezik a rendszerben");
                }

                // Ellenőrizzük a TAJ számot
                Optional<Employee> existingBySSN = getEmployeeRepository().findBySocialSecurityNumber(employee.getSocialSecurityNumber());
                if (existingBySSN.isPresent()) {
                    throw new ServiceException("A TAJ szám már létezik a rendszerben");
                }
            }

            return getEmployeeRepository().save(employee);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error saving employee", e);
            throw new ServiceException("Failed to save employee", e);
        }
    }

    @Override
    public Optional<Employee> findById(String id) throws ServiceException {
        try {
            return getEmployeeRepository().findById(id);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding employee by id: " + id, e);
            throw new ServiceException("Failed to find employee", e);
        }
    }

    @Override
    public List<Employee> findAll() throws ServiceException {
        try {
            return getEmployeeRepository().findAll();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding all employees", e);
            throw new ServiceException("Failed to find all employees", e);
        }
    }

    @Override
    public void deleteById(String id) throws ServiceException {
        try {
            // Ellenőrizzük, hogy vannak-e kapcsolódó munkanaplók
            List<WorkRecord> employeeRecords = workRecordService.getEmployeeMonthlyRecords(
                    id, LocalDate.of(1900, 1, 1), LocalDate.of(2100, 12, 31));

            if (!employeeRecords.isEmpty()) {
                throw new ServiceException("Nem törölhető az alkalmazott, mert " +
                        employeeRecords.size() + " kapcsolódó munkanapló található");
            }

            getEmployeeRepository().deleteById(id);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error deleting employee with id: " + id, e);
            throw new ServiceException("Failed to delete employee", e);
        }
    }

    @Override
    public List<Employee> saveAll(List<Employee> employees) throws ServiceException {
        try {
            if (employees.stream().anyMatch(e -> !validateEmployee(e))) {
                throw new ServiceException("Invalid employee data in batch");
            }
            return getEmployeeRepository().saveAll(employees);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error saving multiple employees", e);
            throw new ServiceException("Failed to save employees", e);
        }
    }

    @Override
    public Optional<Employee> findByTaxNumber(String taxNumber) throws ServiceException {
        try {
            return getEmployeeRepository().findByTaxNumber(taxNumber);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding employee by tax number: " + taxNumber, e);
            throw new ServiceException("Failed to find employee by tax number", e);
        }
    }

    @Override
    public Optional<Employee> findBySocialSecurityNumber(String ssn) throws ServiceException {
        try {
            return getEmployeeRepository().findBySocialSecurityNumber(ssn);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding employee by SSN: " + ssn, e);
            throw new ServiceException("Failed to find employee by SSN", e);
        }
    }

    @Override
    public boolean validateEmployee(Employee employee) {
        return employee != null &&
                ValidationHelper.isValidName(employee.getName()) &&
                ValidationHelper.isValidTaxNumber(employee.getTaxNumber()) &&
                ValidationHelper.isValidSocialSecurityNumber(employee.getSocialSecurityNumber()) &&
                ValidationHelper.isValidBirthDate(employee.getBirthDate());
    }

    @Override
    public WorkRecord addWorkRecord(WorkRecord workRecord) throws ServiceException {
        if (workRecord == null || workRecord.getEmployee() == null) {
            throw new ServiceException("Invalid work record data");
        }

        try {
            // Ellenőrizzük, hogy létezik-e az alkalmazott
            Optional<Employee> employee = findById(workRecord.getEmployee().getId());
            if (employee.isEmpty()) {
                throw new ServiceException("Employee not found");
            }

            // Validáljuk és mentsük a munkanaplót
            if (!workRecordService.validateWorkRecord(workRecord)) {
                throw new ServiceException("Invalid work record data");
            }

            return workRecordService.save(workRecord);
        } catch (Exception e) {
            logger.error("Error adding work record", e);
            throw new ServiceException("Failed to add work record", e);
        }
    }

    @Override
    public List<WorkRecord> addWorkRecords(List<WorkRecord> workRecords) throws ServiceException {
        if (workRecords == null || workRecords.isEmpty()) {
            throw new ServiceException("No work records to add");
        }

        try {
            // Ellenőrizzük minden rekordnál az alkalmazottat
            for (WorkRecord record : workRecords) {
                if (record.getEmployee() == null) {
                    throw new ServiceException("Invalid work record data - missing employee");
                }

                Optional<Employee> employee = findById(record.getEmployee().getId());
                if (employee.isEmpty()) {
                    throw new ServiceException("Employee not found: " + record.getEmployee().getId());
                }

                if (!workRecordService.validateWorkRecord(record)) {
                    throw new ServiceException("Invalid work record data");
                }
            }

            // Batch mentés
            return workRecordService.saveAll(workRecords);

        } catch (Exception e) {
            logger.error("Error adding multiple work records", e);
            throw new ServiceException("Failed to add work records", e);
        }
    }

    @Override
    public List<WorkRecord> getMonthlyRecords(LocalDate startDate, LocalDate endDate) throws ServiceException {
        return workRecordService.getMonthlyRecords(startDate, endDate);
    }

    @Override
    public List<WorkRecord> getEmployeeMonthlyRecords(String employeeId, LocalDate startDate, LocalDate endDate) throws ServiceException {
        return workRecordService.getEmployeeMonthlyRecords(employeeId, startDate, endDate);
    }

    @Override
    public void deleteWorkRecord(String id) throws ServiceException {
        workRecordService.deleteById(id);
    }

    @Override
    public List<WorkRecord> getRecordsByNotificationDate(LocalDate startDate, LocalDate endDate) throws ServiceException {
        try {
            return getWorkRecordRepository().findByNotificationDateBetween(startDate, endDate);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error getting records by notification date", e);
            throw new ServiceException("Failed to get records by notification date", e);
        }
    }

    @Override
    public List<WorkRecord> getRecordsByBothDates(LocalDate notifStart, LocalDate notifEnd,
                                                  LocalDate workStart, LocalDate workEnd) throws ServiceException {
        try {
            return getWorkRecordRepository().findByNotificationDateAndWorkDateBetween(
                    notifStart, notifEnd, workStart, workEnd);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error getting records by both dates", e);
            throw new ServiceException("Failed to get records by both dates", e);
        }
    }
}