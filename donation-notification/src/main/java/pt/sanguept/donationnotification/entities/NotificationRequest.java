package pt.sanguept.donationnotification.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.sanguept.commoninfra.entities.CreationAuditedEntity;
import pt.sanguept.donationnotification.enums.NotificationRequestStatus;

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

    public boolean hasExceededMaxAttempts() {
        return attemptCount >= MAX_ATTEMPTS;
    }

}
