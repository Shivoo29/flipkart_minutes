package com.flipkart.minutes.repository;

import com.flipkart.minutes.model.DeliveryPartner;
import com.flipkart.minutes.model.PartnerStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory store for DeliveryPartner entities.
 */
@Repository
public class DeliveryPartnerRepository {

    private final ConcurrentHashMap<String, DeliveryPartner> store = new ConcurrentHashMap<>();

    public void save(DeliveryPartner partner) {
        store.put(partner.getPartnerId(), partner);
    }

    public Optional<DeliveryPartner> findById(String partnerId) {
        return Optional.ofNullable(store.get(partnerId));
    }

    public boolean existsById(String partnerId) {
        return store.containsKey(partnerId);
    }

    /**
     * Returns a snapshot list of all currently AVAILABLE partners.
     * Callers that need to act on this list should hold the assignment lock.
     */
    public List<DeliveryPartner> findAvailable() {
        List<DeliveryPartner> available = new ArrayList<>();
        for (DeliveryPartner p : store.values()) {
            if (p.getStatus() == PartnerStatus.AVAILABLE) {
                available.add(p);
            }
        }
        return available;
    }

    public Collection<DeliveryPartner> findAll() {
        return store.values();
    }

    public int count() {
        return store.size();
    }
}
