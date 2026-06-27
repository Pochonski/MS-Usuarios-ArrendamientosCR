package com.arrendamientos.usuarios.application.service;

import com.arrendamientos.usuarios.domain.model.Propiedad;
import com.arrendamientos.usuarios.domain.port.in.CatalogoPropiedadesUseCase;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Implementación mock del catálogo público. Mantiene 24 propiedades en memoria
 * — suficiente para que el frontend pueda demostrar paginación, filtros y
 * detalle sin depender del ms-propiedades. Reemplazar por un adapter JPA
 * cuando ese microservicio exista.
 */
@Service
public class CatalogoPropiedadesService implements CatalogoPropiedadesUseCase {

    private static final List<Propiedad> DATA = List.of(
            new Propiedad("prop-001", "Apartamento moderno en Escazú",
                    "Apartamento de 2 habitaciones con vista a la ciudad, piscina, gimnasio y seguridad 24/7.",
                    450000.0, "CRC", "San José", "Escazú", "San Rafael",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?w=800"),
                    "usr-001", List.of("2 hab", "2 baños", "Piscina", "Gimnasio"),
                    Instant.parse("2026-01-10T10:00:00Z")),
            new Propiedad("prop-002", "Casa familiar en Heredia centro",
                    "Casa de 3 habitaciones con jardín amplio, garaje para 2 carros, cerca de escuelas.",
                    650000.0, "CRC", "Heredia", "Heredia", "Heredia centro",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=800"),
                    "usr-001", List.of("3 hab", "2 baños", "Jardín", "Garaje"),
                    Instant.parse("2026-01-12T10:00:00Z")),
            new Propiedad("prop-003", "Local comercial en Avenida Central",
                    "Local de 80m² en zona de alto tráfico, ideal para tienda u oficina.",
                    850000.0, "CRC", "San José", "San José", "Carmen",
                    "local", "disponible",
                    List.of("https://images.unsplash.com/photo-1497366216548-37526070297c?w=800"),
                    "usr-003", List.of("80m²", "1 baño", "Alto tráfico"),
                    Instant.parse("2026-01-15T10:00:00Z")),
            new Propiedad("prop-004", "Apartamento amueblado en Rohrmoser",
                    "Totalmente amueblado, 1 habitación, ideal para ejecutivo. Incluye mantenimiento.",
                    380000.0, "CRC", "San José", "San José", "Rohrmoser",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=800"),
                    "usr-003", List.of("1 hab", "1 baño", "Amueblado", "Mantenimiento"),
                    Instant.parse("2026-01-18T10:00:00Z")),
            new Propiedad("prop-005", "Casa con piscina en Alajuela",
                    "Casa de 4 habitaciones, piscina privada, terraza con BBQ.",
                    950000.0, "CRC", "Alajuela", "Alajuela", "Alajuela centro",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=800"),
                    "usr-001", List.of("4 hab", "3 baños", "Piscina", "BBQ"),
                    Instant.parse("2026-01-20T10:00:00Z")),
            new Propiedad("prop-006", "Oficina ejecutiva en Santa Ana",
                    "Oficina moderna de 60m² en torre empresarial, aire acondicionado, parking.",
                    720000.0, "CRC", "San José", "Santa Ana", "Santa Ana centro",
                    "oficina", "disponible",
                    List.of("https://images.unsplash.com/photo-1497366754035-f200968a6e72?w=800"),
                    "usr-005", List.of("60m²", "A/C", "Parking", "Sala juntas"),
                    Instant.parse("2026-01-22T10:00:00Z")),
            new Propiedad("prop-007", "Apartamento con vista al mar en Jacó",
                    "2 habitaciones, balcón con vista al océano, acceso directo a la playa.",
                    580000.0, "CRC", "Puntarenas", "Garabito", "Jacó",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1499793983690-e29da59ef1c2?w=800"),
                    "usr-003", List.of("2 hab", "2 baños", "Vista mar", "Playa"),
                    Instant.parse("2026-01-25T10:00:00Z")),
            new Propiedad("prop-008", "Bodega industrial en Cartago",
                    "Bodega de 300m² con portón eléctrico, oficina mezzanine, zona industrial.",
                    1200000.0, "CRC", "Cartago", "Cartago", "Cartago centro",
                    "bodega", "disponible",
                    List.of("https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=800"),
                    "usr-005", List.of("300m²", "Portón eléctrico", "Oficina"),
                    Instant.parse("2026-01-28T10:00:00Z")),
            new Propiedad("prop-009", "Casa colonial en Cartago",
                    "Encantadora casa colonial restaurada, 3 habitaciones, patio interior.",
                    520000.0, "CRC", "Cartago", "Cartago", "Guadalupe",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800"),
                    "usr-001", List.of("3 hab", "2 baños", "Patio", "Colonial"),
                    Instant.parse("2026-02-01T10:00:00Z")),
            new Propiedad("prop-010", "Apartamento de lujo en Escazú",
                    "Penthouse de 3 habitaciones con terraza privada y vista panorámica.",
                    850000.0, "CRC", "San José", "Escazú", "Escazú centro",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1567496898669-ee935f5f647a?w=800"),
                    "usr-003", List.of("3 hab", "3 baños", "Terraza", "Vista"),
                    Instant.parse("2026-02-03T10:00:00Z")),
            new Propiedad("prop-011", "Casa de playa en Tamarindo",
                    "Casa frente al mar, 4 habitaciones, piscina, acceso privado a la playa.",
                    1500000.0, "CRC", "Guanacaste", "Santa Cruz", "Tamarindo",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=800"),
                    "usr-005", List.of("4 hab", "4 baños", "Piscina", "Playa privada"),
                    Instant.parse("2026-02-05T10:00:00Z")),
            new Propiedad("prop-012", "Loft moderno en Barrio Escalante",
                    "Loft de concepto abierto, 1 habitación, acabados de lujo, zona gastronómica.",
                    420000.0, "CRC", "San José", "San José", "Barrio Escalante",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1502672023488-70e25813eb80?w=800"),
                    "usr-001", List.of("1 hab", "1 baño", "Loft", "Gastronómico"),
                    Instant.parse("2026-02-08T10:00:00Z")),
            new Propiedad("prop-013", "Casa en condominio en Tres Ríos",
                    "Casa moderna en condominio con áreas verdes, seguridad 24/7, 3 habitaciones.",
                    580000.0, "CRC", "Cartago", "La Unión", "Tres Ríos",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=800"),
                    "usr-003", List.of("3 hab", "2 baños", "Condominio", "Seguridad"),
                    Instant.parse("2026-02-10T10:00:00Z")),
            new Propiedad("prop-014", "Local en Paseo Colón",
                    "Local comercial de 120m² en edificio corporativo, ideal para showroom.",
                    1100000.0, "CRC", "San José", "San José", "Paseo Colón",
                    "local", "disponible",
                    List.of("https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800"),
                    "usr-005", List.of("120m²", "2 baños", "Showroom", "Edificio"),
                    Instant.parse("2026-02-12T10:00:00Z")),
            new Propiedad("prop-015", "Apartamento económico en Guadalupe",
                    "Apartamento de 1 habitación, ideal para estudiante o profesional joven.",
                    220000.0, "CRC", "San José", "Goicoechea", "Guadalupe",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1486304873000-235643847519?w=800"),
                    "usr-001", List.of("1 hab", "1 baño", "Económico"),
                    Instant.parse("2026-02-15T10:00:00Z")),
            new Propiedad("prop-016", "Casa con terreno en Liberia",
                    "Casa de 3 habitaciones con terreno de 500m², zona rural tranquila.",
                    380000.0, "CRC", "Guanacaste", "Liberia", "Liberia centro",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800"),
                    "usr-003", List.of("3 hab", "2 baños", "Terreno", "Rural"),
                    Instant.parse("2026-02-18T10:00:00Z")),
            new Propiedad("prop-017", "Apartamento en Puerto Viejo",
                    "Cabaña/apartamento en zona tropical, cerca de playas del Caribe.",
                    350000.0, "CRC", "Limón", "Talamanca", "Puerto Viejo",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800"),
                    "usr-005", List.of("2 hab", "1 baño", "Tropical", "Playa"),
                    Instant.parse("2026-02-20T10:00:00Z")),
            new Propiedad("prop-018", "Oficina compartida en Heredia",
                    "Espacio de oficina compartida, ideal para startup, incluye internet y sala de reuniones.",
                    280000.0, "CRC", "Heredia", "Heredia", "Mercedes",
                    "oficina", "disponible",
                    List.of("https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=800"),
                    "usr-001", List.of("Coworking", "Internet", "Sala reuniones"),
                    Instant.parse("2026-02-22T10:00:00Z")),
            new Propiedad("prop-019", "Casa moderna en Pinares",
                    "Casa de diseño contemporáneo, 4 habitaciones, acabados de primera.",
                    890000.0, "CRC", "San José", "San José", "Pinares",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=800"),
                    "usr-003", List.of("4 hab", "3 baños", "Moderno", "Diseño"),
                    Instant.parse("2026-02-25T10:00:00Z")),
            new Propiedad("prop-020", "Apartamento en Curridabat",
                    "2 habitaciones en torre moderna con áreas comunes, cerca de la UCR.",
                    340000.0, "CRC", "San José", "Curridabat", "Curridabat centro",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1502005229762-cf1b2da7c5d6?w=800"),
                    "usr-005", List.of("2 hab", "2 baños", "Torre", "Áreas comunes"),
                    Instant.parse("2026-02-28T10:00:00Z")),
            new Propiedad("prop-021", "Casa de campo en San Carlos",
                    "Casa amplia en zona rural, 5 habitaciones, terreno de 1 hectárea, río cercano.",
                    750000.0, "CRC", "Alajuela", "San Carlos", "Ciudad Quesada",
                    "casa", "disponible",
                    List.of("https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=800"),
                    "usr-001", List.of("5 hab", "3 baños", "Campo", "Río"),
                    Instant.parse("2026-03-02T10:00:00Z")),
            new Propiedad("prop-022", "Local en Mall San Pedro",
                    "Local comercial en centro comercial de alto tráfico, totalmente equipado.",
                    980000.0, "CRC", "San José", "Montes de Oca", "San Pedro",
                    "local", "disponible",
                    List.of("https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?w=800"),
                    "usr-003", List.of("90m²", "Equipado", "Mall", "Alto tráfico"),
                    Instant.parse("2026-03-05T10:00:00Z")),
            new Propiedad("prop-023", "Apartamento con jardín en Sabana",
                    "Planta baja con jardín privado, 2 habitaciones, excelente ubicación.",
                    480000.0, "CRC", "San José", "San José", "La Sabana",
                    "apartamento", "disponible",
                    List.of("https://images.unsplash.com/photo-1502673530728-f79b4cab31b4?w=800"),
                    "usr-005", List.of("2 hab", "2 baños", "Jardín", "Planta baja"),
                    Instant.parse("2026-03-08T10:00:00Z")),
            new Propiedad("prop-024", "Bodega en Zona Franca Heredia",
                    "Bodega de 500m² en zona franca, ideal para distribución o manufactura.",
                    1800000.0, "CRC", "Heredia", "Heredia", "Zona Franca",
                    "bodega", "disponible",
                    List.of("https://images.unsplash.com/photo-1604328698692-f76ea9498e76?w=800"),
                    "usr-001", List.of("500m²", "Zona franca", "Distribución"),
                    Instant.parse("2026-03-10T10:00:00Z"))
    );

