package com.flipkart.minutes.model;

/**
 * Represents an inventory item available for order in Flipkart Minutes.
 *
 * Thread-safety: {@code deductStock} and {@code addStock} are synchronized
 * to ensure atomicity of the check-then-act stock operations.
 */
public class Item {

    private final String itemId;
    private final String name;
    private int stockQty;

    public Item(String itemId, String name, int stockQty) {
        if (stockQty < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        this.itemId = itemId;
        this.name = name;
        this.stockQty = stockQty;
    }

    // ─── Thread-safe stock operations ────────────────────────────────────────

    /**
     * Atomically checks and deducts the requested quantity.
     *
     * @param qty quantity to deduct
     * @return true if stock was successfully deducted; false if insufficient stock
     */
    public synchronized boolean deductStock(int qty) {
        if (stockQty < qty) {
            return false;
        }
        stockQty -= qty;
        return true;
    }

    /**
     * Restores stock (e.g., on order cancellation).
     *
     * @param qty quantity to add back
     */
    public synchronized void addStock(int qty) {
        stockQty += qty;
    }

    /**
     * Directly set stock quantity (e.g., inventory update command).
     *
     * @param qty new stock quantity
     */
    public synchronized void updateStock(int qty) {
        if (qty < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        this.stockQty = qty;
    }

    public synchronized int getStockQty() {
        return stockQty;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("Item{id='%s', name='%s', stock=%d}", itemId, name, getStockQty());
    }
}
