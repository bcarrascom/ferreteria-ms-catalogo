package cl.duoc.ferreteria.catalogo.controller;

import cl.duoc.ferreteria.catalogo.security.TokenDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración: aplicación completa sobre H2 con los datos de productos.json.
 * Cada petición pasa por el filtro JWT, las reglas de seguridad, el controller, el service y la base.
 * @Transactional revierte al final de cada test lo que se creó, editó o eliminó.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoriaControllerTest {

    private static final String URL = "/api/v1/categorias";

    @Autowired
    private MockMvc mvc;

    // ── Lectura pública ──

    @Test
    void listarEsPublicoYDevuelveLas9ConSuCantidadDeProductos() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[0].nombre").value("Mat. Construcción"))
                .andExpect(jsonPath("$[0].cantidadProductos").value(10));
    }

    @Test
    void obtenerExistente() throws Exception {
        mvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Mat. Construcción"));
    }

    @Test
    void obtenerInexistenteDa404ConMensaje() throws Exception {
        mvc.perform(get(URL + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la categoría 999"))
                .andExpect(jsonPath("$.ruta").value(URL + "/999"));
    }

    @Test
    void idQueNoEsNumeroDa400() throws Exception {
        mvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La solicitud está mal formada"));
    }

    // ── Crear ──

    @Test
    void crearComoAdministradorDa201ConLocation() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content("""
                        {"nombre": "Aseo", "descripcion": "Productos de limpieza"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(URL + "/\\d+")))
                .andExpect(jsonPath("$.nombre").value("Aseo"))
                .andExpect(jsonPath("$.cantidadProductos").value(0));
    }

    @Test
    void crearSinNombreDa400ConDetallePorCampo() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content("""
                        {"nombre": "  "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void crearConNombreRepetidoEnMinusculasDa409() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content("""
                        {"nombre": "pinturas"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe la categoría pinturas"));
    }

    @Test
    void crearConJsonRotoDa400() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content("{ esto no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearSinTokenDa401() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre": "Aseo"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearComoVendedorDa403() throws Exception {
        mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre": "Aseo"}"""))
                .andExpect(status().isForbidden());
    }

    // ── Actualizar ──

    @Test
    void actualizarComoAdministradorDa200() throws Exception {
        mvc.perform(comoAdmin(put(URL + "/2")).content("""
                        {"nombre": "Pinturas y barnices", "descripcion": "Látex, esmaltes y barnices"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pinturas y barnices"))
                .andExpect(jsonPath("$.cantidadProductos").value(9));
    }

    @Test
    void actualizarConElNombreDeOtraDa409() throws Exception {
        mvc.perform(comoAdmin(put(URL + "/2")).content("""
                        {"nombre": "Madera"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarInexistenteDa404() throws Exception {
        mvc.perform(comoAdmin(put(URL + "/999")).content("""
                        {"nombre": "Aseo"}"""))
                .andExpect(status().isNotFound());
    }

    // ── Eliminar ──

    @Test
    void eliminarCategoriaConProductosDa409() throws Exception {
        mvc.perform(comoAdmin(delete(URL + "/9")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede eliminar la categoría Jardín: tiene 5 productos"));
    }

    @Test
    void eliminarCategoriaVaciaDa204YLuegoNoExiste() throws Exception {
        String location = mvc.perform(comoAdmin(post(URL)).content("""
                        {"nombre": "Temporal"}"""))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mvc.perform(comoAdmin(delete(location))).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void eliminarComoClienteDa403() throws Exception {
        mvc.perform(delete(URL + "/9").header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON);
    }
}
