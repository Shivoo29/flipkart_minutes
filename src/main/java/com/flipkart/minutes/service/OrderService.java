package com.flipkart.minutes.service;

import com.flipkart.minutes.exception.FlipkartMinutesException;
import com.flipkart.minutes.model.*;
import com.flipkart.minutes.repository.*;
import com.flipkart.minutes.strategy.AssignmentStrategy;
import com.flipkart.minutes.strategy.AssignmentStrategyFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core service for order lifecycle management in Flipkart Minutes.
 *
 * <h3>Concurrency Design</h3>
 * <ul>
 *   <li>{@code assignmentLock} — a {@link ReentrantLock} that guards all
 *       state transitions involving both an {@link Order} and a
 *       {@link DeliveryPartner} simultaneously (assign, cancel, pick-up,
 *       complete). This prevents race conditions such as two orders being
 *       assigned to the same partner.</li>
 *   <li>{@code pendingOrderQueue} — a {@link ConcurrentLinkedQueue} of
 *       order IDs waiting for a partner. Polls are done inside the lock;
 *       the queue is {@link java.util.concurrent.ConcurrentLinkedQueue}
 *       for extra safety in edge cases where the lock is not held.</li>
 *   <li>Auto-cancel — a single-thread {@link ScheduledExecutorService}
 *       fires per order after the configured timeout. The task re-acquires
 *       the lock before making any state change.</li>
 * </ul>
 *
 * <h3>Order lifecycle</h3>
 * <pre>
 *  createOrder() → PENDING ─(partner available)→ ASSIGNED
 *                          ─(no partner)─────────►[queue]
 *
 *  ASSIGNED → pickUpOrder() → PICKED_UP → completeOrder() → DELIVERED
 *
 *  PENDING / ASSIGNED → cancelOrder() → CANCELLED
 *  PENDING / ASSIGNED → [auto-cancel timer] → CANCELLED
 * </pre>
 */
@Service
public class OrderService {

    // ─── Configuration ───────────────────────────────────────────────────────

    @Value("${flipkart.minutes.auto-cancel-minutes:30}")
    private long autoCancelMinutes;

    // ─── Dependencies ────────────────────────────────────────────────────────

    private final CustomerRepository customerRepo;
    private final DeliveryPartnerRepository partnerRepo;
    private final ItemRepository itemRepo;
    private final OrderRepository orderRepo;
    private final AssignmentStrategyFactory strategyFactory;
    private final NotificationService notificationService;

    // ─── Internal state ──────────────────────────────────────────────────────

    /** Protects concurrent assignment / cancellation / pickup / completion logic. */
    private final ReentrantLock assignmentLock = new ReentrantLock(true /* fair */);

    /** Queue of order IDs waiting for a delivery partner to become available. */
    private final ConcurrentLinkedQueue<String> pendingOrderQueue = new ConcurrentLinkedQueue<>();

    /** Monotonically increasing counter for order ID generation. */
    private final AtomicLong orderIdCounter = new AtomicLong(1);

