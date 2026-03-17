package com.flipkart.minutes.repository;

import com.flipkart.minutes.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for Customer entities.
 */
@Repository
public class CustomerRepository {

    private final ConcurrentHashMap<String, Customer> store = new ConcurrentHashMap<>();

    public void save(Customer customer) {
        store.put(customer.getCustomerId(), customer);
    }

    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(store.get(customerId));
    }

    public boolean existsById(String customerId) {
        return store.containsKey(customerId);
    }

    public Collection<Customer> findAll() {
        return store.values();
    }

    public int count() {
        return store.size();
    }
}
