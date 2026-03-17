package com.flipkart.minutes.strategy;

import com.flipkart.minutes.model.DeliveryPartner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-Robin strategy: rotates assignments across all available partners
 * in a circular fashion. Uses an {@link AtomicInteger} counter so the
 * strategy is safe to use from multiple threads.
 */
@Component
public class RoundRobinStrategy implements AssignmentStrategy {

    /**
     * Global call counter. Incremented atomically on each assignment.
     * Modulo the list size gives the selected index.
     */
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public DeliveryPartner select(List<DeliveryPartner> availablePartners) {
        int size = availablePartners.size();
        // getAndIncrement is atomic; modulo distributes evenly across partners
        int index = counter.getAndIncrement() % size;
        return availablePartners.get(index);
    }

    @Override
    public String getStrategyName() {
        return "ROUND_ROBIN";
    }
}
