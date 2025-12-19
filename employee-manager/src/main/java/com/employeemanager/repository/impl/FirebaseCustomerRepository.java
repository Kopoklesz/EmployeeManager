package com.employeemanager.repository.impl;

import com.employeemanager.model.Customer;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;
import com.employeemanager.repository.interfaces.CustomerRepository;
import com.google.cloud.firestore.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firebase Customer Repository implementáció
 */
@Slf4j
@Repository("firebaseCustomerRepository")
@ConditionalOnBean(Firestore.class)
public class FirebaseCustomerRepository extends BaseFirebaseRepository<Customer> implements CustomerRepository {

    public FirebaseCustomerRepository(Firestore firestore) {
        super(firestore, "customers", Customer.class);
    }

    @Override
    protected String getEntityId(Customer entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(Customer entity, String id) {
        entity.setId(id);
    }

    @Override
    protected Map<String, Object> convertToMap(Customer entity) {
        return entity.toMap();
    }

    @Override
    protected Customer convertFromMap(Map<String, Object> data) {
        return Customer.fromMap(data);
    }

    @Override
    public Customer findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        if (taxNumber == null || taxNumber.isEmpty()) {
            return null;
        }

        QuerySnapshot querySnapshot = firestore.collection(collectionName)
                .whereEqualTo("taxNumber", taxNumber)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.isEmpty()) {
            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
            Map<String, Object> data = doc.getData();
            if (data != null) {
                data.put("id", doc.getId());
                return convertFromMap(data);
            }
        }

        return null;
    }

    @Override
    public List<Customer> findByNameContaining(String name) throws ExecutionException, InterruptedException {
        if (name == null || name.isEmpty()) {
            return new ArrayList<>();
        }

        // Firebase nem támogatja a LIKE keresést, ezért kliensoldali szűrés
        List<Customer> allCustomers = findAll();
        String searchLower = name.toLowerCase();

        return allCustomers.stream()
                .filter(customer -> customer.getName() != null &&
                        customer.getName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
    }

    @Override
    public List<Customer> findByIsActive(boolean isActive) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(collectionName)
                .whereEqualTo("isActive", isActive)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        return convertFromMap(data);
                    }
                    return null;
                })
                .filter(customer -> customer != null)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Customer> findByIsActive(boolean isActive, PageRequest pageRequest) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(collectionName)
                .whereEqualTo("isActive", isActive)
                .orderBy("name")
                .limit(pageRequest.getPageSize())
                .offset(pageRequest.getOffset());

        QuerySnapshot querySnapshot = query.get().get();

        List<Customer> content = querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        return convertFromMap(data);
                    }
                    return null;
                })
                .filter(customer -> customer != null)
                .collect(Collectors.toList());

        long totalElements = countByIsActive(isActive);

        return Page.of(content, pageRequest, totalElements);
    }

    @Override
    public List<Customer> findByIsCompany(boolean isCompany) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(collectionName)
                .whereEqualTo("isCompany", isCompany)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    if (data != null) {
                        data.put("id", doc.getId());
                        return convertFromMap(data);
                    }
                    return null;
                })
                .filter(customer -> customer != null)
                .collect(Collectors.toList());
    }

    @Override
    public long countByIsActive(boolean isActive) throws ExecutionException, InterruptedException {
        AggregateQuerySnapshot snapshot = firestore.collection(collectionName)
                .whereEqualTo("isActive", isActive)
                .count()
                .get()
                .get();

        return snapshot.getCount();
    }
}
