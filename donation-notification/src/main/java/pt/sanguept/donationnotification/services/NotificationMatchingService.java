package pt.sanguept.donationnotification.services;

import pt.sanguept.donationsession.entities.DonationSession;

import java.util.Set;
import java.util.UUID;

public interface NotificationMatchingService {

    Set<UUID> findInterestedUsers(DonationSession session);

}
