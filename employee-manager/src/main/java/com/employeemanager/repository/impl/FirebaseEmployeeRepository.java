package com.employeemanager.repository.impl;

import com.employeemanager.model.Employee;
import com.employeemanager.repository.interfaces.EmployeeRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

// @Repository
@Slf4j
public class FirebaseEmployeeRepository extends BaseFirebaseRepository<Employee> implements EmployeeRepository {

    public FirebaseEmployeeRepository(Firestore firestore) {
        super(firestore, "employees", Employee.class);
    }

    @Override
    protected String getEntityId(Employee employee) {
        return employee.getId();
    }

    @Override
    protected void setEntityId(Employee employee, String id) {
        employee.setId(id);
    }

    @Override
    protected Map<String, Object> convertToMap(Employee employee) {
        return employee.toMap();
    }

    @Override
    protected Employee convertFromMap(Map<String, Object> data) {
        return Employee.fromMap(data);
    }

    @Override
    public Optional<Employee> findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        QuerySnapshot query = firestore.collection(collectionName)
                .whereEqualTo("taxNumber", taxNumber)
                .limit(1)
                .get()
                .get();

        if (!query.isEmpty()) {
            Map<String, Object> data = query.getDocuments().get(0).getData();
            if (data != null) {
                data.put("id", query.getDocuments().get(0).getId());
                return Optional.ofNullable(convertFromMap(data));
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Employee> findBySocialSecurityNumber(String ssn) throws ExecutionException, InterruptedException {
        QuerySnapshot query = firestore.collection(collectionName)
                .whereEqualTo("socialSecurityNumber", ssn)
                .limit(1)
                .get()
                .get();

        if (!query.isEmpty()) {
            Map<String, Object> data = query.getDocuments().get(0).getData();
            if (data != null) {
                data.put("id", query.getDocuments().get(0).getId());
                return Optional.ofNullable(convertFromMap(data));
            }
        }

        return Optional.empty();
    }
}