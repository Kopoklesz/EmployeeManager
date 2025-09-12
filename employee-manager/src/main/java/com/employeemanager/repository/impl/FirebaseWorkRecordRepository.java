package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.model.WorkRecord;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.employeemanager.repository.interfaces.WorkRecordRepository;
import com.employeemanager.util.FirebaseDateConverter;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// @Repository
@Slf4j
public class FirebaseWorkRecordRepository extends BaseFirebaseRepository<WorkRecord> implements WorkRecordRepository {

    private final EmployeeRepository employeeRepository;

    // @Autowired
    public FirebaseWorkRecordRepository(Firestore firestore, EmployeeRepository employeeRepository) {
        super(firestore, "workrecords", WorkRecord.class);
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected String getEntityId(WorkRecord workRecord) {
        return workRecord.getId();
    }

    @Override
    protected void setEntityId(WorkRecord workRecord, String id) {
        workRecord.setId(id);
    }

    @Override
    protected Map<String, Object> convertToMap(WorkRecord workRecord) {
        return workRecord.toMap();
    }

    @Override
    protected WorkRecord convertFromMap(Map<String, Object> data) {
        try {
            WorkRecord record = WorkRecord.fromMap(data);

            // Employee kapcsolat betöltése
            String employeeId = (String) data.get("employeeId");
            if (employeeId != null) {
                try {
                    employeeRepository.findById(employeeId).ifPresent(record::setEmployee);
                } catch (Exception e) {
                    log.warn("Could not load full employee data for ID: {}, using minimal data", employeeId);
                    // Minimális employee adatok beállítása
                    Employee employee = new Employee();
                    employee.setId(employeeId);
                    employee.setName((String) data.get("employeeName"));
                    record.setEmployee(employee);
                }
            } else {
                log.warn("WorkRecord without employeeId found: {}", data.get("id"));
            }

            return record;
        } catch (Exception e) {
            log.error("Error converting map to WorkRecord. Data: {}", data, e);
            return null;
        }
    }

    @Override
    public List<WorkRecord> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {

        try {
            // Először csak az employeeId alapján szűrünk, majd Java-ban szűrjük a dátumokat
            QuerySnapshot querySnapshot = firestore.collection(collectionName)
                    .whereEqualTo("employeeId", employeeId)
                    .get()
                    .get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> data = doc.getData();
                        {
                            data.put("id", doc.getId());
                            return convertFromMap(data);
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(record -> {
                        // Szűrés dátum alapján Java oldalon
                        LocalDate workDate = record.getWorkDate();
                        return workDate != null &&
                                !workDate.isBefore(startDate) &&
                                !workDate.isAfter(endDate);
                    })
                    .sorted((r1, r2) -> {
                        // Rendezés dátum szerint (csökkenő)
                        LocalDate d1 = r1.getWorkDate();
                        LocalDate d2 = r2.getWorkDate();
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching work records for employee {}: {}", employeeId, e.getMessage(), e);
            throw new ExecutionException("Failed to fetch work records", e);
        }
    }

    @Override
    public List<WorkRecord> findByWorkDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {

        String startDateStr = FirebaseDateConverter.dateToString(startDate);
        String endDateStr = FirebaseDateConverter.dateToString(endDate);

        log.debug("Querying work records between {} and {}", startDateStr, endDateStr);

        try {
            // Egyszerű lekérdezés index nélkül
            QuerySnapshot querySnapshot = firestore.collection(collectionName)
                    .get()
                    .get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> data = doc.getData();
                        {
                            data.put("id", doc.getId());
                            return convertFromMap(data);
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(record -> {
                        // Szűrés dátum alapján Java oldalon
                        LocalDate workDate = record.getWorkDate();
                        return workDate != null &&
                                !workDate.isBefore(startDate) &&
                                !workDate.isAfter(endDate);
                    })
                    .sorted((r1, r2) -> {
                        // Rendezés dátum szerint (csökkenő)
                        LocalDate d1 = r1.getWorkDate();
                        LocalDate d2 = r2.getWorkDate();
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching work records between dates: {}", e.getMessage(), e);
            throw new ExecutionException("Failed to fetch work records", e);
        }
    }

    @Override
    public List<WorkRecord> findByNotificationDateBetween(LocalDate startDate, LocalDate endDate)
            throws ExecutionException, InterruptedException {

        try {
            QuerySnapshot querySnapshot = firestore.collection(collectionName)
                    .get()
                    .get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> data = doc.getData();
                        {
                            data.put("id", doc.getId());
                            return convertFromMap(data);
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(record -> {
                        LocalDate notificationDate = record.getNotificationDate();
                        return notificationDate != null &&
                                !notificationDate.isBefore(startDate) &&
                                !notificationDate.isAfter(endDate);
                    })
                    .sorted((r1, r2) -> {
                        LocalDate d1 = r1.getNotificationDate();
                        LocalDate d2 = r2.getNotificationDate();
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching work records by notification date: {}", e.getMessage(), e);
            throw new ExecutionException("Failed to fetch work records", e);
        }
    }

    @Override
    public List<WorkRecord> findByNotificationDateAndWorkDateBetween(
            LocalDate notifStart, LocalDate notifEnd,
            LocalDate workStart, LocalDate workEnd)
            throws ExecutionException, InterruptedException {

        try {
            QuerySnapshot querySnapshot = firestore.collection(collectionName)
                    .get()
                    .get();

            return querySnapshot.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> data = doc.getData();
                        {
                            data.put("id", doc.getId());
                            return convertFromMap(data);
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(record -> {
                        LocalDate notificationDate = record.getNotificationDate();
                        LocalDate workDate = record.getWorkDate();

                        boolean notifInRange = notificationDate != null &&
                                !notificationDate.isBefore(notifStart) &&
                                !notificationDate.isAfter(notifEnd);

                        boolean workInRange = workDate != null &&
                                !workDate.isBefore(workStart) &&
                                !workDate.isAfter(workEnd);

                        return notifInRange && workInRange;
                    })
                    .sorted((r1, r2) -> {
                        LocalDate d1 = r1.getWorkDate();
                        LocalDate d2 = r2.getWorkDate();
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d2.compareTo(d1);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching work records by both dates: {}", e.getMessage(), e);
            throw new ExecutionException("Failed to fetch work records", e);
        }
    }

    @Override
    public void delete(String id) throws ExecutionException, InterruptedException {
        deleteById(id);  
    }

    @Override
    public List<WorkRecord> findAll() throws ExecutionException, InterruptedException {
        try {
            // Használjuk a firestore.collection(collectionName) hívást
            QuerySnapshot querySnapshot = firestore.collection(collectionName).get().get();
            List<WorkRecord> records = new ArrayList<>();
            
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId()); // Biztosítjuk, hogy az ID benne legyen
                    WorkRecord record = convertFromMap(data);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
            
            log.debug("Found {} work records in collection: {}", records.size(), collectionName);
            return records;
            
        } catch (Exception e) {
            log.error("Failed to fetch all work records", e);
            throw new ExecutionException("Failed to fetch all work records", e);
        }
    }
}