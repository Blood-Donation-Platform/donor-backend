package pt.sanguept.territory.repositories;

import pt.sanguept.commoninfra.repositories.BaseRepository;
import pt.sanguept.territory.entities.Country;
import java.util.Optional;

public interface CountryRepository extends BaseRepository<Country, Integer> {

	Optional<Country> findByCode(String code);
}
