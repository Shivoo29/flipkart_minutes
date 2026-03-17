package com.flipkart.minutes.strategy;

import com.flipkart.minutes.model.DeliveryPartner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Least-Busy strategy: assigns to the available partner who has completed
 * the fewest total deliveries, thereby balancing overall workload.
 *
 * <p>If multiple partners have equal delivery counts the first (by insertion
 * order) is chosen; tie-breaking is intentionally deterministic so tests
 * remain predictable.
 */
@Component
public class LeastBusyStrategy implements AssignmentStrategy {

    @Override
    public DeliveryPartner select(List<DeliveryPartner> availablePartners) {
        return availablePartners.stream()
                .min(Comparator.comparingInt(DeliveryPartner::getTotalDeliveries))
                .orElse(availablePartners.get(0));
    }

    @Override
    public String getStrategyName() {
        return "LEAST_BUSY";
    }
}
