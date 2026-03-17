package com.flipkart.minutes.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a delivery partner in the Flipkart Minutes system.
 *
 * Thread-safety notes:
 *  - {@code status} and {@code currentOrderId} are mutated under the global
 *    assignment lock inside OrderService, so volatile is sufficient.
 *  - {@code totalDeliveries}, {@code ratingSum}, {@code ratingCount} use
 *    AtomicInteger for lock-free, thread-safe updates.
 */
public class DeliveryPartner {

    private final String partnerId;
    private final String name;

    /** Current availability of this partner. Protected by OrderService assignment lock. */
    private volatile PartnerStatus status;

    /** The orderId currently assigned to this partner (null if AVAILABLE). */
    private volatile String currentOrderId;

    /** Cumulative count of successfully delivered orders. */
    private final AtomicInteger totalDeliveries;

    /** Sum of all ratings received (for computing average). */
    private final AtomicInteger ratingSum;

    /** Number of ratings received. */
    private final AtomicInteger ratingCount;

    public DeliveryPartner(String partnerId, String name) {
        this.partnerId = partnerId;
        this.name = name;
        this.status = PartnerStatus.AVAILABLE;
        this.currentOrderId = null;
        this.totalDeliveries = new AtomicInteger(0);
        this.ratingSum = new AtomicInteger(0);
        this.ratingCount = new AtomicInteger(0);
    }

    // ─── Mutators ────────────────────────────────────────────────────────────

    public void setStatus(PartnerStatus status) {
        this.status = status;
    }

    public void setCurrentOrderId(String orderId) {
        this.currentOrderId = orderId;
    }

    /** Increment delivery count atomically. */
    public void incrementDeliveries() {
        totalDeliveries.incrementAndGet();
    }

    /**
     * Record a customer rating (1–5 stars).
     *
     * @param rating integer rating between 1 and 5
     */
    public void addRating(int rating) {
        ratingSum.addAndGet(rating);
        ratingCount.incrementAndGet();
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String getPartnerId() {
        return partnerId;
    }

    public String getName() {
        return name;
    }

    public PartnerStatus getStatus() {
        return status;
    }

    public String getCurrentOrderId() {
        return currentOrderId;
    }

    public int getTotalDeliveries() {
        return totalDeliveries.get();
    }

    public int getRatingCount() {
        return ratingCount.get();
    }

    /** Returns the average star rating, or 0.0 if no ratings yet. */
    public double getAverageRating() {
        int count = ratingCount.get();
        return count == 0 ? 0.0 : (double) ratingSum.get() / count;
    }

    @Override
    public String toString() {
        return String.format(
                "DeliveryPartner{id='%s', name='%s', status=%s, deliveries=%d, rating=%.1f (%d reviews)}",
                partnerId, name, status, totalDeliveries.get(), getAverageRating(), ratingCount.get()
        );
    }
}
