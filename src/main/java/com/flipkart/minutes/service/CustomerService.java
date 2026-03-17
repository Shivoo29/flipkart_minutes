package com.flipkart.minutes.service;

import com.flipkart.minutes.exception.FlipkartMinutesException;
import com.flipkart.minutes.model.Customer;
import com.flipkart.minutes.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Handles customer onboarding and lookup.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final NotificationService notificationService;

    public CustomerService(CustomerRepository customerRepo,
                           NotificationService notificationService) {
        this.customerRepo = customerRepo;
        this.notificationService = notificationService;
    }

    /**
     * Onboard a new customer.
     *
     * @param customerId unique identifier for the customer
     * @param name       display name
     * @return the newly created Customer
     * @throws FlipkartMinutesException if a customer with that ID already exists
     */
    public Customer onboardCustomer(String customerId, String name) {
        if (customerId == null || customerId.isBlank()) {
            throw new FlipkartMinutesException("Customer ID cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new FlipkartMinutesException("Customer name cannot be blank");
        }
        if (customerRepo.existsById(customerId)) {
            throw new FlipkartMinutesException(
                    "Customer already exists with ID: " + customerId);
        }

        Customer customer = new Customer(customerId, name);
        customerRepo.save(customer);
        notificationService.log(String.format("Customer onboarded: %s", customer));
        return customer;
    }

    /**
     * Retrieve a customer by ID.
     *
     * @throws FlipkartMinutesException if not found
     */
    public Customer getCustomer(String customerId) {
        return customerRepo.findById(customerId)
                .orElseThrow(() -> new FlipkartMinutesException(
                        "Customer not found: " + customerId));
    }

    public Collection<Customer> getAllCustomers() {
        return customerRepo.findAll();
    }
}
