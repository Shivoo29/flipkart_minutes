package com.flipkart.minutes.model;

/**
 * Supported order-assignment strategies.
 *
 * ANY           → Assign to any available partner (default).
 * ROUND_ROBIN   → Assign in rotating order across all partners.
 * LEAST_BUSY    → Assign to the partner with fewest total deliveries
 *                 (balances workload over time).
 * HIGHEST_RATED → Assign to the highest-rated available partner.
 */
public enum StrategyType {
    ANY,
    ROUND_ROBIN,
    LEAST_BUSY,
    HIGHEST_RATED
}
