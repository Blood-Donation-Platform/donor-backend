package pt.sanguept.donationnotification.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.sanguept.commoninfra.entities.CreationAuditedEntity;
import pt.sanguept.donationnotification.enums.NotificationRequestStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_request",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "session_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class NotificationRequest extends CreationAuditedEntity {

    private static final int MAX_ATTEMPTS = 3;

    private static final Duration[] BACKOFF_DELAYS = {
        Duration.ofSeconds(30),
        Duration.ofSeconds(120)
    };

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationRequestStatus status = NotificationRequestStatus.PENDING;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    public boolean hasExceededMaxAttempts() {
        return attemptCount >= MAX_ATTEMPTS;
    }

    public void computeNextAttemptAt() {
        int idx = attemptCount - 1;
        if (idx < 0) idx = 0;
        if (idx >= BACKOFF_DELAYS.length) {
            this.nextAttemptAt = null;
        } else {
            this.nextAttemptAt = Instant.now().plus(BACKOFF_DELAYS[idx]);
        }
    }

}
