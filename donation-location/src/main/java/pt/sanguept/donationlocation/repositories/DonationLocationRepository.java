package pt.sanguept.donationlocation.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.donationlocation.entities.DonationLocation;

import java.util.UUID;

@Repository
public interface DonationLocationRepository extends BaseRepository<DonationLocation, UUID>, JpaSpecificationExecutor<DonationLocation> {
}
