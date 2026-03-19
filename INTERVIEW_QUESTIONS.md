# Flipkart Minutes — Interview Questions

---

## 1. System Design & Architecture

**Q1. Walk me through the high-level design of this system. What are the core components and how do they interact?**

> The system has five services (Customer, DeliveryPartner, Item, Order, Dashboard), four in-memory repositories using `ConcurrentHashMap`, a strategy layer for partner assignment, and a notification service. `OrderService` is the central coordinator — it talks to all other components. There is no web layer; it's a standalone Spring Boot app driven by a `CommandLineRunner`.

---

**Q2. Why was this built as a standalone Spring Boot app instead of a REST API?**

> For a coding challenge / evaluator context, a standalone `CommandLineRunner` is self-contained — no HTTP client needed, no port conflicts, and the entire demo runs and exits deterministically. The design could easily be extended by adding a `@RestController` layer on top of the existing services.

---

**Q3. The repositories use `ConcurrentHashMap` instead of a real database. What are the trade-offs?**

> **Pros:** Zero setup, fast, no I/O latency, great for a demo.
> **Cons:** All data is lost on restart, no persistence, no ACID transactions, no query language. In production you'd replace these with JPA repositories backed by PostgreSQL or similar, and rely on database-level locking instead of in-memory locks.

---

**Q4. How would you extend this system to support a REST API?**

> Add `spring-boot-starter-web` to `pom.xml`, create `@RestController` classes that delegate to the existing services, and map HTTP verbs to operations (POST `/orders`, PUT `/orders/{id}/pickup`, etc.). The service layer needs no changes — it's already decoupled from the transport layer.

---

## 2. Concurrency & Thread Safety

**Q5. What is the `assignmentLock` and why is it a `ReentrantLock` instead of `synchronized`?**

> `assignmentLock` is a fair `ReentrantLock` (initialized with `true`) that guards all state transitions involving both an `Order` and a `DeliveryPartner` simultaneously. `ReentrantLock` is preferred here because:
> - The `fair=true` flag prevents starvation under high load.
> - It allows `tryLock()` with timeouts if needed in the future.
> - It is more explicit and easier to reason about than scattered `synchronized` blocks.

---

**Q6. Why is `pendingOrderQueue` a `ConcurrentLinkedQueue` if it's already accessed under `assignmentLock`?**

> It's a belt-and-suspenders approach. The lock protects the logical state transitions (assign + update partner + update order atomically), while `ConcurrentLinkedQueue` provides safe concurrent `offer`/`poll` in case any future code path accesses the queue without holding the lock. It costs nothing and adds safety.

---

**Q7. `Order` uses `volatile` on mutable fields like `status` and `assignedPartnerId`. Is that enough for thread safety?**

> `volatile` guarantees visibility (any write is immediately visible to other threads) but not atomicity for compound operations (read-check-write). Here it is sufficient because all compound state transitions (e.g., check status then change it) are performed while holding `assignmentLock`. `volatile` alone handles the simple read case without needing to acquire the lock.

---

**Q8. `DeliveryPartner` uses `AtomicInteger` for `totalDeliveries`, `ratingSum`, and `ratingCount`. Why not just use `volatile int`?**

