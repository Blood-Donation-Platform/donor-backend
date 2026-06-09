package pt.sanguept.territory.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.sanguept.commoninfra.jpa.SpecBuilder;
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

/**
 * Read-only query service for AdministrativeDivision related lookups.
 * Keeps all query logic separate from mutation/maintenance operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DivisionService {
	
  	private final AdministrativeDivisionRepository repository;

	public Page<AdministrativeDivisionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(AdministrativeDivisionMapper::toDto);
    }

    public Page<AdministrativeDivisionDto> search(DivisionFilter filter, Pageable pageable) {
        var sb = new SpecBuilder<AdministrativeDivision>()
                .like("name", filter.name())
                .eq("parent.id", filter.parentId())
                .isNull("parent", filter.rootOnly());
        return repository.findAll(sb.build(), pageable).map(AdministrativeDivisionMapper::toDto);
    }

	public Optional<AdministrativeDivision> findParent(Long childId) {
		return repository.findById(childId).map(AdministrativeDivision::getParent);
	}

	public List<AdministrativeDivision> findAncestors(Long childId) {
		return repository.findById(childId)
				.map(this::collectAncestors)
				.orElseGet(Collections::emptyList);
	}

	public List<DivisionSelectorDto> searchSelector(String query, int size) {
		return repository.findAll(nameLike(query), PageRequest.of(0, size)).stream()
				.map(d -> {
					var ancestors = collectAncestors(d).stream()
							.map(a -> new AncestorDto(a.getId(), a.getName()))
							.toList();
					return new DivisionSelectorDto(d.getId(), d.getName(), ancestors, ancestors.size());
				})
				.toList();
	}

	public static Specification<AdministrativeDivision> nameLike(String query) {
		return (root, cq, cb) -> {
			if (query == null || query.isBlank()) {
				return cb.conjunction();
			}
			return cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%");
		};
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


