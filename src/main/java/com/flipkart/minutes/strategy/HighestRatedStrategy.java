package com.flipkart.minutes.strategy;

import com.flipkart.minutes.model.DeliveryPartner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Highest-Rated strategy: assigns to the available partner with the best
 * average star rating. Partners with no ratings are treated as 0.0 and will
 * receive orders only if everyone else also has no ratings.
 */
@Component
public class HighestRatedStrategy implements AssignmentStrategy {

    @Override
    public DeliveryPartner select(List<DeliveryPartner> availablePartners) {
        return availablePartners.stream()
                .max(Comparator.comparingDouble(DeliveryPartner::getAverageRating))
                .orElse(availablePartners.get(0));
    }

    @Override
    public String getStrategyName() {
        return "HIGHEST_RATED";
    }
}
