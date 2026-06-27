package com.arrendamientos.usuarios.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Mock in-memory de una propiedad. Se usa únicamente para que el frontend
 * pueda listar y detallar el catálogo público mientras el microservicio
 * ms-propiedades esté en desarrollo. NO persiste en DB.
 */
public record Propiedad(
        String id,
        String titulo,
        String descripcion,
        double precio,
        String moneda,
        String provincia,
        String canton,
        String distrito,
        String tipo,
        String estado,
        List<String> imagenes,
        String duenoId,
        List<String> caracteristicas,
        Instant createdAt
) {
}
