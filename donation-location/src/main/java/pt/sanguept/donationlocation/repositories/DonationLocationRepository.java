package pt.sanguept.donationlocation.repositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.donationlocation.entities.DonationLocation;

import java.util.List;
import java.util.UUID;

@Repository
public interface DonationLocationRepository extends BaseRepository<DonationLocation, UUID>, JpaSpecificationExecutor<DonationLocation> {

    @Query(value = """
        SELECT l.id, l.name, ST_Y(l.coordinates) AS latitude, ST_X(l.coordinates) AS longitude
        FROM donation_location l
        WHERE l.active = true
        AND l.coordinates && ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326)
        ORDER BY l.name
        """, nativeQuery = true)
    List<Object[]> findMapLocationsInBounds(@Param("swLat") double swLat, @Param("swLng") double swLng,
                                            @Param("neLat") double neLat, @Param("neLng") double neLng);
}
