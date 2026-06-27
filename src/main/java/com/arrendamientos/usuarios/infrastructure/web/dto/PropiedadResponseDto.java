package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.Propiedad;

import java.time.Instant;
import java.util.List;

public record PropiedadResponseDto(
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
    public static PropiedadResponseDto from(Propiedad p) {
        return new PropiedadResponseDto(
                p.id(), p.titulo(), p.descripcion(), p.precio(), p.moneda(),
                p.provincia(), p.canton(), p.distrito(), p.tipo(), p.estado(),
                p.imagenes(), p.duenoId(), p.caracteristicas(), p.createdAt()
        );
    }
}
