package com.employeemanager.service.impl;

import com.employeemanager.database.factory.RepositoryFactory;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.employeemanager.service.exception.ServiceException;
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
    public void deleteById(String id) throws ServiceException {
        try {
            getWorkRecordRepository().deleteById(id);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Error deleting work record with id: " + id, e);
            throw new ServiceException("Failed to delete work record", e);
        }
    }

    @Override
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
}