    /** Dedicated thread for auto-cancel scheduled tasks. */
    private final ScheduledExecutorService autoCancelScheduler =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "auto-cancel-scheduler");
                t.setDaemon(true);
                return t;
            });

    /** Active assignment strategy — switchable at runtime. */
    private volatile AssignmentStrategy currentStrategy;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public OrderService(CustomerRepository customerRepo,
                        DeliveryPartnerRepository partnerRepo,
                        ItemRepository itemRepo,
                        OrderRepository orderRepo,
                        AssignmentStrategyFactory strategyFactory,
                        NotificationService notificationService) {
        this.customerRepo = customerRepo;
        this.partnerRepo = partnerRepo;
        this.itemRepo = itemRepo;
        this.orderRepo = orderRepo;
        this.strategyFactory = strategyFactory;
        this.notificationService = notificationService;
        // Default strategy
        this.currentStrategy = strategyFactory.getStrategy(StrategyType.ANY);
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Change the active assignment strategy at runtime.
     *
     * @param strategyType the desired strategy
     */
    public void setAssignmentStrategy(StrategyType strategyType) {
        this.currentStrategy = strategyFactory.getStrategy(strategyType);
        notificationService.log("Assignment strategy changed to: "
                + currentStrategy.getStrategyName());
    }

    /**
     * Create a new order for a customer.
     *
     * <p>Stock is deducted immediately. The order is either assigned to an
     * available partner right away, or queued if none are free.
     *
     * @param customerId ID of the ordering customer
     * @param itemName   name of the item to order
     * @param itemQty    quantity requested
     * @return the created {@link Order}
     * @throws FlipkartMinutesException on validation failure or insufficient stock
     */
    public Order createOrder(String customerId, String itemName, int itemQty) {
        // ── Validate inputs ──────────────────────────────────────────────────
        if (itemQty <= 0) {
            throw new FlipkartMinutesException("Item quantity must be at least 1");
        }

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new FlipkartMinutesException(
                        "Customer not found: " + customerId));

        Item item = itemRepo.findByName(itemName)
                .orElseThrow(() -> new FlipkartMinutesException(
                        "Item not found in catalogue: " + itemName));

        // ── Deduct stock (thread-safe on Item) ───────────────────────────────
        boolean deducted = item.deductStock(itemQty);
        if (!deducted) {
            throw new FlipkartMinutesException(String.format(
                    "Item '%s' is out of stock or insufficient quantity. Available: %d, Requested: %d",
                    itemName, item.getStockQty(), itemQty));
        }

        // ── Create and persist order ─────────────────────────────────────────
        String orderId = "ORD-" + orderIdCounter.getAndIncrement();
        Order order = new Order(orderId, customerId, itemName, itemQty);
        orderRepo.save(order);

        notificationService.notifyOrderCreated(customer, order);

        // ── Schedule auto-cancel ─────────────────────────────────────────────
        scheduleAutoCancel(orderId);

        // ── Try to assign immediately ────────────────────────────────────────
        tryAssignOrder(order);

        return order;
    }

    /**
     * A delivery partner picks up an assigned order, marking it in transit.
     * After this point the order <strong>cannot</strong> be cancelled.
     *
     * @param partnerId ID of the picking-up partner
     * @param orderId   ID of the order to pick up
     * @throws FlipkartMinutesException if the order is not in ASSIGNED state
     *                                   or does not belong to this partner
     */
    public void pickUpOrder(String partnerId, String orderId) {
        assignmentLock.lock();
        try {
            DeliveryPartner partner = requirePartner(partnerId);
            Order order = requireOrder(orderId);

            // Validate ownership
            if (!orderId.equals(partner.getCurrentOrderId())) {
                throw new FlipkartMinutesException(String.format(
                        "Order %s is not assigned to partner %s", orderId, partnerId));
            }
            // Validate state
            if (order.getStatus() != OrderStatus.ASSIGNED) {
                throw new FlipkartMinutesException(String.format(
                        "Order %s cannot be picked up. Current status: %s",
                        orderId, order.getStatus()));
            }

            order.setStatus(OrderStatus.PICKED_UP);
            order.setPickedUpAt(LocalDateTime.now());

            notificationService.notifyOrderPickedUp(partner, order);

        } finally {
            assignmentLock.unlock();
        }
    }

    /**
     * Mark an order as delivered, free the partner, and assign any queued order.
     *
     * @param partnerId ID of the completing partner
     * @param orderId   ID of the order completed
     * @throws FlipkartMinutesException if the order is not in PICKED_UP state
     */
    public void completeOrder(String partnerId, String orderId) {
        assignmentLock.lock();
        try {
            DeliveryPartner partner = requirePartner(partnerId);
            Order order = requireOrder(orderId);

            if (!orderId.equals(partner.getCurrentOrderId())) {
                throw new FlipkartMinutesException(String.format(
                        "Order %s is not assigned to partner %s", orderId, partnerId));
            }
            if (order.getStatus() != OrderStatus.PICKED_UP) {
                throw new FlipkartMinutesException(String.format(
                        "Order %s cannot be completed. Current status: %s",
                        orderId, order.getStatus()));
            }

            // Transition to DELIVERED
            order.setStatus(OrderStatus.DELIVERED);
            order.setCompletedAt(LocalDateTime.now());
            partner.incrementDeliveries();

            notificationService.notifyOrderDelivered(partner, order);

            // Free the partner and serve next pending order if any
            partner.setCurrentOrderId(null);
            releasePartnerAndAssignNext(partner);

        } finally {
            assignmentLock.unlock();
        }
    }

    /**
     * Cancel an order by ID (customer-initiated or system-initiated).
     * Cancellation is only allowed for PENDING or ASSIGNED orders.
     *
     * @param orderId ID of the order to cancel
     * @param reason  human-readable cancellation reason (for notifications)
     * @throws FlipkartMinutesException if the order cannot be cancelled
     */
    public void cancelOrder(String orderId, String reason) {
        assignmentLock.lock();
        try {
            Order order = requireOrder(orderId);

            // Guard against illegal transitions
            if (order.getStatus() == OrderStatus.PICKED_UP) {
                throw new FlipkartMinutesException(
                        "Order " + orderId + " cannot be cancelled — it has already been picked up.");
            }
            if (order.getStatus() == OrderStatus.DELIVERED) {
                throw new FlipkartMinutesException(
                        "Order " + orderId + " is already delivered — cannot cancel.");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                // Idempotent — silently ignore double-cancel (e.g. auto-cancel + manual cancel)
                return;
            }

            OrderStatus previousStatus = order.getStatus();

            // Restore stock
            itemRepo.findByName(order.getItemName())
                    .ifPresent(item -> item.addStock(order.getItemQty()));

            // Transition order
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());

            notificationService.notifyOrderCancelled(order, reason);

            // If a partner was assigned, free them and serve next queued order
            if (previousStatus == OrderStatus.ASSIGNED) {
                String assignedPartnerId = order.getAssignedPartnerId();
                if (assignedPartnerId != null) {
                    partnerRepo.findById(assignedPartnerId).ifPresent(partner -> {
                        partner.setCurrentOrderId(null);
                        releasePartnerAndAssignNext(partner);
                    });
                }
            }

        } finally {
            assignmentLock.unlock();
        }
    }

    /**
     * Rate a delivery partner after a successful delivery.
     *
     * @param customerId the rating customer
     * @param partnerId  the partner being rated
     * @param orderId    the completed order (must be DELIVERED)
     * @param rating     star rating 1–5
     */
    public void ratePartner(String customerId, String partnerId, String orderId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new FlipkartMinutesException("Rating must be between 1 and 5");
        }

        Order order = requireOrder(orderId);

        if (!customerId.equals(order.getCustomerId())) {
            throw new FlipkartMinutesException(
                    "Customer " + customerId + " did not place order " + orderId);
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new FlipkartMinutesException(
                    "Can only rate a DELIVERED order. Current status: " + order.getStatus());
        }
        if (!partnerId.equals(order.getAssignedPartnerId())) {
            throw new FlipkartMinutesException(
                    "Partner " + partnerId + " did not deliver order " + orderId);
        }

        DeliveryPartner partner = requirePartner(partnerId);
        partner.addRating(rating);

        notificationService.notifyPartnerRated(partner, rating, customerId);
    }

    /**
     * Display the status of a specific order (or all orders if null).
     */
    public void showOrderStatus(String orderId) {
        if (orderId != null && !orderId.isBlank()) {
            Order order = requireOrder(orderId);
            printOrderStatus(order);
        } else {
            List<Order> all = new ArrayList<>(orderRepo.findAll());
            if (all.isEmpty()) {
                System.out.println("  No orders in the system.");
            } else {
                all.forEach(this::printOrderStatus);
            }
        }
    }

    // ─── Internal assignment logic ────────────────────────────────────────────

    /**
     * Attempt to assign the order to an available partner.
     * Must be called from a context where the caller MAY or MAY NOT hold the lock.
     * This method acquires the lock internally.
     */
    private void tryAssignOrder(Order order) {
        assignmentLock.lock();
        try {
            List<DeliveryPartner> available = partnerRepo.findAvailable();
            if (available.isEmpty()) {
                // Queue it; a partner will pick it up when they complete their current order
                pendingOrderQueue.offer(order.getOrderId());
                notificationService.notifyOrderQueued(order);
            } else {
                DeliveryPartner partner = currentStrategy.select(available);
                assignOrderToPartner(order, partner);
            }
        } finally {
            assignmentLock.unlock();
        }
    }

    /**
     * Bind an order to a partner. Caller MUST hold {@code assignmentLock}.
     */
    private void assignOrderToPartner(Order order, DeliveryPartner partner) {
        partner.setStatus(PartnerStatus.BUSY);
        partner.setCurrentOrderId(order.getOrderId());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setAssignedPartnerId(partner.getPartnerId());
        notificationService.notifyOrderAssigned(partner, order);
    }

    /**
     * After a partner is freed (delivery complete or order cancelled), try to
     * assign the next PENDING order from the queue.
     * Caller MUST hold {@code assignmentLock}.
     */
    private void releasePartnerAndAssignNext(DeliveryPartner partner) {
        // Drain stale/cancelled entries from the head of the queue
        while (!pendingOrderQueue.isEmpty()) {
            String nextOrderId = pendingOrderQueue.peek();
            if (nextOrderId == null) break;

            Order nextOrder = orderRepo.findById(nextOrderId).orElse(null);
            if (nextOrder != null && nextOrder.getStatus() == OrderStatus.PENDING) {
                pendingOrderQueue.poll(); // consume
                assignOrderToPartner(nextOrder, partner);
                return; // partner is now BUSY again
            } else {
                // Stale entry (already cancelled/delivered) — discard
                pendingOrderQueue.poll();
            }
        }
        // No pending orders — partner becomes available
        partner.setStatus(PartnerStatus.AVAILABLE);
        notificationService.log("Partner " + partner.getName() + " is now AVAILABLE.");
    }

    /**
     * Schedule an automatic cancellation after the configured timeout.
     * The task is a no-op if the order is already in a terminal state.
     */
    private void scheduleAutoCancel(String orderId) {
        autoCancelScheduler.schedule(() -> {
            try {
                Order order = orderRepo.findById(orderId).orElse(null);
                if (order == null) return;

                OrderStatus status = order.getStatus();
                // Only cancel if still in a cancellable state
                if (status == OrderStatus.PENDING || status == OrderStatus.ASSIGNED) {
                    notificationService.notifyAutoCancelled(order);
                    cancelOrder(orderId, "Auto-cancelled: not picked up within "
                            + autoCancelMinutes + " minutes");
                }
            } catch (Exception e) {
                notificationService.log("Auto-cancel task error for " + orderId
                        + ": " + e.getMessage());
            }
        }, autoCancelMinutes, TimeUnit.MINUTES);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Order requireOrder(String orderId) {
        return orderRepo.findById(orderId)
                .orElseThrow(() -> new FlipkartMinutesException("Order not found: " + orderId));
    }

    private DeliveryPartner requirePartner(String partnerId) {
        return partnerRepo.findById(partnerId)
                .orElseThrow(() -> new FlipkartMinutesException(
                        "Delivery partner not found: " + partnerId));
    }

    private void printOrderStatus(Order order) {
        System.out.printf(
                "  Order %-10s | Customer: %-8s | Item: %-15s x%-3d | Status: %-10s | Partner: %s%n",
                order.getOrderId(), order.getCustomerId(),
                order.getItemName(), order.getItemQty(),
                order.getStatus(),
                order.getAssignedPartnerId() != null ? order.getAssignedPartnerId() : "—"
        );
    }

    /**
     * Convenience method: directly add a rating to a partner (used in testing
     * when a specific delivered orderId is not readily available).
     *
     * @param partnerId partner to rate
     * @param rating    1–5 stars
     */
    public void addPartnerRatingDirect(String partnerId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new FlipkartMinutesException("Rating must be between 1 and 5");
        }
        DeliveryPartner partner = requirePartner(partnerId);
        partner.addRating(rating);
        notificationService.log(String.format(
                "Direct rating applied: partner [%s] → %d/5 (avg: %.1f)",
                partner.getName(), rating, partner.getAverageRating()));
    }

    /** Gracefully shut down the auto-cancel scheduler on app close. */
    @PreDestroy
    public void shutdown() {
        autoCancelScheduler.shutdownNow();
    }
}