    @Override
    public ListadoPropiedades listar(FiltrosPropiedades filtros) {
        int page = Math.max(1, filtros.page());
        int size = Math.max(1, Math.min(100, filtros.size()));

        List<Propiedad> filtradas = DATA.stream()
                .filter(matchSearch(filtros.search()))
                .filter(matchProvincia(filtros.provincia()))
                .filter(matchTipo(filtros.tipo()))
                .filter(matchPrecio(filtros.precioMin(), filtros.precioMax()))
                .filter(matchDueno(filtros.duenoId()))
                .sorted(Comparator.comparing(Propiedad::createdAt).reversed())
                .toList();

        long total = filtradas.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int from = Math.min((page - 1) * size, filtradas.size());
        int to = Math.min(from + size, filtradas.size());
        List<Propiedad> page_data = filtradas.subList(from, to);

        return new ListadoPropiedades(page_data, total, page, totalPages);
    }

    @Override
    public Optional<Propiedad> porId(String id) {
        if (id == null) return Optional.empty();
        return DATA.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    private static java.util.function.Predicate<Propiedad> matchSearch(String q) {
        if (q == null || q.isBlank()) return p -> true;
        String needle = normalize(q);
        return p -> normalize(p.titulo()).contains(needle)
                || normalize(p.descripcion()).contains(needle)
                || normalize(p.provincia()).contains(needle)
                || normalize(p.canton()).contains(needle)
                || normalize(p.distrito()).contains(needle);
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private static java.util.function.Predicate<Propiedad> matchProvincia(String provincia) {
        if (provincia == null || provincia.isBlank()) return p -> true;
        return p -> p.provincia().equalsIgnoreCase(provincia);
    }

    private static java.util.function.Predicate<Propiedad> matchTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return p -> true;
        return p -> p.tipo().equalsIgnoreCase(tipo);
    }

    private static java.util.function.Predicate<Propiedad> matchPrecio(Double min, Double max) {
        if (min == null && max == null) return p -> true;
        return p -> {
            if (min != null && p.precio() < min) return false;
            return max == null || p.precio() <= max;
        };
    }

    private static java.util.function.Predicate<Propiedad> matchDueno(String duenoId) {
        if (duenoId == null || duenoId.isBlank()) return p -> true;
        return p -> p.duenoId().equals(duenoId);
    }
}
