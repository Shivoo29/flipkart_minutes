package com.flipkart.minutes.strategy;

import com.flipkart.minutes.model.DeliveryPartner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default strategy: assign to the first available delivery partner.
 * Fastest assignment with no sorting overhead.
 */
@Component
public class AnyAvailableStrategy implements AssignmentStrategy {

    @Override
    public DeliveryPartner select(List<DeliveryPartner> availablePartners) {
        return availablePartners.get(0);
    }

    @Override
    public String getStrategyName() {
        return "ANY_AVAILABLE";
    }
}
