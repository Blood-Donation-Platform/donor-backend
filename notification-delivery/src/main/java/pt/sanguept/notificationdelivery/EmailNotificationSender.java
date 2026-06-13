package pt.sanguept.notificationdelivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.sanguept.communications.email.EmailService;
import pt.sanguept.donationsession.entities.DonationSession;
import pt.sanguept.donationsession.repositories.DonationSessionRepository;
import pt.sanguept.notification.entities.NotificationRequest;
import pt.sanguept.user.entities.User;
import pt.sanguept.user.repositories.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final DonationSessionRepository sessionRepository;

    @Override
    public void send(NotificationRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty() || userOpt.get().getEmail() == null) {
            log.debug("Skipping email for user {}: no email address", request.getUserId());
            return;
        }

        Optional<DonationSession> sessionOpt = sessionRepository.findById(request.getSessionId());
        if (sessionOpt.isEmpty()) {
            log.debug("Skipping email for user {}: session {} not found", request.getUserId(), request.getSessionId());
            return;
        }

        User user = userOpt.get();
        DonationSession session = sessionOpt.get();

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("title", session.getTitle());
        variables.put("description", session.getDescription());
        if (session.getLocation() != null) {
            variables.put("location", session.getLocation().getName());
        }
        variables.put("startAt", session.getStartAt());
        variables.put("endAt", session.getEndAt());

        emailService.sendTemplate(user.getEmail(), "session-published", variables);
    }

}
