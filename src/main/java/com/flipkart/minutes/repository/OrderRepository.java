package com.flipkart.minutes.repository;

import com.flipkart.minutes.model.Order;
import com.flipkart.minutes.model.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for Order entities.
 */
@Repository
public class OrderRepository {

    private final ConcurrentHashMap<String, Order> store = new ConcurrentHashMap<>();

    public void save(Order order) {
        store.put(order.getOrderId(), order);
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    public boolean existsById(String orderId) {
        return store.containsKey(orderId);
    }

    /**
     * Returns all orders with the given status.
     */
    public List<Order> findByStatus(OrderStatus status) {
        List<Order> result = new ArrayList<>();
        for (Order o : store.values()) {
            if (o.getStatus() == status) {
                result.add(o);
            }
        }
        return result;
    }

    /**
     * Returns all orders placed by a specific customer.
     */
    public List<Order> findByCustomerId(String customerId) {
        List<Order> result = new ArrayList<>();
        for (Order o : store.values()) {
            if (customerId.equals(o.getCustomerId())) {
                result.add(o);
            }
        }
        return result;
    }

    public Collection<Order> findAll() {
        return store.values();
    }

    public int count() {
        return store.size();
    }
}
