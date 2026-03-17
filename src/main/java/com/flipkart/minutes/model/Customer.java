package com.flipkart.minutes.model;

/**
 * Represents a Flipkart Minutes customer.
 */
public class Customer {

    private final String customerId;
    private final String name;

    public Customer(String customerId, String name) {
        this.customerId = customerId;
        this.name = name;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("Customer{id='%s', name='%s'}", customerId, name);
    }
}
