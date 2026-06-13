# Architecture Debt & Future Scale-Up Map

This document catalogs deliberate simplifications made to ship quickly. Each section describes what was compromised, why it was the right call now, the expected breakpoint, and the recommended replacement.

---

## 1. Outbox Polling → Message Queue

| | Current | Future |
|---|---|---|
| **Pattern** | `NotificationProcessor` polls `notification_request` table every 30s | Dedicated message broker |
| **Transport** | PostgreSQL row-level polling (`FOR UPDATE SKIP LOCKED`) | RabbitMQ / Kafka |
| **Latency** | 0–30 seconds (worst-case) | Near real-time |

**Current implementation:**

```
SessionPublishedEvent
    → NotificationGenerationService
    → INSERT notification_request (PENDING)
    → ...30s...
    → NotificationProcessor polls
    → sender.send(...)
    → UPDATE status = PROCESSED
```

**Breakpoint:** When session publish rate exceeds ~10/s or latency SLA drops below 10s.

**Recommended migration path (RabbitMQ):**

```
donation-session
    SessionPublishedEvent
        → RabbitTemplate.convertAndSend("session.published", event)

donation-notification
    @RabbitListener(queues = "session.published")
    handle(event)
        → matching
        → RabbitTemplate.convertAndSend("notification.delivery", request)

donation-notification-delivery
    @RabbitListener(queues = "notification.delivery")
    handle(request)
        → sender.send(...)
```

**What stays the same:** Matching algorithm, preference filtering, subscription model.

**What disappears:** `NotificationProcessor`, `@Scheduled`, `NotificationSchedulingConfig`, `findPendingForProcessing`, the entire `PROCESSING` status, retry count in DB (moved to MQ dead-letter).

**Where to introduce it:** Two new modules.

```
donation-notification-messaging     # RabbitMQ config, bindings, RabbitTemplate wrappers
donation-notification-delivery      # Queue consumers, retry, sender orchestration
```

`donation-notification` stays pure domain: subscriptions, preferences, matching.

---

## 2. Radius Matching: Java Haversine → PostGIS ST_DWithin

| | Current | Future |
|---|---|---|
| **Algorithm** | Load all RADIUS subscriptions, filter in Java with Haversine | Single SQL query with spatial index |
| **Complexity** | O(N) per session — every radius subscription evaluated | O(log N) with GiST index |
| **Transport** | All rows transferred over JDBC | Database filters rows before returning them |

**Current implementation:**

```java
// DefaultNotificationMatchingService.matchByRadius()
List<NotificationSubscription> all = repository.findByTypeAndEnabled(RADIUS, true);
for (sub : all) {
    double d = haversine(sub.lat, sub.lon, session.lat, session.lon);
    if (d <= sub.radiusKm) { candidateUserIds.add(sub.userId); }
}
```

**Breakpoint:** ~10,000 radius subscriptions or ~100 sessions published per minute.

**Recommended replacement:**

```java
// NotificationSubscriptionRepository (requires hibernate-spatial)
@Query(value = """
    SELECT ns.* FROM notification_subscription ns
    WHERE ns.type = 'RADIUS'
      AND ns.enabled = true
      AND ST_DWithin(
          ST_SetSRID(ST_Point(ns.longitude, ns.latitude), 4326)::geography,
          ST_SetSRID(ST_Point(:sessionLon, :sessionLat), 4326)::geography,
          ns.radius_km * 1000
      )
    """, nativeQuery = true)
List<NotificationSubscription> findMatchingRadius(
    @Param("sessionLat") double lat, @Param("sessionLon") double lon);
```

**Prerequisite:** GiST index on `notification_subscription`:

```sql
CREATE INDEX idx_notif_sub_radius_coords
  ON notification_subscription
  USING GIST (ST_SetSRID(ST_Point(longitude, latitude), 4326));
```

**Module impact:** Add `hibernate-spatial` dependency to `donation-notification`. The `territory` and `donation-location` modules already transitively provide the JTS types.

---

## 3. Admin Matching: IN Clause → Optimized Hierarchy Traversal

| | Current | Future |
|---|---|---|
| **Query** | `WHERE administrative_division_id IN (:ids)` | Flat hierarchy table or recursive CTE |
| **Ancestor resolution** | Application-layer traversal via `DivisionService.findAncestors()` | Database-side traversal or materialized path |
| **Index usage** | B-tree on `administrative_division_id` | Same, but query shape changes |

