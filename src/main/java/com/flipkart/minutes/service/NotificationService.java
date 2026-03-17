package com.flipkart.minutes.service;

import com.flipkart.minutes.model.Customer;
import com.flipkart.minutes.model.DeliveryPartner;
import com.flipkart.minutes.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simulated notification service.
 *
 * In a production system this would send push notifications / SMS / emails.
 * Here it logs structured messages at INFO level using SLF4J so all
 * notifications are visible in the console output.
 *
 * Notifications are sent for:
 *  - Order created (customer acknowledgement)
 *  - Order assigned to partner
 *  - Order picked up by partner
 *  - Order delivered
 *  - Order cancelled
 *  - Order auto-cancelled (timeout)
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ─── Public notification methods ─────────────────────────────────────────

    public void notifyOrderCreated(Customer customer, Order order) {
        notify("CUSTOMER [%s]: Your order %s for '%s' (qty: %d) has been placed. Status: %s",
                customer.getName(), order.getOrderId(),
                order.getItemName(), order.getItemQty(), order.getStatus());
    }

    public void notifyOrderAssigned(DeliveryPartner partner, Order order) {
        notify("SYSTEM: Order %s assigned to partner [%s]",
                order.getOrderId(), partner.getName());
        notify("PARTNER [%s]: New order %s assigned to you — '%s' (qty: %d). Please pick up.",
                partner.getName(), order.getOrderId(),
                order.getItemName(), order.getItemQty());
    }

    public void notifyOrderQueued(Order order) {
        notify("SYSTEM: Order %s queued — no delivery partner currently available.",
                order.getOrderId());
    }

    public void notifyOrderPickedUp(DeliveryPartner partner, Order order) {
        notify("PARTNER [%s]: Picked up order %s — '%s'. En route to customer.",
                partner.getName(), order.getOrderId(), order.getItemName());
        notify("CUSTOMER [order %s]: Your order has been picked up and is on the way!",
                order.getOrderId());
    }

    public void notifyOrderDelivered(DeliveryPartner partner, Order order) {
        notify("PARTNER [%s]: Order %s delivered successfully. Great job!",
                partner.getName(), order.getOrderId());
        notify("CUSTOMER [order %s]: Your order '%s' has been delivered. Enjoy! You can rate the partner.",
                order.getOrderId(), order.getItemName());
    }

    public void notifyOrderCancelled(Order order, String reason) {
        notify("SYSTEM: Order %s CANCELLED. Reason: %s", order.getOrderId(), reason);
        notify("CUSTOMER [order %s]: Your order for '%s' has been cancelled.",
                order.getOrderId(), order.getItemName());
    }

    public void notifyAutoCancelled(Order order) {
        notify("AUTO-CANCEL: Order %s was not picked up within the timeout window — auto-cancelled.",
                order.getOrderId());
        notify("CUSTOMER [order %s]: Your order '%s' was auto-cancelled (no partner picked it up in time).",
                order.getOrderId(), order.getItemName());
    }

    public void notifyPartnerRated(DeliveryPartner partner, int rating, String customerId) {
        notify("RATING: Customer [%s] rated partner [%s] → %d/5 stars (avg: %.1f)",
                customerId, partner.getName(), rating, partner.getAverageRating());
    }

    /**
     * Generic log — used for system-level messages that don't fit a specific notification.
     */
    public void log(String message) {
        notify("INFO: %s", message);
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private void notify(String format, Object... args) {
        String message = String.format(format, args);
        log.info("[{}] {}", LocalDateTime.now().format(FMT), message);
    }
}
