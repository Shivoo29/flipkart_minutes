package com.flipkart.minutes.service;

import com.flipkart.minutes.model.DeliveryPartner;
import com.flipkart.minutes.model.Order;
import com.flipkart.minutes.model.OrderStatus;
import com.flipkart.minutes.repository.DeliveryPartnerRepository;
import com.flipkart.minutes.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides real-time analytics and leaderboard views.
 *
 * <ul>
 *   <li>Top delivery partners by total deliveries</li>
 *   <li>Top delivery partners by average rating</li>
 *   <li>Order summary statistics</li>
 * </ul>
 */
@Service
public class DashboardService {

    private final DeliveryPartnerRepository partnerRepo;
    private final OrderRepository orderRepo;

    public DashboardService(DeliveryPartnerRepository partnerRepo,
                            OrderRepository orderRepo) {
        this.partnerRepo = partnerRepo;
        this.orderRepo = orderRepo;
    }

    /**
     * Print the full system dashboard to stdout.
     *
     * @param topN number of partners to show in each leaderboard (default 5)
     */
    public void showDashboard(int topN) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║          FLIPKART MINUTES — SYSTEM DASHBOARD             ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        printOrderSummary();
        printTopByDeliveries(topN);
        printTopByRating(topN);

        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println();
    }

    // ─── Dashboard sections ──────────────────────────────────────────────────

    private void printOrderSummary() {
        List<Order> allOrders = new ArrayList<>(orderRepo.findAll());
        long total     = allOrders.size();
        long pending   = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long assigned  = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.ASSIGNED).count();
        long pickedUp  = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.PICKED_UP).count();
        long delivered = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long cancelled = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        System.out.println("─── Order Summary ─────────────────────────────────────────");
        System.out.printf("  Total: %d  |  Pending: %d  |  Assigned: %d  |  In-Transit: %d%n",
                total, pending, assigned, pickedUp);
        System.out.printf("  Delivered: %d  |  Cancelled: %d%n", delivered, cancelled);
    }

    private void printTopByDeliveries(int topN) {
        System.out.println("─── Top Partners by Deliveries ────────────────────────────");
        List<DeliveryPartner> ranked = partnerRepo.findAll().stream()
                .sorted(Comparator.comparingInt(DeliveryPartner::getTotalDeliveries).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            System.out.println("  No delivery partners registered.");
            return;
        }

        int rank = 1;
        for (DeliveryPartner p : ranked) {
            System.out.printf("  #%-2d %-10s | %-8s | %3d deliveries | Rating: %.1f (%d reviews)%n",
                    rank++, p.getPartnerId(), p.getName(),
                    p.getTotalDeliveries(), p.getAverageRating(), p.getRatingCount());
        }
    }

    private void printTopByRating(int topN) {
        System.out.println("─── Top Partners by Rating ────────────────────────────────");
        List<DeliveryPartner> ranked = partnerRepo.findAll().stream()
                .filter(p -> p.getRatingCount() > 0) // only rated partners
                .sorted(Comparator.comparingDouble(DeliveryPartner::getAverageRating).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            System.out.println("  No rated delivery partners yet.");
            return;
        }

        int rank = 1;
        for (DeliveryPartner p : ranked) {
            System.out.printf("  #%-2d %-10s | %-8s | Rating: %.1f (%d reviews) | %d deliveries%n",
                    rank++, p.getPartnerId(), p.getName(),
                    p.getAverageRating(), p.getRatingCount(), p.getTotalDeliveries());
        }
    }
}
