package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase;

import java.util.List;

public record PaginatedResponseDto<T>(
        List<T> data,
        Pagination pagination
) {
    public record Pagination(int page, int limit, long total, int pages) {}

    public static <T> PaginatedResponseDto<T> from(ListarUsuariosUseCase.ListadoPaginado<T> p) {
        return new PaginatedResponseDto<>(
                p.data(),
                new Pagination(p.page(), p.size(), p.total(), p.pages())
        );
    }
}
