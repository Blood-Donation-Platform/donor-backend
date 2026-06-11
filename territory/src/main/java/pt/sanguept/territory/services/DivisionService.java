package pt.sanguept.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.territory.dtos.AdministrativeDivisionDto;
import pt.sanguept.territory.dtos.AncestorDto;
import pt.sanguept.territory.dtos.DivisionFilter;
import pt.sanguept.territory.dtos.DivisionSelectorDto;
import pt.sanguept.territory.entities.AdministrativeDivision;
import pt.sanguept.territory.mappers.AdministrativeDivisionMapper;
import pt.sanguept.territory.repositories.AdministrativeDivisionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only query service for AdministrativeDivision related lookups.
 * Keeps all query logic separate from mutation/maintenance operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DivisionService {
	
  	private final AdministrativeDivisionRepository repository;

    public Page<AdministrativeDivisionDto> search(DivisionFilter filter, Pageable pageable) {
        var spec = nameContains(filter.name())
                .and(parentIdEq(filter.parentId()))
                .and(parentIsNull(filter.rootOnly()));
        return repository.findAll(spec, pageable).map(AdministrativeDivisionMapper::toDto);
    }

	public Optional<AdministrativeDivision> findParent(UUID childId) {
		return repository.findById(childId).map(AdministrativeDivision::getParent);
	}

	public List<AdministrativeDivision> findAncestors(UUID childId) {
		return repository.findById(childId)
				.map(this::collectAncestors)
				.orElseGet(Collections::emptyList);
	}

	public List<DivisionSelectorDto> searchSelector(String query, int size) {
		if (query == null || query.isBlank()) {
			return List.of();
		}

		List<AdministrativeDivision> prefixResults = repository
				.findAll(nameStartsWith(query), PageRequest.of(0, size))
				.stream().toList();

		if (prefixResults.size() >= size) {
			return toSelectorDtoList(prefixResults);
		}

		int remaining = size - prefixResults.size();
		List<UUID> foundIds = prefixResults.stream()
				.map(AdministrativeDivision::getId).toList();
		List<AdministrativeDivision> fallbackResults = repository
				.findAll(nameContains(query).and(idNotIn(foundIds)), PageRequest.of(0, remaining))
				.stream().toList();

		List<AdministrativeDivision> all = new ArrayList<>(prefixResults);
		all.addAll(fallbackResults);
		return toSelectorDtoList(all);
	}

	static Specification<AdministrativeDivision> nameStartsWith(String query) {
		return (root, cq, cb) ->
				cb.like(cb.lower(root.get("name")), query.toLowerCase() + "%");
	}

	static Specification<AdministrativeDivision> nameContains(String query) {
		return (root, cq, cb) -> {
			if (query == null || query.isBlank()) {
				return cb.conjunction();
			}
			return cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%");
		};
	}

	static Specification<AdministrativeDivision> idNotIn(List<UUID> ids) {
		return (root, cq, cb) ->
				ids.isEmpty() ? cb.conjunction() : cb.not(root.get("id").in(ids));
	}

	static Specification<AdministrativeDivision> parentIdEq(UUID parentId) {
		return (root, cq, cb) ->
				parentId == null ? cb.conjunction() : cb.equal(root.get("parent").get("id"), parentId);
	}

	static Specification<AdministrativeDivision> parentIsNull(Boolean value) {
		return (root, cq, cb) ->
				value == null || !value ? cb.conjunction() : cb.isNull(root.get("parent"));
	}

	private List<DivisionSelectorDto> toSelectorDtoList(List<AdministrativeDivision> divisions) {
		return divisions.stream()
				.map(d -> {
					var ancestors = collectAncestors(d).stream()
							.map(a -> new AncestorDto(a.getId(), a.getName()))
							.toList();
					return new DivisionSelectorDto(d.getId(), d.getName(), ancestors, ancestors.size());
				})
				.toList();
	}

	private List<AdministrativeDivision> collectAncestors(AdministrativeDivision division) {
		List<AdministrativeDivision> ancestors = new ArrayList<>();
		AdministrativeDivision current = division.getParent();
		while (current != null) {
			ancestors.add(current);
			current = current.getParent();
		}
		Collections.reverse(ancestors);
		return ancestors;
	}
}


