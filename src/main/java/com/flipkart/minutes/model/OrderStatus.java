package com.flipkart.minutes.model;

/**
 * Lifecycle states of an Order.
 *
 * PENDING    → Created, waiting for a delivery partner to be assigned.
 * ASSIGNED   → A delivery partner has been assigned but hasn't picked up yet.
 *              Customer can still cancel in this state.
 * PICKED_UP  → Partner has physically picked up the order.
 *              Cancellation is NOT allowed after this point.
 * DELIVERED  → Order successfully delivered to the customer.
 * CANCELLED  → Order was cancelled (by customer, system, or auto-cancel timer).
 */
public enum OrderStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}
