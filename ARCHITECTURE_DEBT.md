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

## 4. Preference Filtering: Per-User DB Round-Trip → Batch Query

| | Current | Future |
|---|---|---|
| **Pattern** | `preferenceRepository.findByUserId(id)` — one query per candidate | Single batch query |
| **N+1 risk** | 100 candidates = 100 preference queries | 100 candidates = 1 query |

**Current implementation:**

```java
// DefaultNotificationMatchingService.filterByPreferences()
for (UUID userId : candidateUserIds) {
    NotificationPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
    if (pref == null || (pref.isEnabled() && notMuted(pref))) {
        result.add(userId);
    }
}
```

**Breakpoint:** Sessions matching >500 users at once.

**Recommended replacement:**

```java
// NotificationPreferenceRepository
@Query("SELECT p FROM NotificationPreference p WHERE p.userId IN :userIds")
List<NotificationPreference> findByUserIdIn(@Param("userIds") Set<UUID> userIds);
```

Then convert to Map and filter in application layer:

```java
Map<UUID, NotificationPreference> prefMap = preferences.stream()
    .collect(toMap(NotificationPreference::getUserId, Function.identity()));

for (UUID userId : candidateUserIds) {
    NotificationPreference pref = prefMap.getOrDefault(userId, null);
    if (pref == null || (pref.isEnabled() && notMuted(pref))) {
        result.add(userId);
    }
}
```

---

## 5. NotificationRequest Bulk Insert

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

## 6. Territory Service Coupling

| | Current | Future |
|---|---|---|
| **Dependency** | `DefaultNotificationMatchingService` directly injects `DivisionService` | Injects a notification-owned interface |
| **Risk** | Territory module refactors break notification matching | Territory changes are insulated |

**Recommended change:**

Define the interface in `donation-notification`:

```java
// pt.sanguept.donationnotification.services.TerritoryHierarchyQuery
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

## 7. Territory Hierarchy Caching

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

## 8. Module Split: Domain vs Delivery

**Current state:** Everything in `donation-notification`.

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

## 9. Single-Table Subscription: When to Split

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

## 10. Radius Subscription Dedup Model

| | Current | Future |
|---|---|---|
| **Uniqueness key** | `(user_id, latitude, longitude, radius_km)` | Named subscriptions with fuzzy dedup |
| **Problem** | `40.12345, -8.12345` ≠ `40.12346, -8.12346` (same intent) | Too strict |

**When to fix:** If users complain about duplicate subscriptions OR you add a UI that lets them name their circles.

**Recommended approach:** Add `name` field to `NotificationSubscription` (optional, only for RADIUS type). Make uniqueness `(user_id, name)` for radius, `(user_id, admin_division_id)` for admin as today. This also gives users a way to differentiate "Home" vs "Work" circles.

**Alternative:** Geohash-based dedup. Round lat/lon to 3 decimal places (~111m precision) and compute a hash. But this adds complexity without user-facing benefit.

---

## 11. Observability & Metrics

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

## 12. Priorities & Trigger Conditions

Ranked by "when this breaks, it breaks hard":

| Priority | Item | Trigger condition | Effort |
|---|---|---|---|
| P0 | Radius: PostGIS ST_DWithin (Section 2) | >10k radius subs OR >100 sessions/min | Medium |
| P0 | Batch preference query (Section 4) | Sessions match >500 users | Low |
| P1 | Module split: domain vs delivery (Section 8) | Before adding Telegram/email/SMS | Medium |
| P1 | Territory interface decoupling (Section 6) | Before territory module refactoring | Low |
| P1 | NotificationRequest bulk insert (Section 5) | Sessions match >1k users | Low |
| P2 | Message queue (Section 1) | >10 sessions/sec OR latency SLA <10s | High |
| P2 | Territory hierarchy caching (Section 7) | >100 sessions/min | Low |
| P3 | Admin: optimized hierarchy (Section 3) | >50 hierarchy levels OR >500 queries/min | Medium |
| P3 | Named radius subscriptions (Section 10) | User complaints about duplicates OR UI addition | Medium |
| P4 | Single-table split (Section 9) | 3rd subscription type added | Medium |
| P4 | Observability (Section 11) | First production incident | Low |

---

## Summary

The `donation-notification` module was deliberately built with:

- **Application-layer spatial math** instead of PostGIS spatial queries
- **Database polling** instead of a message broker
- **Inline ancestor traversal** instead of cached/materialized hierarchies
- **Per-user preference lookups** instead of batch queries
- **Single-table subscription model** with nullable columns
- **Tight coupling to territory internals** via `DivisionService`
- **Log-based observability** instead of metrics infrastructure

Every one of these was a deliberate trade-off: **simple now, easy to replace later**. None of them are architectural mistakes. They are acceleration decisions.

The entire system can scale to ~10,000 users and ~100 sessions/day with zero changes. Beyond that, the P0 items above become the first optimization targets.

Everything in this document should be reevaluated after the first external delivery channel (Telegram/email/SMS) is integrated, because the throughput characteristics of external I/O will dominate the performance profile.
