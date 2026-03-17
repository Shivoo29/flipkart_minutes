package com.flipkart.minutes.service;

import com.flipkart.minutes.exception.FlipkartMinutesException;
import com.flipkart.minutes.model.Item;
import com.flipkart.minutes.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Manages the product inventory — adding items and updating stock levels.
 */
@Service
public class ItemService {

    private final ItemRepository itemRepo;
    private final NotificationService notificationService;

    public ItemService(ItemRepository itemRepo,
                       NotificationService notificationService) {
        this.itemRepo = itemRepo;
        this.notificationService = notificationService;
    }

    /**
     * Add a new item to the inventory catalogue.
     *
     * @param itemId   unique identifier
     * @param name     display name (also used as a lookup key)
     * @param stockQty initial stock quantity (≥ 0)
     * @return the created Item
     * @throws FlipkartMinutesException if the ID is already registered
     */
    public Item addItem(String itemId, String name, int stockQty) {
        if (itemId == null || itemId.isBlank()) {
            throw new FlipkartMinutesException("Item ID cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new FlipkartMinutesException("Item name cannot be blank");
        }
        if (stockQty < 0) {
            throw new FlipkartMinutesException("Initial stock cannot be negative");
        }
        if (itemRepo.existsById(itemId)) {
            throw new FlipkartMinutesException("Item already exists with ID: " + itemId);
        }

        Item item = new Item(itemId, name, stockQty);
        itemRepo.save(item);
        notificationService.log(String.format("Item added to inventory: %s", item));
        return item;
    }

    /**
     * Update the stock quantity for an existing item.
     *
     * @param itemId   the item to update
     * @param stockQty new absolute stock quantity (≥ 0)
     * @throws FlipkartMinutesException if the item does not exist
     */
    public void updateStock(String itemId, int stockQty) {
        if (stockQty < 0) {
            throw new FlipkartMinutesException("Stock quantity cannot be negative");
        }
        Item item = itemRepo.findById(itemId)
                .orElseThrow(() -> new FlipkartMinutesException("Item not found: " + itemId));

        item.updateStock(stockQty);
        notificationService.log(String.format(
                "Stock updated: item '%s' → %d units", item.getName(), stockQty));
    }

    /**
     * Retrieve an item by name (case-insensitive).
     *
     * @throws FlipkartMinutesException if not found
     */
    public Item getItemByName(String name) {
        return itemRepo.findByName(name)
                .orElseThrow(() -> new FlipkartMinutesException("Item not found: " + name));
    }

    /**
     * Retrieve an item by ID.
     *
     * @throws FlipkartMinutesException if not found
     */
    public Item getItemById(String itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new FlipkartMinutesException("Item not found: " + itemId));
    }

    public Collection<Item> getAllItems() {
        return itemRepo.findAll();
    }
}
