package com.arrendamientos.usuarios.infrastructure.web.dto;

import java.util.List;

public record PropiedadesListadoDto(
        List<PropiedadResponseDto> data,
        long total,
        int page,
        int totalPages
) {
}
