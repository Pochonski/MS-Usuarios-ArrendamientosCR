package com.arrendamientos.usuarios.web;

import com.arrendamientos.usuarios.domain.model.Propiedad;
import com.arrendamientos.usuarios.domain.port.in.CatalogoPropiedadesUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropiedadesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogoPropiedadesUseCase catalogo;

    private static Propiedad sample(String id, String provincia, String tipo, double precio) {
        return new Propiedad(id, "Título " + id, "Desc " + id, precio, "CRC",
                provincia, "Cantón", "Distrito", tipo, "disponible",
                List.of("https://img/1.jpg"), "usr-001", List.of("Wifi"),
                Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Nested
    class Listar {

        @Test
        void devuelve200ConShapePaginada() throws Exception {
            when(catalogo.listar(any())).thenReturn(
                    new CatalogoPropiedadesUseCase.ListadoPropiedades(
                            List.of(sample("prop-1", "San José", "casa", 500000)),
                            1L, 1, 1
                    )
            );

            mockMvc.perform(get("/api/propiedades?page=1&limit=6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value("prop-1"))
                    .andExpect(jsonPath("$.data[0].titulo").value("Título prop-1"))
                    .andExpect(jsonPath("$.data[0].precio").value(500000))
                    .andExpect(jsonPath("$.data[0].moneda").value("CRC"))
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        void sinParametrosUsaDefaults1Y6() throws Exception {
            when(catalogo.listar(any())).thenReturn(
                    new CatalogoPropiedadesUseCase.ListadoPropiedades(List.of(), 0L, 1, 0)
            );

            mockMvc.perform(get("/api/propiedades"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.total").value(0));

            org.mockito.ArgumentCaptor<CatalogoPropiedadesUseCase.FiltrosPropiedades> captor =
                    org.mockito.ArgumentCaptor.forClass(CatalogoPropiedadesUseCase.FiltrosPropiedades.class);
            org.mockito.Mockito.verify(catalogo).listar(captor.capture());
            org.junit.jupiter.api.Assertions.assertEquals(1, captor.getValue().page());
            org.junit.jupiter.api.Assertions.assertEquals(6, captor.getValue().size());
        }

        @Test
        void propagaFiltrosAlUseCase() throws Exception {
            when(catalogo.listar(any())).thenReturn(
                    new CatalogoPropiedadesUseCase.ListadoPropiedades(List.of(), 0L, 1, 0)
            );

            mockMvc.perform(get("/api/propiedades")
                            .param("page", "2")
                            .param("limit", "12")
                            .param("search", "escazu")
                            .param("provincia", "San José")
                            .param("tipo", "casa")
                            .param("precioMin", "100000")
                            .param("precioMax", "900000")
                            .param("duenoId", "usr-001"))
                    .andExpect(status().isOk());

            org.mockito.ArgumentCaptor<CatalogoPropiedadesUseCase.FiltrosPropiedades> captor =
                    org.mockito.ArgumentCaptor.forClass(CatalogoPropiedadesUseCase.FiltrosPropiedades.class);
            org.mockito.Mockito.verify(catalogo).listar(captor.capture());
            var f = captor.getValue();
            org.junit.jupiter.api.Assertions.assertEquals(2, f.page());
            org.junit.jupiter.api.Assertions.assertEquals(12, f.size());
            org.junit.jupiter.api.Assertions.assertEquals("escazu", f.search());
            org.junit.jupiter.api.Assertions.assertEquals("San José", f.provincia());
            org.junit.jupiter.api.Assertions.assertEquals("casa", f.tipo());
            org.junit.jupiter.api.Assertions.assertEquals(100000.0, f.precioMin());
            org.junit.jupiter.api.Assertions.assertEquals(900000.0, f.precioMax());
            org.junit.jupiter.api.Assertions.assertEquals("usr-001", f.duenoId());
        }

        @Test
        void esPublico_SinTokenDevuelve200() throws Exception {
            when(catalogo.listar(any())).thenReturn(
                    new CatalogoPropiedadesUseCase.ListadoPropiedades(List.of(), 0L, 1, 0)
            );

            mockMvc.perform(get("/api/propiedades"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class PorId {

        @Test
        void existenteDevuelve200ConPropiedad() throws Exception {
            when(catalogo.porId("prop-1")).thenReturn(Optional.of(sample("prop-1", "Heredia", "apartamento", 350000)));

            mockMvc.perform(get("/api/propiedades/prop-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("prop-1"))
                    .andExpect(jsonPath("$.provincia").value("Heredia"))
                    .andExpect(jsonPath("$.tipo").value("apartamento"))
                    .andExpect(jsonPath("$.duenoId").value("usr-001"));
        }

        @Test
        void inexistenteDevuelve404() throws Exception {
            when(catalogo.porId("no-existe")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/propiedades/no-existe"))
                    .andExpect(status().isNotFound());
        }
    }
}
