package pt.sanguept.territory.dtos;

import java.util.List;
import java.util.UUID;

public record DivisionSelectorDto(UUID id, String name, List<AncestorDto> ancestors, int depth) {}
