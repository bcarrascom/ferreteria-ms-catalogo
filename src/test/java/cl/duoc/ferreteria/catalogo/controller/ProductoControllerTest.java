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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración por HTTP con los 85 productos de productos.json.
 * @Transactional revierte al final de cada test lo que se creó, editó o eliminó.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductoControllerTest {

    private static final String URL = "/api/v1/productos";

    /** Cuerpo válido de ProductoEntrada; categoriaId 1 = Mat. Construcción. */
    private static final String NUEVO = """
            {"codigo": "mc011", "nombre": "Cemento especial 25 kg", "categoriaId": 1,
             "subcategoria": "Cementos", "marca": "Polpaico", "unidad": "Saco",
             "precioVenta": 6490, "precioCompra": 3500, "stock": 40, "stockMinimo": 10}""";

    @Autowired
    private MockMvc mvc;

    // ── precioCompra solo para ADMINISTRADOR ──

    @Test
    void sinTokenListaLos85ProductosSinPrecioDeCompra() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(85)))
                .andExpect(jsonPath("$[0].codigo").value("EL001"))           // ordenados por código
                .andExpect(jsonPath("$[*].precioCompra").isEmpty())          // el campo no existe en ninguno
                .andExpect(jsonPath("$[0].precioVenta").exists());
    }

    @Test
    void conTokenDeAdministradorApareceElPrecioDeCompra() throws Exception {
        mvc.perform(get(URL + "/MC001").header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioCompra").value(3200));
    }

    /** not(hasKey): la clave no debe estar en el JSON. doesNotExist() aceptaría "precioCompra": null. */
    @Test
    void sinTokenOConTokenDeVendedorOClienteLaClavePrecioCompraNoEstaEnElJson() throws Exception {
        mvc.perform(get(URL + "/MC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasKey("precioCompra"))));
        mvc.perform(get(URL + "/MC001").header(HttpHeaders.AUTHORIZATION, bearer("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasKey("precioCompra"))));
        mvc.perform(get(URL + "/MC001").header(HttpHeaders.AUTHORIZATION, bearer("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasKey("precioCompra"))));
    }

    // ── Detalle y filtros ──

    @Test
    void detalleTieneTodosLosCamposDelContrato() throws Exception {
        mvc.perform(get(URL + "/MC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("MC001"))
                .andExpect(jsonPath("$.nombre").value("Cemento Polpaico gris 25 kg"))
                .andExpect(jsonPath("$.categoria").value("Mat. Construcción"))
                .andExpect(jsonPath("$.categoriaId").value(1))
                .andExpect(jsonPath("$.subcategoria").value("Cementos"))
                .andExpect(jsonPath("$.marca").value("Polpaico"))
                .andExpect(jsonPath("$.unidad").value("Saco"))
                .andExpect(jsonPath("$.precioVenta").value(5990))
                .andExpect(jsonPath("$.stock").value(80))
                .andExpect(jsonPath("$.stockMinimo").value(20))
                .andExpect(jsonPath("$.stockBajo").value(false));
    }

    @Test
    void elCodigoNoDistingueMayusculas() throws Exception {
        mvc.perform(get(URL + "/mc001")).andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("MC001"));
    }

    @Test
    void productoInexistenteDa404() throws Exception {
        mvc.perform(get(URL + "/XX999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe el producto XX999"));
    }

    @Test
    void filtrarPorCategoria() throws Exception {
        mvc.perform(get(URL).param("categoria", "2"))                       // 2 = Pinturas
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[*].categoria", everyItem(is("Pinturas"))));
    }

    @Test
    void buscarPorTexto() throws Exception {
        mvc.perform(get(URL).param("q", "polpaico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].marca", everyItem(is("Polpaico"))));
    }

    @Test
    void categoriaQueNoEsNumeroDa400() throws Exception {
        mvc.perform(get(URL).param("categoria", "pinturas")).andExpect(status().isBadRequest());
    }

    // ── Crear ──

    @Test
    void crearComoAdministradorDa201ConElCodigoEnMayusculas() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content(NUEVO))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, URL + "/MC011"))
                .andExpect(jsonPath("$.codigo").value("MC011"))
                .andExpect(jsonPath("$.categoria").value("Mat. Construcción"))
                .andExpect(jsonPath("$.precioCompra").value(3500));
        mvc.perform(get(URL + "/MC011")).andExpect(status().isOk());
    }

    @Test
    void crearConCodigoExistenteDa409() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content(NUEVO.replace("mc011", "MC001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un producto con código MC001"));
    }

    @Test
    void crearConCategoriaInexistenteDa404() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content(NUEVO.replace("\"categoriaId\": 1", "\"categoriaId\": 999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la categoría 999"));
    }

    @Test
    void crearConPrecioDeVentaCeroDa400ConDetalle() throws Exception {
        mvc.perform(comoAdmin(post(URL)).content(NUEVO.replace("\"precioVenta\": 6490", "\"precioVenta\": 0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.precioVenta").value("El precio de venta debe ser mayor a 0"));
    }

    @Test
    void crearComoVendedorDa403() throws Exception {
        mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, bearer("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearSinTokenDa401() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isUnauthorized());
    }

    // ── Actualizar ──

    @Test
    void actualizarCambiaLosDatosPeroNoElCodigo() throws Exception {
        mvc.perform(comoAdmin(put(URL + "/MC001")).content(NUEVO))      // el cuerpo trae "mc011": se ignora
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("MC001"))
                .andExpect(jsonPath("$.nombre").value("Cemento especial 25 kg"))
                .andExpect(jsonPath("$.precioVenta").value(6490));
        mvc.perform(get(URL + "/MC011")).andExpect(status().isNotFound());
    }

    @Test
    void stockBajoSeEnciendeAlLlegarAlMinimo() throws Exception {
        mvc.perform(comoAdmin(put(URL + "/MC001")).content(NUEVO.replace("\"stock\": 40", "\"stock\": 10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.stockMinimo").value(10))
                .andExpect(jsonPath("$.stockBajo").value(true));
    }

    @Test
    void actualizarInexistenteDa404() throws Exception {
        mvc.perform(comoAdmin(put(URL + "/XX999")).content(NUEVO)).andExpect(status().isNotFound());
    }

    // ── Eliminar ──

    @Test
    void eliminarComoAdministradorDa204YLuegoNoExiste() throws Exception {
        mvc.perform(comoAdmin(delete(URL + "/MC001"))).andExpect(status().isNoContent());
        mvc.perform(get(URL + "/MC001")).andExpect(status().isNotFound());
    }

    @Test
    void eliminarInexistenteDa404() throws Exception {
        mvc.perform(comoAdmin(delete(URL + "/XX999"))).andExpect(status().isNotFound());
    }

    @Test
    void eliminarComoClienteDa403() throws Exception {
        mvc.perform(delete(URL + "/MC001").header(HttpHeaders.AUTHORIZATION, bearer("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    private static String bearer(String rol) {
        return "Bearer " + TokenDePrueba.access(rol);
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder peticion) {
        return peticion.header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON);
    }
}
