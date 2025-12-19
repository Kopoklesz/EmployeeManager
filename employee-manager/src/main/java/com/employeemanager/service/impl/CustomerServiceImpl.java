package com.employeemanager.service.impl;

import com.employeemanager.model.Customer;
import com.employeemanager.model.dto.Page;
import com.employeemanager.model.dto.PageRequest;
import com.employeemanager.repository.interfaces.CustomerRepository;
import com.employeemanager.service.exception.ValidationException;
import com.employeemanager.service.interfaces.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Customer Service implementáció
 * Üzleti logika és validáció a vevők kezeléséhez
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern HUNGARIAN_TAX_NUMBER_PATTERN = Pattern.compile(
            "^\\d{8}(-\\d{1}(-\\d{2})?)?$"
    );

    @Override
    public Customer createCustomer(Customer customer) throws ExecutionException, InterruptedException {
        log.info("Creating new customer: {}", customer.getName());

        // Validálás
        validateCustomer(customer);

        // Ellenőrizzük, hogy az adószám egyedi-e
        if (customer.getTaxNumber() != null && !customer.getTaxNumber().isEmpty()) {
            if (existsByTaxNumber(customer.getTaxNumber())) {
                throw new ValidationException("Vevő már létezik ezzel az adószámmal: " + customer.getTaxNumber());
            }
        }

        // ID generálás ha nincs
        if (customer.getId() == null || customer.getId().isEmpty()) {
            customer.setId(UUID.randomUUID().toString());
        }

        // Alapértelmezett értékek
        if (customer.getIsActive() == null) {
            customer.setIsActive(true);
        }

        if (customer.getPaymentDeadlineDays() == null) {
            customer.setPaymentDeadlineDays(8);
        }

        if (customer.getCountry() == null || customer.getCountry().isEmpty()) {
            customer.setCountry("Magyarország");
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer created successfully with ID: {}", savedCustomer.getId());

        return savedCustomer;
    }

    @Override
    public Customer updateCustomer(Customer customer) throws ExecutionException, InterruptedException {
        log.info("Updating customer: {}", customer.getId());

        // Validálás
        validateCustomer(customer);

        // Ellenőrizzük, hogy létezik-e
        Optional<Customer> existing = customerRepository.findById(customer.getId());
        if (existing.isEmpty()) {
            throw new ValidationException("Vevő nem található ID-val: " + customer.getId());
        }

        // Ha az adószám változott, ellenőrizzük az egyediséget
        Customer existingCustomer = existing.get();
        if (customer.getTaxNumber() != null && !customer.getTaxNumber().isEmpty()) {
            if (!customer.getTaxNumber().equals(existingCustomer.getTaxNumber())) {
                if (existsByTaxNumber(customer.getTaxNumber())) {
                    throw new ValidationException("Másik vevő már létezik ezzel az adószámmal: " + customer.getTaxNumber());
                }
            }
        }

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Customer updated successfully: {}", updatedCustomer.getId());

        return updatedCustomer;
    }

    @Override
    public void deleteCustomer(String id) throws ExecutionException, InterruptedException {
        log.info("Deleting customer: {}", id);

        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            throw new ValidationException("Vevő nem található ID-val: " + id);
        }

        customerRepository.deleteById(id);
        log.info("Customer deleted successfully: {}", id);
    }

    @Override
    public Optional<Customer> getCustomerById(String id) throws ExecutionException, InterruptedException {
        log.debug("Getting customer by ID: {}", id);
        return customerRepository.findById(id);
    }

    @Override
    public List<Customer> getAllCustomers() throws ExecutionException, InterruptedException {
        log.debug("Getting all customers");
        return customerRepository.findAll();
    }

    @Override
    public Page<Customer> getCustomers(PageRequest pageRequest) throws ExecutionException, InterruptedException {
        log.debug("Getting customers with pagination - page: {}, size: {}", pageRequest.getPageNumber(), pageRequest.getPageSize());
        return customerRepository.findAll(pageRequest);
    }

    @Override
    public Optional<Customer> findByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        log.debug("Finding customer by tax number: {}", taxNumber);

        if (taxNumber == null || taxNumber.isEmpty()) {
            return Optional.empty();
        }

        Customer customer = customerRepository.findByTaxNumber(taxNumber);
        return Optional.ofNullable(customer);
    }

    @Override
    public List<Customer> searchByName(String name) throws ExecutionException, InterruptedException {
        log.debug("Searching customers by name: {}", name);

        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }

        return customerRepository.findByNameContaining(name.trim());
    }

    @Override
    public List<Customer> getActiveCustomers() throws ExecutionException, InterruptedException {
        log.debug("Getting active customers");
        return customerRepository.findByIsActive(true);
    }

    @Override
    public Page<Customer> getActiveCustomers(PageRequest pageRequest) throws ExecutionException, InterruptedException {
        log.debug("Getting active customers with pagination");
        return customerRepository.findByIsActive(true, pageRequest);
    }

    @Override
    public List<Customer> getCompanyCustomers() throws ExecutionException, InterruptedException {
        log.debug("Getting company customers");
        return customerRepository.findByIsCompany(true);
    }

    @Override
    public Customer setCustomerActive(String id, boolean active) throws ExecutionException, InterruptedException {
        log.info("Setting customer {} active status to: {}", id, active);

        Optional<Customer> customerOpt = customerRepository.findById(id);
        if (customerOpt.isEmpty()) {
            throw new ValidationException("Vevő nem található ID-val: " + id);
        }

        Customer customer = customerOpt.get();
        customer.setIsActive(active);

        return customerRepository.save(customer);
    }

    @Override
    public boolean isValidTaxNumber(String taxNumber) {
        if (taxNumber == null || taxNumber.trim().isEmpty()) {
            return false;
        }

        // Magyar adószám formátum: 8 számjegy vagy 8-1-2 formátum
        return HUNGARIAN_TAX_NUMBER_PATTERN.matcher(taxNumber.trim()).matches();
    }

    @Override
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true; // Email nem kötelező
        }

        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    @Override
    public boolean existsByTaxNumber(String taxNumber) throws ExecutionException, InterruptedException {
        if (taxNumber == null || taxNumber.isEmpty()) {
            return false;
        }

        Customer customer = customerRepository.findByTaxNumber(taxNumber);
        return customer != null;
    }

    /**
     * Vevő validálása
     */
    private void validateCustomer(Customer customer) {
        if (customer == null) {
            throw new ValidationException("Vevő nem lehet null");
        }

        // Kötelező mezők
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new ValidationException("Vevő neve kötelező");
        }

        // Adószám validálás (ha van)
        if (customer.getTaxNumber() != null && !customer.getTaxNumber().isEmpty()) {
            if (!isValidTaxNumber(customer.getTaxNumber())) {
                throw new ValidationException("Érvénytelen adószám formátum: " + customer.getTaxNumber());
            }
        }

        // Email validálás (ha van)
        if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
            if (!isValidEmail(customer.getEmail())) {
                throw new ValidationException("Érvénytelen email cím: " + customer.getEmail());
            }
        }

        // Cím validálás céges vevőnél
        if (customer.getIsCompany() != null && customer.getIsCompany()) {
            if (customer.getTaxNumber() == null || customer.getTaxNumber().isEmpty()) {
                throw new ValidationException("Céges vevőnél az adószám kötelező");
            }
        }

        // Fizetési határidő validálás
        if (customer.getPaymentDeadlineDays() != null) {
            if (customer.getPaymentDeadlineDays() < 0 || customer.getPaymentDeadlineDays() > 365) {
                throw new ValidationException("Fizetési határidő 0 és 365 nap között lehet");
            }
        }
    }
}
