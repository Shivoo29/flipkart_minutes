package com.flipkart.minutes.strategy;

import com.flipkart.minutes.model.StrategyType;
import org.springframework.stereotype.Component;

/**
 * Factory that maps a {@link StrategyType} enum value to the corresponding
 * {@link AssignmentStrategy} implementation.
 *
 * <p>All strategy beans are injected via constructor so Spring manages their
 * lifecycle. Adding a new strategy only requires implementing the interface,
 * creating a Spring bean, and registering it here — no existing code changes.
 */
@Component
public class AssignmentStrategyFactory {

    private final AnyAvailableStrategy anyAvailableStrategy;
    private final RoundRobinStrategy roundRobinStrategy;
    private final LeastBusyStrategy leastBusyStrategy;
    private final HighestRatedStrategy highestRatedStrategy;

    public AssignmentStrategyFactory(
            AnyAvailableStrategy anyAvailableStrategy,
            RoundRobinStrategy roundRobinStrategy,
            LeastBusyStrategy leastBusyStrategy,
            HighestRatedStrategy highestRatedStrategy) {
        this.anyAvailableStrategy = anyAvailableStrategy;
        this.roundRobinStrategy = roundRobinStrategy;
        this.leastBusyStrategy = leastBusyStrategy;
        this.highestRatedStrategy = highestRatedStrategy;
    }

    /**
     * Returns the {@link AssignmentStrategy} for the requested type.
     *
     * @param type strategy type from caller
     * @return the matching strategy implementation
     */
    public AssignmentStrategy getStrategy(StrategyType type) {
        return switch (type) {
            case ROUND_ROBIN   -> roundRobinStrategy;
            case LEAST_BUSY    -> leastBusyStrategy;
            case HIGHEST_RATED -> highestRatedStrategy;
            default            -> anyAvailableStrategy;   // ANY + fallback
        };
    }
}