**Current implementation:**

```java
// DefaultNotificationMatchingService.matchByAdministrativeDivision()
Set<UUID> divisionIds = new HashSet<>();
divisionIds.add(sessionDivision.getId());
for (ancestor : divisionService.findAncestors(sessionDivision.getId()))
    divisionIds.add(ancestor.getId());

List<NotificationSubscription> matching =
    repository.findMatchingAdminSubscriptions(divisionIds);
```

**Breakpoint:** Deep hierarchies (50+ levels, like Brazil's IBGE divisions) or high query frequency (>500/min).

**Recommended replacement path — Option A: Materialized flat list:**

Add a read-optimized query in the territory module:

```java
// AdministrativeDivisionRepository
@Query(value = """
    WITH RECURSIVE hierarchy AS (
        SELECT id, parent_id FROM administrative_division WHERE id = :id
        UNION ALL
        SELECT d.id, d.parent_id FROM administrative_division d
        JOIN hierarchy h ON d.id = h.parent_id
    )
    SELECT id FROM hierarchy
    """, nativeQuery = true)
List<UUID> findAllDivisionIdsInHierarchy(@Param("id") UUID divisionId);
```

Then the notification module calls this instead of `DivisionService.findAncestors()`.

**Recommended replacement path — Option B: Materialized path column (better for high throughput):**

Add `path` column to `administrative_division` table (e.g., `{portugal}/{aveiro-district}/{aveiro-municipality}`). Query with `LIKE` prefix match. This avoids recursive CTEs entirely.

**What to decouple:** The `DefaultNotificationMatchingService` should not depend on `DivisionService` (territory internals). Introduce `TerritoryHierarchyQuery` or `AncestorResolver` interface in `donation-notification`, implemented by territory.

---

---

## 4. NotificationRequest Bulk Insert

| | Current | Future |
|---|---|---|
| **Insert pattern** | One `INSERT` per `NotificationRequest` in a loop | Batch `INSERT` |
| **Transaction** | All within one `@Transactional`, but individual inserts | JDBC batch or `saveAll()` |

**Current implementation:**

```java
// NotificationGenerationService.handle()
for (UUID userId : interestedUsers) {
    if (!requestRepository.existsByUserIdAndSessionId(userId, sessionId)) {
        requestRepository.save(new NotificationRequest(userId, sessionId));
    }
}
```

**Breakpoint:** Sessions matching >1000 users.

**Recommended replacement:**

```java
List<NotificationRequest> requests = interestedUsers.stream()
    .filter(id -> !requestRepository.existsByUserIdAndSessionId(id, sessionId))
    .map(id -> NotificationRequest.builder().userId(id).sessionId(sessionId).build())
    .toList();

requestRepository.saveAll(requests);
```

Note: `saveAll` with Hibernate still emits individual INSERT statements by default. For true batch, add:

```yaml
spring.jpa.properties.hibernate.jdbc.batch_size: 100
spring.jpa.properties.hibernate.order_inserts: true
```

---

## 5. Territory Service Coupling

| | Current | Future |
|---|---|---|
| **Dependency** | `DefaultNotificationMatchingService` directly injects `DivisionService` | Injects a notification-owned interface |
| **Risk** | Territory module refactors break notification matching | Territory changes are insulated |

**Recommended change:**

Define the interface in `donation-notification`:

```java
// pt.sanguept.notification.services.TerritoryHierarchyQuery
public interface TerritoryHierarchyQuery {
    Set<UUID> resolveDivisionHierarchy(UUID divisionId);
}
```

Implement in `territory` module (or in `donation-notification` as a bridge):

```java
// pt.sanguept.territory.services.AdministrativeDivisionHierarchyQuery
@Service
public class AdministrativeDivisionHierarchyQuery implements TerritoryHierarchyQuery {
    private final DivisionService divisionService;

    @Override
    public Set<UUID> resolveDivisionHierarchy(UUID divisionId) {
        Set<UUID> ids = new HashSet<>();
        ids.add(divisionId);
        for (var ancestor : divisionService.findAncestors(divisionId)) {
            ids.add(ancestor.getId());
        }
        return ids;
    }
}
```

This is the Dependency Inversion Principle at the module boundary.

---

## 6. Territory Hierarchy Caching

| | Current | Future |
|---|---|---|
| **Resolution** | Every matching call traverses the full parent chain | Cached hierarchy tree |
| **Query count** | 1 query per session (for ancestors) | 0 queries (cache hit) |

**Breakpoint:** >100 sessions published per minute.

**Recommended approach:**

```java
@Service
public class CachedTerritoryHierarchyQuery implements TerritoryHierarchyQuery {
    private final AdministrativeDivisionRepository repository;

    @Cacheable(value = "division-hierarchy", key = "#divisionId")
    @Override
    public Set<UUID> resolveDivisionHierarchy(UUID divisionId) {
        // ... same logic, cached result
    }
}
```

With `@EnableCaching` and a Caffeine or Redis cache backend. Even a Spring default `ConcurrentMapCache` helps.

---

## 7. Module Split: Domain vs Delivery

**Current state:** Partial split implemented. Delivery module (`donation-notification-delivery`) now contains `NotificationSender` interface, `LoggingNotificationSender`, `NotificationProcessor`, and `NotificationSchedulingConfig`. Domain module (`donation-notification`) retains entities, repositories, services (matching, generation, preferences, subscriptions), controllers, and `NotificationRequestService`.

| Package | Responsibility | Future module |
|---|---|---|
| `enums`, `entities`, `repositories`, `dtos`, `mappers` | Domain model | `donation-notification` |
| `services/NotificationSubscriptionService` | Subscription CRUD | `donation-notification` |
| `services/NotificationPreferenceService` | Preference management | `donation-notification` |
| `services/NotificationMatchingService` | Core matching | `donation-notification` |
| `services/NotificationGenerationService` | Event → Request | `donation-notification` |
| `services/NotificationProcessor` | Polling & retry | `donation-notification-delivery` |
| `services/NotificationSender` (interface) | Delivery abstraction | `donation-notification-delivery` |
| `services/LoggingNotificationSender` | Log adapter | `donation-notification-delivery` |
| `services/NotificationRequestService` | Request queries | `donation-notification-delivery` |
| `config/NotificationSchedulingConfig` | Scheduler | `donation-notification-delivery` |

**Recommended split point:** After introducing a message queue. Before that, the polling mechanism keeps the module small enough.

```
donation-notification            # Domain: subscriptions, preferences, matching, event handlers
donation-notification-delivery   # Infrastructure: sender adapters, queue consumers, retry policies
donation-notification-messaging  # (If MQ): RabbitMQ config, bindings, serialization
```

---

## 8. Single-Table Subscription: When to Split

| | Current | Future |
|---|---|---|
| **Model** | One `notification_subscription` table, nullable columns | Inheritance or separate tables |
| **Trigger** | Cross-type queries become complex | OR new subscription types added |
| **Risk now** | None. Simplicity wins at current scale. | Nullable columns confuse new developers |

**When to refactor:** The day you add a third `SubscriptionType` (e.g., `HEALTH_FACILITY`, `BLOOD_TYPE`). Single-table with 3+ null-sets per type becomes unreadable. At that point, evaluate:

- **Option A:** `@Inheritance(strategy = SINGLE_TABLE)` with `@DiscriminatorColumn` (adds `dtype` column, keeps single table)
- **Option B:** Separate tables with `SubscriptionConfig` interface (more normalized, harder to query across types)

Both are reasonable. Option A is the simpler migration path.

---

## 9. Radius Subscription Dedup Model

| | Current | Future |
|---|---|---|
| **Uniqueness key** | `(user_id, latitude, longitude, radius_km)` | Named subscriptions with fuzzy dedup |
| **Problem** | `40.12345, -8.12345` ≠ `40.12346, -8.12346` (same intent) | Too strict |

**When to fix:** If users complain about duplicate subscriptions OR you add a UI that lets them name their circles.

**Recommended approach:** Add `name` field to `NotificationSubscription` (optional, only for RADIUS type). Make uniqueness `(user_id, name)` for radius, `(user_id, admin_division_id)` for admin as today. This also gives users a way to differentiate "Home" vs "Work" circles.

**Alternative:** Geohash-based dedup. Round lat/lon to 3 decimal places (~111m precision) and compute a hash. But this adds complexity without user-facing benefit.

---

## 10. Observability & Metrics

| | Current | Future |
|---|---|---|
| **Visibility** | Log lines only | Micrometer metrics + health endpoint |
| **What's missing** | Processing rate, failure rate, queue depth, P99 latency | All of these |

**Recommended additions (when Actuator/Micrometer is added):**

```java
// In NotificationProcessor
private final Counter processedCounter;
private final Counter failedCounter;
private final Timer processingTimer;
```

```java
// In NotificationMatchingService
private final Timer matchingTimer;
private final Counter matchesCounter;
```

**Key metrics to expose:**

| Metric | Type | Description |
|---|---|---|
| `notification.processing.batch.size` | Gauge | Pending requests in current batch |
| `notification.processing.success` | Counter | Successfully delivered |
| `notification.processing.failure` | Counter | Failed (all attempts exhausted) |
| `notification.processing.retry` | Counter | Retried (not yet failed) |
| `notification.matching.duration` | Timer | Time spent in matching algorithm |
| `notification.matching.candidates` | Counter | Users matched per session |
| `notification.queue.depth` | Gauge | COUNT(*) WHERE status = PENDING |

**Lightweight approach (no additional dependencies):**

Add a `/api/v1/notification-requests/stats` endpoint that runs:

```sql
SELECT status, COUNT(*) FROM notification_request GROUP BY status;
```

No Micrometer needed. Just a read endpoint.

---

---

## 11. Fan-out Model (Corrected)

**Current assumption (wrong if held long-term):** 1 matched user → 1 request → 1 delivery

**Correct model:** 1 request → sender produces N deliveries (based on runtime resolution)

**Why fan-out belongs in sender, NOT generation:**

Channel selection depends on:
- User preferences (mutable — update independently of matching)
- Enabled integrations (system-level config — toggle without re-matching)
- Channel health (runtime condition — circuit breaker per channel)
- Future A/B routing or fallback logic

None of these belong in domain or matching layers.

**Correct pipeline:**

```
SessionPublishedEvent
    → MatchingService
    → Set<UUID> userIds

    → NotificationGenerationService
    → NotificationRequest(userId, sessionId, idempotencyKey)

    → NotificationProcessor / MQ consumer
    → NotificationSender.send(request)

    → Sender:
         resolve user channels
         fan-out internally
         deliver per channel
         ensure idempotency per channel
```

**Why generation-level fan-out is wrong:**
- Explodes DB rows (1 request becomes N rows preemptively)
- Couples matching to delivery topology
- Schema changes required every time a channel is added
- Breaks retry boundaries (retrying all channels when only one failed)

---

## 12. Queue Ordering & Partitioning (Corrected)

**Problem with current design:** Single FIFO queue (`created_at ASC`) fails under burst traffic, mixed urgency workloads, and replay/backfill operations.

**A `priority INT` column alone does NOT solve structural contention** — it still shares a single queue with sorting overhead under load.

**Correct model: logical queue partitioning.**

Instead of encoding priority per row, define separate processing lanes:

| Queue | Purpose |
|---|---|
| `notification.urgent` | Expiring / high-impact sessions (e.g. session starts in <2h) |
| `notification.normal` | Standard delivery |
| `notification.backfill` | Replay / rebuild operations |

**Routing rule:** At generation or enqueue time:
```
if session.startAt - now < threshold → urgent queue
else → normal queue
```

Replay always routes to backfill queue.

**Why partitioning beats a priority column:**
- Isolates workload types — urgent never starves behind backfill
- Prevents contention by design (not by sorting)
- Maps cleanly to MQ later: RabbitMQ exchanges, Kafka partitions
- Each partition can have independent processing rate, batch size, and retry policy

**Implementation:** Initially a `queue` column on `notification_request`. Later, separate physical queues.

---

---

## 13. Replayability / Reprocessing Strategy

| | Current | Risk |
|---|---|---|
| **Model** | One-time generation | No safe reprocessing |

**Problem:** If matching logic changes, old sessions are frozen. No recomputation possible. Bug fixes cannot be applied retroactively.

**Required capability:**
```
rebuildNotifications(sessionId)
rebuildNotifications(fromDate, toDate)
```

**Critical subtlety — deterministic matching contract:**

Matching must be reproducible. Current inputs change:
- Subscriptions added/removed after session publication
- Preferences updated (mute/enable)
- Territory hierarchy restructured

Replay is NOT just re-running the function. It requires either:
- **Snapshot strategy:** Capture subscription/preference/territory state at the moment of generation, store alongside the request, replay against the snapshot
- **Approximate strategy:** Replay against current state, accept non-determinism, document that replay may produce different results

Option A is correct for auditability. Option B is acceptable for disaster recovery.

**Two modes:**
- Live mode → event-driven generation (current)
- Replay mode → explicit invocation, routed to `backfill` queue

---

## 14. User Notification State Model (Observability Gap)

| | Current | Risk |
|---|---|---|
| **Data** | `NotificationRequest` | Fragmented visibility |

**Problem:** You cannot answer: "what did user X actually receive?", "what was suppressed?", "what failed?"

**Required abstraction — read model:**

```java
class UserNotificationState {
    UUID userId;
    UUID sessionId;
    NotificationStatus status;
    Instant lastAttemptAt;
    String failureReason;
}
```

**Purpose:** This is NOT operational data. It is a support/debugging + analytics read model. Derived from `NotificationRequest`, materialized on delivery.

**Trigger for introduction:** First user-facing notification history feature or support/debug tooling.

---

## Architecture Core Model

```
┌─────────────┐
│   MATCHING   │  input:  session + subscriptions
│              │  output: Set<UUID> userIds
│              │  constraint: pure, no channels, no delivery logic
└──────┬───────┘
       │
       ▼
┌─────────────┐
│  GENERATION  │  input:  Set<UUID> userIds
│              │  output: NotificationRequest(userId, sessionId, idempotencyKey)
│              │  constraint: persistence only, no fan-out, no channel awareness
└──────┬───────┘
       │
       ▼
┌─────────────┐
│    SENDER    │  input:  NotificationRequest
│              │  responsibilities:
│              │    - resolve user channels
│              │    - fan-out internally
│              │    - deliver per channel
│              │    - ensure idempotency
│              │  constraint: no domain logic, no matching, no subscription awareness
└─────────────┘
```

**This boundary is the single most important architectural decision in the system.** Matching should never know channels. Sender should never know subscriptions.

---

## 15. Priorities & Trigger Conditions

Ranked by "when this breaks, it breaks hard":

| Priority | Item | Trigger condition | Effort |
|---|---|---|---|
| P0 | Radius: PostGIS ST_DWithin (Section 2) | >10k radius subs OR >100 sessions/min | Medium |
| ~~P1~~ PARTIAL | Module split: domain vs delivery (Section 7) | Before Telegram/email/SMS | Medium |
| P1 | Territory interface decoupling (Section 5) | Before territory module refactoring | Low |
| P1 | NotificationRequest bulk insert (Section 4) | Sessions match >1k users | Low |
| P2 | Message queue (Section 1) | >10 sessions/sec OR latency SLA <10s | High |
| P2 | Territory hierarchy caching (Section 6) | >100 sessions/min | Low |
| P2 | Queue partitioning (Section 12) | >500 pending requests sustained | Low |
| P2 | Fan-out → sender (Section 11) | Before multi-channel delivery | Low |
| P3 | Admin: optimized hierarchy (Section 3) | >50 hierarchy levels OR >500 queries/min | Medium |
| P3 | Named radius subscriptions (Section 9) | User complaints about duplicates OR UI addition | Medium |
| P3 | User notification state model (Section 14) | First support/debug tooling | Low |
| P4 | Single-table split (Section 8) | 3rd subscription type added | Medium |
| P4 | Observability (Section 10) | First production incident | Low |
| P4 | Replayability (Section 13) | Matching logic change or bug fix | Medium |

---

## Summary

The `donation-notification` module was deliberately built with:

- **Application-layer spatial math** instead of PostGIS spatial queries
- **Database polling** instead of a message broker
- **Inline ancestor traversal** instead of cached/materialized hierarchies
- **Single-table subscription model** with nullable columns
- **Tight coupling to territory internals** via `DivisionService`
- **Log-based observability** instead of metrics infrastructure
- **Implicit ordering** instead of explicit queue partitioning

Every one of these was a deliberate trade-off: **simple now, easy to replace later**. None are architectural mistakes. They are acceleration decisions.

The system can scale to ~10,000 users and ~100 sessions/day with zero changes. Beyond that, the remaining P0 item (spatial matching via PostGIS, Section 2) is the primary performance target.

The single most important architectural boundary to protect: **matching → generation → sender**. Matching never knows channels. Sender never knows subscriptions. This boundary is what prevents notification systems from becoming unmaintainable.

Everything in this document should be reevaluated after the first external delivery channel is integrated, because external I/O dominates the performance profile.