> `volatile int` only guarantees visibility — `i++` is not atomic (it's read-modify-write). `AtomicInteger.incrementAndGet()` and `addAndGet()` are CAS-based and truly atomic, so no lock is needed for these counters. This is a classic lock-free pattern for independent counters.

---

**Q9. What is the `autoCancelScheduler` and what problem does it solve?**

> It's a single-daemon-thread `ScheduledExecutorService` that fires a cancellation task for each order after a configurable timeout (`flipkart.minutes.auto-cancel-minutes`, default 30). This prevents orders from being stuck in PENDING or ASSIGNED state indefinitely if a partner never acts. The task re-acquires `assignmentLock` before changing state, making it safe to run concurrently with regular operations.

---

**Q10. In Scenario 9, 10 threads place 20 orders simultaneously. How does the system ensure no two orders are assigned to the same partner?**

> `tryAssignOrder()` acquires `assignmentLock` before reading `partnerRepo.findAvailable()` and calling `assignOrderToPartner()`. Because the lock is exclusive, only one thread can execute that critical section at a time, so the same available partner cannot be picked by two threads simultaneously.

---

## 3. Design Patterns

**Q11. Which design pattern is used for assignment strategies? Explain how it works here.**

> The **Strategy Pattern**. `AssignmentStrategy` is an interface with a single method `select(List<DeliveryPartner>)`. Four concrete implementations exist: `AnyAvailableStrategy`, `RoundRobinStrategy`, `LeastBusyStrategy`, and `HighestRatedStrategy`. `OrderService` holds a reference to the current strategy as `volatile AssignmentStrategy currentStrategy` and can switch it at runtime via `setAssignmentStrategy()`.

---

**Q12. What is `AssignmentStrategyFactory` and what pattern does it implement?**

> It's a **Factory** (specifically a simple factory / service locator). It maps `StrategyType` enum values to pre-constructed Spring beans. All strategy instances are injected into the factory via constructor injection — Spring manages their lifecycle. Adding a new strategy requires only: implementing the interface, creating a bean, and adding one case to the switch — no other code changes needed (Open/Closed Principle).

---

**Q13. Why is `currentStrategy` declared `volatile` in `OrderService`?**

> Because `setAssignmentStrategy()` can be called from any thread and the updated reference must be immediately visible to threads running `tryAssignOrder()`. Without `volatile`, a thread might see a stale cached reference to the old strategy.

---

**Q14. How does the queue drain work when a partner becomes free?**

> `releasePartnerAndAssignNext()` is called (under the lock) whenever a partner finishes a delivery or an order is cancelled. It peeks at the head of `pendingOrderQueue`, skips stale entries (already cancelled orders), and if a PENDING order is found, immediately assigns it to the now-free partner. The partner never transitions to AVAILABLE if there is work waiting.

---

**Q15. How is idempotency handled for double-cancellation?**

> In `cancelOrder()`, if `order.getStatus() == CANCELLED`, the method returns silently without throwing. This is an intentional idempotent design: calling cancel twice on the same order is a no-op, avoiding crashes when auto-cancel and manual cancel race.

---

## 4. Order Lifecycle

**Q16. Draw the order state machine.**

```
createOrder()
     │
     ▼
  PENDING ──(partner available)──► ASSIGNED ──► pickUpOrder() ──► PICKED_UP ──► completeOrder() ──► DELIVERED
     │                                │
     │                                │
     └──(cancelOrder / auto-cancel)───┘
                   │
                   ▼
               CANCELLED
               (not possible after PICKED_UP)
```

---

**Q17. What happens to stock when an order is cancelled?**

> Stock is restored: `item.addStock(order.getItemQty())`. This happens inside `cancelOrder()` under the assignment lock, so it's atomic with the status transition. Stock deduction happens eagerly on `createOrder()` — this prevents overselling even when orders are queued.

---

**Q18. Why is stock deducted eagerly at order creation rather than at assignment?**

> To prevent overselling. If stock were deducted only at assignment, two customers could both create orders for the last unit — both would succeed at creation, but only one could actually be fulfilled. Eager deduction reserves the stock immediately.

---

**Q19. Can an order be cancelled after it is picked up? Why?**

> No. Once an order is in `PICKED_UP` state, `cancelOrder()` throws a `FlipkartMinutesException`. The rationale: the partner is physically in transit — cancelling at that point creates an unresolvable operational inconsistency (stock has been physically picked up, partner is en route).

---

## 5. Spring Boot Specifics

**Q20. What does `@Value("${flipkart.minutes.auto-cancel-minutes:30}")` do?**

> It injects the value of the property `flipkart.minutes.auto-cancel-minutes` from `application.properties`. The `:30` is the default value used if the property is not defined. This makes the timeout configurable without recompiling.

---

**Q21. Why is `@PreDestroy` used on `OrderService.shutdown()`?**

> Spring calls `@PreDestroy` methods when the application context closes. `autoCancelScheduler.shutdownNow()` ensures the background scheduler thread is stopped cleanly, preventing the JVM from hanging on exit due to non-daemon threads (though the thread is marked as daemon here — it's still good practice).

---

**Q22. What is `CommandLineRunner` and how is `DemoRunner` using it?**

> `CommandLineRunner` is a Spring Boot interface with a single method `run(String... args)` that Spring calls after the application context is fully initialized. `DemoRunner` implements it and is annotated with `@Component`, so Spring auto-detects it. This is the entry point for the entire demo — it runs all 9 scenarios sequentially.

---

## 6. Edge Cases & Failure Handling

**Q23. What happens if `findAvailable()` returns an empty list right after a partner becomes free?**

> This can't happen in the current design because `releasePartnerAndAssignNext()` is called while holding `assignmentLock`. No other thread can add a new order to the queue or change partner status between the "partner freed" event and the queue drain. The lock ensures atomicity of the entire release-and-reassign sequence.

---

**Q24. What is the risk of the `ratingSum / ratingCount` computation in `getAverageRating()`?**

> There is a subtle non-atomicity: `ratingCount.get()` and `ratingSum.get()` are two separate reads. Between them, another thread could increment both. This means the computed average could be slightly off for one read. In practice this is acceptable for a rating display, but if strict consistency were required you'd use a single lock or a `synchronized` block around both reads.

---

**Q25. What would happen if you called `pickUpOrder()` with the wrong `partnerId`?**

> The method checks `!orderId.equals(partner.getCurrentOrderId())` under the lock and throws `FlipkartMinutesException("Order X is not assigned to partner Y")`. This is tested explicitly in Scenario 8.

---

## 7. Potential Improvements

**Q26. How would you make the in-memory repositories production-ready?**

> Replace `ConcurrentHashMap` with JPA repositories (`@Repository` extending `JpaRepository`), add a database (PostgreSQL/MySQL), use `@Transactional` on service methods, and replace `ReentrantLock` with database-level row locking or optimistic concurrency with `@Version`.

---

**Q27. How would you scale this system horizontally (multiple instances)?**

> The current in-memory state can't be shared across instances. You'd need:
> - A shared database for orders, partners, items.
> - A distributed queue (Redis, Kafka, SQS) instead of `ConcurrentLinkedQueue`.
> - Distributed locking (Redis Redlock, database advisory locks) instead of `ReentrantLock`.
> - A message broker for notifications instead of direct `System.out` logging.

---

**Q28. The `NotificationService` prints to stdout. How would you make it production-grade?**

> Replace it with an event-driven system: publish domain events (`OrderCreatedEvent`, `OrderAssignedEvent`, etc.) via Spring's `ApplicationEventPublisher`. Consumers could send push notifications, emails, SMS, or write to a Kafka topic. This decouples notification logic from business logic.

---

**Q29. How would you add a new assignment strategy, say `NearestPartnerStrategy`?**

> 1. Add `NEAREST` to the `StrategyType` enum.
> 2. Implement `AssignmentStrategy` in a new `NearestPartnerStrategy` class, annotate with `@Component`.
> 3. Inject it into `AssignmentStrategyFactory` and add `case NEAREST -> nearestPartnerStrategy` to the switch.
> No other existing code needs to change — this is the Open/Closed Principle in action.

---

**Q30. What would you add to make this system observable in production?**

> - **Metrics:** Micrometer + Prometheus for order counts, queue depth, partner utilization.
> - **Tracing:** OpenTelemetry / Zipkin for distributed request tracing.
> - **Structured logging:** Replace `System.out.println` with SLF4J + Logback/JSON format.
> - **Health checks:** Spring Actuator `/health` endpoint.
> - **Alerting:** Alert on queue depth > threshold, partner availability < threshold.
