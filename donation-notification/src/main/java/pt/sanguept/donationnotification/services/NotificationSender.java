package pt.sanguept.donationnotification.services;

import java.util.UUID;

/**
 * Abstraction for delivering a notification to a user.
 * <p>
 * Implementations MUST be safe to retry with the same {@code requestId}.
 * If the delivery has already occurred for a given {@code requestId},
 * the implementation should detect this and silently succeed (idempotent).
 */
public interface NotificationSender {

    void send(UUID requestId, UUID userId, UUID sessionId);

}
