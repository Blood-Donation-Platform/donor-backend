package pt.sanguept.territory.dtos;

import java.util.List;

public record DivisionSelectorDto(Long id, String name, List<AncestorDto> ancestors, int depth) {}
