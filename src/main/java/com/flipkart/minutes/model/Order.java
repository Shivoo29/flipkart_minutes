package com.flipkart.minutes.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a customer order in the Flipkart Minutes system.
 *
 * Lifecycle:  PENDING → ASSIGNED → PICKED_UP → DELIVERED
 *                    ↘               ↓
 *                      CANCELLED (not possible after PICKED_UP)
 *
 * Thread-safety: mutable fields use {@code volatile}. State transitions
 * that require compare-and-set semantics are guarded by the assignment
 * lock in {@code OrderService}.
 */
public class Order {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String orderId;
    private final String customerId;
    private final String itemName;
    private final int itemQty;

    private volatile OrderStatus status;
    private volatile String assignedPartnerId;

    private final LocalDateTime createdAt;
    private volatile LocalDateTime assignedAt;
    private volatile LocalDateTime pickedUpAt;
    private volatile LocalDateTime completedAt;
    private volatile LocalDateTime cancelledAt;

    public Order(String orderId, String customerId, String itemName, int itemQty) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.itemName = itemName;
        this.itemQty = itemQty;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String getOrderId()          { return orderId; }
    public String getCustomerId()       { return customerId; }
    public String getItemName()         { return itemName; }
    public int getItemQty()             { return itemQty; }
    public OrderStatus getStatus()      { return status; }
    public String getAssignedPartnerId(){ return assignedPartnerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPickedUpAt(){ return pickedUpAt; }
    public LocalDateTime getCompletedAt(){ return completedAt; }

    // ─── Mutators ────────────────────────────────────────────────────────────

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setAssignedPartnerId(String partnerId) {
        this.assignedPartnerId = partnerId;
        this.assignedAt = LocalDateTime.now();
    }

    public void setPickedUpAt(LocalDateTime time) {
        this.pickedUpAt = time;
    }

    public void setCompletedAt(LocalDateTime time) {
        this.completedAt = time;
    }

    public void setCancelledAt(LocalDateTime time) {
        this.cancelledAt = time;
    }

    // ─── Display ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Order{id='%s', customer='%s', item='%s' x%d, status=%s",
                orderId, customerId, itemName, itemQty, status));
        if (assignedPartnerId != null) {
            sb.append(String.format(", partner='%s'", assignedPartnerId));
        }
        sb.append(String.format(", created='%s'", createdAt.format(FMT)));
        if (pickedUpAt != null) {
            sb.append(String.format(", pickedUp='%s'", pickedUpAt.format(FMT)));
        }
        if (completedAt != null) {
            sb.append(String.format(", completed='%s'", completedAt.format(FMT)));
        }
        if (cancelledAt != null) {
            sb.append(String.format(", cancelled='%s'", cancelledAt.format(FMT)));
        }
        sb.append("}");
        return sb.toString();
    }
}
