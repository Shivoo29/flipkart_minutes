package com.flipkart.minutes.repository;

import com.flipkart.minutes.model.Item;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for Item (inventory) entities.
 * Keyed by both itemId and item name for flexible lookups.
 */
@Repository
public class ItemRepository {

    /** Primary store keyed by itemId. */
    private final ConcurrentHashMap<String, Item> storeById = new ConcurrentHashMap<>();

    /** Secondary index keyed by item name (lowercase) for name-based lookups. */
    private final ConcurrentHashMap<String, Item> storeByName = new ConcurrentHashMap<>();

    public void save(Item item) {
        storeById.put(item.getItemId(), item);
        storeByName.put(item.getName().toLowerCase(), item);
    }

    public Optional<Item> findById(String itemId) {
        return Optional.ofNullable(storeById.get(itemId));
    }

    /**
     * Lookup by item name (case-insensitive).
     */
    public Optional<Item> findByName(String name) {
        return Optional.ofNullable(storeByName.get(name.toLowerCase()));
    }

    public boolean existsById(String itemId) {
        return storeById.containsKey(itemId);
    }

    public Collection<Item> findAll() {
        return storeById.values();
    }
}
