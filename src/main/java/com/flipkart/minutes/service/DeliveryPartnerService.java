package com.flipkart.minutes.service;

import com.flipkart.minutes.exception.FlipkartMinutesException;
import com.flipkart.minutes.model.DeliveryPartner;
import com.flipkart.minutes.repository.DeliveryPartnerRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Handles delivery partner onboarding, lookup, and status queries.
 */
@Service
public class DeliveryPartnerService {

    private final DeliveryPartnerRepository partnerRepo;
    private final NotificationService notificationService;

    public DeliveryPartnerService(DeliveryPartnerRepository partnerRepo,
                                  NotificationService notificationService) {
        this.partnerRepo = partnerRepo;
        this.notificationService = notificationService;
    }

    /**
     * Onboard a new delivery partner.
     *
     * @param partnerId unique identifier
     * @param name      display name
     * @return the newly created DeliveryPartner
     * @throws FlipkartMinutesException if the ID is already registered
     */
    public DeliveryPartner onboardPartner(String partnerId, String name) {
        if (partnerId == null || partnerId.isBlank()) {
            throw new FlipkartMinutesException("Partner ID cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new FlipkartMinutesException("Partner name cannot be blank");
        }
        if (partnerRepo.existsById(partnerId)) {
            throw new FlipkartMinutesException(
                    "Delivery partner already exists with ID: " + partnerId);
        }

        DeliveryPartner partner = new DeliveryPartner(partnerId, name);
        partnerRepo.save(partner);
        notificationService.log(String.format("Delivery partner onboarded: %s", partner));
        return partner;
    }

    /**
     * Retrieve a partner by ID.
     *
     * @throws FlipkartMinutesException if not found
     */
    public DeliveryPartner getPartner(String partnerId) {
        return partnerRepo.findById(partnerId)
                .orElseThrow(() -> new FlipkartMinutesException(
                        "Delivery partner not found: " + partnerId));
    }

    /**
     * Print the current status of a delivery partner (or all partners).
     *
     * @param partnerId specific partner ID, or null to show all
     */
    public void showPartnerStatus(String partnerId) {
        if (partnerId != null && !partnerId.isBlank()) {
            DeliveryPartner p = getPartner(partnerId);
            printPartnerStatus(p);
        } else {
            Collection<DeliveryPartner> all = partnerRepo.findAll();
            if (all.isEmpty()) {
                System.out.println("  No delivery partners registered.");
                return;
            }
            all.forEach(this::printPartnerStatus);
        }
    }

    private void printPartnerStatus(DeliveryPartner p) {
        System.out.printf(
                "  Partner %-10s | %-8s | Status: %-9s | Current Order: %-12s | Deliveries: %3d | Rating: %.1f (%d reviews)%n",
                p.getPartnerId(), p.getName(), p.getStatus(),
                p.getCurrentOrderId() != null ? p.getCurrentOrderId() : "—",
                p.getTotalDeliveries(), p.getAverageRating(), p.getRatingCount()
        );
    }

    public Collection<DeliveryPartner> getAllPartners() {
        return partnerRepo.findAll();
    }
}
