package com.arrendamientos.usuarios.infrastructure.web;

import com.arrendamientos.usuarios.domain.port.in.CatalogoPropiedadesUseCase;
import com.arrendamientos.usuarios.infrastructure.web.dto.PropiedadResponseDto;
import com.arrendamientos.usuarios.infrastructure.web.dto.PropiedadesListadoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo público de propiedades. Endpoint mock para que el frontend pueda
 * listar y detallar el inventario mientras el microservicio ms-propiedades
 * esté en desarrollo. NO requiere autenticación.
 */
@RestController
@RequestMapping("/api/propiedades")
@Tag(name = "Propiedades", description = "Catálogo público (mock en memoria)")
public class PropiedadesController {

    private final CatalogoPropiedadesUseCase catalogo;

    public PropiedadesController(CatalogoPropiedadesUseCase catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping
    @Operation(summary = "Listar propiedades con filtros y paginación")
    public ResponseEntity<PropiedadesListadoDto> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String provincia,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String duenoId
    ) {
        CatalogoPropiedadesUseCase.ListadoPropiedades r = catalogo.listar(
                new CatalogoPropiedadesUseCase.FiltrosPropiedades(
                        search, provincia, tipo, precioMin, precioMax, duenoId, page, limit
                )
        );
        PropiedadesListadoDto body = new PropiedadesListadoDto(
                r.data().stream().map(PropiedadResponseDto::from).toList(),
                r.total(), r.page(), r.totalPages()
        );
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una propiedad por id")
    public ResponseEntity<PropiedadResponseDto> porId(@PathVariable String id) {
        return catalogo.porId(id)
                .map(PropiedadResponseDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
