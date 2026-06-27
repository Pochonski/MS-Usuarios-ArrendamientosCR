package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.domain.model.Propiedad;

import java.util.List;
import java.util.Optional;

/**
 * Caso de uso para listar y detallar el catálogo público de propiedades.
 * Backed por un mock in-memory hasta que ms-propiedades esté disponible.
 */
public interface CatalogoPropiedadesUseCase {

    ListadoPropiedades listar(FiltrosPropiedades filtros);

    Optional<Propiedad> porId(String id);

    record ListadoPropiedades(List<Propiedad> data, long total, int page, int totalPages) {}

    record FiltrosPropiedades(
            String search,
            String provincia,
            String tipo,
            Double precioMin,
            Double precioMax,
            String duenoId,
            int page,
            int size
    ) {}
}
