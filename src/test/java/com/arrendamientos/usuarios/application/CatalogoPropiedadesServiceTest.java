package com.arrendamientos.usuarios.application;

import com.arrendamientos.usuarios.application.service.CatalogoPropiedadesService;
import com.arrendamientos.usuarios.domain.port.in.CatalogoPropiedadesUseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoPropiedadesServiceTest {

    private final CatalogoPropiedadesService service = new CatalogoPropiedadesService();

    private static CatalogoPropiedadesUseCase.FiltrosPropiedades filtros(int page, int size) {
        return new CatalogoPropiedadesUseCase.FiltrosPropiedades(
                null, null, null, null, null, null, page, size);
    }

    @Test
    void listarSinFiltrosDevuelveTodasLasPropiedadesPaginadas() {
        var r = service.listar(filtros(1, 10));
        assertEquals(24, r.total(), "dataset mock debe tener 24 propiedades");
        assertEquals(3, r.totalPages());
        assertEquals(10, r.data().size());
    }

    @Test
    void listarPagina2DevuelveSiguienteSet() {
        var r = service.listar(filtros(2, 10));
        assertEquals(24, r.total());
        assertEquals(2, r.page());
        assertEquals(10, r.data().size());
    }

    @Test
    void listarPaginaInexistenteDevuelveVacio() {
        var r = service.listar(filtros(99, 10));
        assertEquals(0, r.data().size());
        assertEquals(24, r.total());
    }

    @Test
    void filtroPorProvincia() {
        var r = service.listar(new CatalogoPropiedadesUseCase.FiltrosPropiedades(
                null, "Guanacaste", null, null, null, null, 1, 100));
        assertTrue(r.total() >= 1);
        assertTrue(r.data().stream().allMatch(p -> p.provincia().equals("Guanacaste")));
    }

    @Test
    void filtroPorTipo() {
        var r = service.listar(new CatalogoPropiedadesUseCase.FiltrosPropiedades(
                null, null, "casa", null, null, null, 1, 100));
        assertTrue(r.total() >= 1);
        assertTrue(r.data().stream().allMatch(p -> p.tipo().equals("casa")));
    }

    @Test
    void filtroPorRangoDePrecio() {
        var r = service.listar(new CatalogoPropiedadesUseCase.FiltrosPropiedades(
                null, null, null, 400000.0, 600000.0, null, 1, 100));
        assertTrue(r.total() >= 1);
        assertTrue(r.data().stream().allMatch(p -> p.precio() >= 400000 && p.precio() <= 600000));
    }

    @Test
    void filtroPorDueno() {
        var r = service.listar(new CatalogoPropiedadesUseCase.FiltrosPropiedades(
                null, null, null, null, null, "usr-001", 1, 100));
        assertTrue(r.total() >= 1);
        assertTrue(r.data().stream().allMatch(p -> p.duenoId().equals("usr-001")));
    }

    @Test
    void busquedaPorTextoCoincideEnTituloODescripcionOUbicacion() {
        var r = service.listar(new CatalogoPropiedadesUseCase.FiltrosPropiedades(
                "escazu", null, null, null, null, null, 1, 100));
        assertTrue(r.total() >= 1);
        assertTrue(r.data().stream().anyMatch(p -> p.titulo().toLowerCase().contains("escazú")
                || p.provincia().toLowerCase().contains("escazú")
                || p.canton().toLowerCase().contains("escazú")
                || p.distrito().toLowerCase().contains("escazú")));
    }

    @Test
    void porIdExistenteDevuelvePropiedad() {
        var opt = service.porId("prop-001");
        assertTrue(opt.isPresent());
        assertEquals("prop-001", opt.get().id());
    }

    @Test
    void porIdInexistenteDevuelveVacio() {
        assertTrue(service.porId("no-existe").isEmpty());
    }

    @Test
    void porIdNullDevuelveVacio() {
        assertTrue(service.porId(null).isEmpty());
    }

    @Test
    void limiteSizeSeAcotaA100() {
        var r = service.listar(filtros(1, 9999));
        assertEquals(24, r.data().size(), "dataset tiene 24 elementos, capped a 100 por el service");
    }

    @Test
    void sizeMinimoEs1() {
        var r = service.listar(filtros(1, 0));
        assertEquals(1, r.data().size(), "size=0 debe normalizarse a 1");
    }

    @Test
    void pageMinimoEs1() {
        var r = service.listar(filtros(-5, 10));
        assertEquals(1, r.page(), "page=-5 debe normalizarse a 1");
        assertEquals(10, r.data().size());
    }
}
