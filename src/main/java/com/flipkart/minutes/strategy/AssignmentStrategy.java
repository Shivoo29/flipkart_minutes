package com.flipkart.minutes.strategy;

import com.flipkart.minutes.model.DeliveryPartner;

import java.util.List;

/**
 * Strategy interface for selecting a delivery partner from a list of
 * available candidates.
 *
 * <p>Implementations define <em>how</em> a partner is selected (e.g.
 * round-robin, highest-rated). The {@code OrderService} calls
 * {@link #select(List)} while holding the assignment lock, so
 * implementations do <strong>not</strong> need to be individually
 * thread-safe beyond what the JVM guarantees for their own state.
 * However, implementations that maintain shared counters (e.g.
 * {@link RoundRobinStrategy}) must use atomic operations.
 */
public interface AssignmentStrategy {

    /**
     * Select one delivery partner from the provided list of available partners.
     *
     * @param availablePartners non-empty list of AVAILABLE delivery partners
     * @return the chosen partner
     */
    DeliveryPartner select(List<DeliveryPartner> availablePartners);

    /**
     * Human-readable name of this strategy (used in logs / dashboard).
     */
    String getStrategyName();
}
