package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Respuesta paginada estándar. Se usa en endpoints que retornan listas (ej: /api/usuarios).")
public record PaginatedResponseDto<T>(
        @Schema(description = "Lista de elementos de la página actual")
        List<T> data,

        @Schema(description = "Metadatos de paginación")
        Pagination pagination
) {
    @Schema(description = "Información de la página actual")
    public record Pagination(
            @Schema(description = "Número de página (1-indexed)", example = "1")
            int page,

            @Schema(description = "Cantidad de elementos por página (max 100)", example = "20")
            int limit,

            @Schema(description = "Total de elementos en todas las páginas", example = "47")
            long total,

            @Schema(description = "Cantidad total de páginas", example = "3")
            int pages
    ) {}

    public static <T> PaginatedResponseDto<T> from(ListarUsuariosUseCase.ListadoPaginado<T> p) {
        return new PaginatedResponseDto<>(
                p.data(),
                new Pagination(p.page(), p.size(), p.total(), p.pages())
        );
    }
}
