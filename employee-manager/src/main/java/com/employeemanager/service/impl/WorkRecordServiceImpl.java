package com.employeemanager.service.impl;

import com.employeemanager.database.factory.RepositoryFactory;
import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.employeemanager.service.exception.ServiceException;
import com.employeemanager.service.interfaces.WorkRecordService;
import com.employeemanager.util.ValidationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkRecordServiceImpl implements WorkRecordService {
    private static final Logger logger = LoggerFactory.getLogger(WorkRecordServiceImpl.class);

    // VÁLTOZÁS: RepositoryFactory injektálása a statikus repository helyett
    private final RepositoryFactory repositoryFactory;

    /**
     * Dinamikus WorkRecordRepository lekérése
     */
    private WorkRecordRepository getWorkRecordRepository() {
        WorkRecordRepository repo = repositoryFactory.getWorkRecordRepository();
        logger.debug("Using WorkRecordRepository: {}", repo.getClass().getSimpleName());
        return repo;
    }

    @Override
    @Transactional(readOnly = false)
    public WorkRecord save(WorkRecord workRecord) throws ServiceException {
        try {
            if (!validateWorkRecord(workRecord)) {
                throw new ServiceException("Invalid work record data");
            }
            return getWorkRecordRepository().save(workRecord);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error saving work record", e);
            throw new ServiceException("Failed to save work record", e);
        }
    }

    @Override
    public Optional<WorkRecord> findById(String id) throws ServiceException {
        try {
            return getWorkRecordRepository().findById(id);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding work record by id: " + id, e);
            throw new ServiceException("Failed to find work record", e);
        }
    }

    @Override
    public List<WorkRecord> findAll() throws ServiceException {
        try {
            return getWorkRecordRepository().findAll();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding all work records", e);
            throw new ServiceException("Failed to find all work records", e);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteById(String id) throws ServiceException {
        try {
            getWorkRecordRepository().deleteById(id);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error deleting work record with id: " + id, e);
            throw new ServiceException("Failed to delete work record", e);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public List<WorkRecord> saveAll(List<WorkRecord> records) throws ServiceException {
        try {
            if (records.stream().anyMatch(r -> !validateWorkRecord(r))) {
                throw new ServiceException("Invalid work record data in batch");
            }
            return getWorkRecordRepository().saveAll(records);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error saving multiple work records", e);
            throw new ServiceException("Failed to save work records", e);
        }
    }

    @Override
    public List<WorkRecord> getMonthlyRecords(LocalDate startDate, LocalDate endDate) throws ServiceException {
        try {
            return getWorkRecordRepository().findByWorkDateBetween(startDate, endDate);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error getting monthly records", e);
            throw new ServiceException("Failed to get monthly records", e);
        }
    }

    @Override
    public List<WorkRecord> getEmployeeMonthlyRecords(String employeeId, LocalDate startDate, LocalDate endDate)
            throws ServiceException {
        try {
            return getWorkRecordRepository().findByEmployeeIdAndWorkDateBetween(employeeId, startDate, endDate);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error getting employee monthly records", e);
            throw new ServiceException("Failed to get employee monthly records", e);
        }
    }

    @Override
    public boolean validateWorkRecord(WorkRecord workRecord) {
        return workRecord != null &&
                workRecord.getEmployee() != null &&
                workRecord.getNotificationDate() != null &&
                workRecord.getWorkDate() != null &&
                workRecord.getPayment() != null &&
                workRecord.getPayment().doubleValue() > 0 &&
                ValidationHelper.isValidWorkHours(workRecord.getHoursWorked()) &&
                ValidationHelper.isValidEbevSerial(workRecord.getEbevSerialNumber());
    }

    @Override
    public List<WorkRecord> findByEmployee(Employee employee) throws ServiceException {
        try {
            if (employee == null || employee.getId() == null) {
                throw new ServiceException("Invalid employee data");
            }
            
            logger.debug("Finding work records for employee: {}", employee.getName());
            
            // Az aktuális repository implementáció használata
            WorkRecordRepository repository = getWorkRecordRepository();
            
            // Összes munkanapló lekérése és szűrése az alkalmazott alapján
            List<WorkRecord> allRecords = repository.findAll();
            List<WorkRecord> employeeRecords = allRecords.stream()
                .filter(record -> record.getEmployee() != null 
                    && employee.getId().equals(record.getEmployee().getId()))
                .collect(Collectors.toList());
            
            logger.debug("Found {} work records for employee {}", 
                employeeRecords.size(), employee.getName());
            
            return employeeRecords;
            
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error finding work records for employee: {}", employee.getName(), e);
            throw new ServiceException("Failed to find work records", e);
        }
    }
}