package com.employeemanager.service.interfaces;

import com.employeemanager.model.WorkRecord;
import com.employeemanager.service.exception.ServiceException;

import java.time.LocalDate;
import java.util.List;

public interface WorkRecordService extends BaseService<WorkRecord, String> {
    List<WorkRecord> getMonthlyRecords(LocalDate startDate, LocalDate endDate) throws ServiceException;
    List<WorkRecord> getEmployeeMonthlyRecords(String employeeId, LocalDate startDate, LocalDate endDate) throws ServiceException;
    boolean validateWorkRecord(WorkRecord workRecord);
}