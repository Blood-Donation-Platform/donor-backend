package pt.sanguept.donationnotification.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.donationnotification.dtos.NotificationRequestDto;
import pt.sanguept.donationnotification.enums.NotificationRequestStatus;
import pt.sanguept.donationnotification.repositories.NotificationRequestRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationRequestService {

    private final NotificationRequestRepository repository;

    public List<NotificationRequestDto> findFailed() {
        return repository.findByStatusOrderByCreatedAtDesc(NotificationRequestStatus.FAILED)
                .stream()
                .map(r -> NotificationRequestDto.builder()
                        .id(r.getId())
                        .userId(r.getUserId())
                        .sessionId(r.getSessionId())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .processedAt(r.getProcessedAt())
                        .attemptCount(r.getAttemptCount())
                        .lastAttemptAt(r.getLastAttemptAt())
                        .failureReason(r.getFailureReason())
                        .build())
                .toList();
    }

}
