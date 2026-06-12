package pt.sanguept.donationnotification.services;

import pt.sanguept.donationnotification.entities.NotificationRequest;

/**
 * Abstraction for delivering a notification to a user.
 * <p>
 * Implementations receive the full {@link NotificationRequest} entity and are
 * responsible for resolving delivery channels, performing fan-out internally,
 * and ensuring idempotency using the request's {@code idempotencyKey}.
 * <p>
 * Implementations MUST be safe to retry — if the delivery has already occurred
 * for a given request, the implementation should detect this and silently succeed.
 */
public interface NotificationSender {

    void send(NotificationRequest request);

}
