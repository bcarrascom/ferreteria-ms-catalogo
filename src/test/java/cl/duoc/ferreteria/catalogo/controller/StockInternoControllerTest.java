package cl.duoc.ferreteria.catalogo.controller;

import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import cl.duoc.ferreteria.catalogo.security.TokenDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de la API interna de stock contra H2, con los 85 productos de productos.json
 * (EL004 tiene stock 50, HE004 stock 4, MC001 stock 80).
 * El stock se lee con obtenerStock, que consulta la base directamente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockInternoControllerTest {

    private static final String DESCUENTO = "/api/v1/interno/stock/descuento";
    private static final String REPOSICION = "/api/v1/interno/stock/reposicion";
    private static final String CLAVE = "clave-interna-de-prueba";      // la de src/test/resources

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductoRepository productoRepository;

    // ── Descuento ──

    @Test
    void descuentoDevuelvePreciosOrdenadosPorCodigoYBajaElStock() throws Exception {
        int antes = stock("MC001");

        mvc.perform(conClave(post(DESCUENTO)).content("""
                        {"items": [{"codigo": "MC001", "cantidad": 2}, {"codigo": "el004", "cantidad": 1}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].codigo").value("EL004"))
                .andExpect(jsonPath("$.items[0].nombre").value("Enchufe empotrar 16A c/tierra (schuko)"))
                .andExpect(jsonPath("$.items[0].precioUnitario").value(3690))
                .andExpect(jsonPath("$.items[0].cantidad").value(1))
                .andExpect(jsonPath("$.items[1].codigo").value("MC001"))
                .andExpect(jsonPath("$.items[1].precioUnitario").value(5990))
                .andExpect(jsonPath("$.items[1].cantidad").value(2));

        assertThat(stock("MC001")).isEqualTo(antes - 2);
    }

    @Test
    void stockInsuficienteDa409ConElMensajeQuePropagaMsVentas() throws Exception {
        mvc.perform(conClave(post(DESCUENTO)).content("""
                        {"items": [{"codigo": "HE004", "cantidad": 5}]}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente para HE004: solicitado 5, disponible 4"));
    }

    @Test
    void productoInexistenteDa404() throws Exception {
        mvc.perform(conClave(post(DESCUENTO)).content("""
                        {"items": [{"codigo": "XX999", "cantidad": 1}]}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe el producto XX999"));
    }

    /**
     * Todo o nada. Sin la transacción del test (NOT_SUPPORTED), así el rollback de StockService es real:
     * EL004 se descuenta primero (va antes por orden de código), HE004 no alcanza, y EL004 debe volver
     * a su stock. Si todo funciona, este test no deja cambios en la base.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void siUnItemNoAlcanzaNoSeDescuentaNinguno() throws Exception {
        int el004Antes = stock("EL004");
        int he004Antes = stock("HE004");

        mvc.perform(conClave(post(DESCUENTO)).content("""
                        {"items": [{"codigo": "EL004", "cantidad": 1}, {"codigo": "HE004", "cantidad": 999}]}"""))
                .andExpect(status().isConflict());

        assertThat(stock("EL004")).isEqualTo(el004Antes);
        assertThat(stock("HE004")).isEqualTo(he004Antes);
    }

    // ── Validación ──

    @Test
    void sinItemsDa400() throws Exception {
        mvc.perform(conClave(post(DESCUENTO)).content("{\"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.items").value("Debe venir al menos un ítem"));
    }

    @Test
    void cantidadFueraDeRangoDa400ConElCampoDelItem() throws Exception {
        mvc.perform(conClave(post(DESCUENTO)).content("""
                        {"items": [{"codigo": "MC001", "cantidad": 0}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles['items[0].cantidad']").value("La cantidad debe estar entre 1 y 999"));
    }

    // ── Clave interna ──

    @Test
    void sinClaveDa401() throws Exception {
        mvc.perform(post(DESCUENTO).contentType(MediaType.APPLICATION_JSON).content("""
                        {"items": [{"codigo": "MC001", "cantidad": 1}]}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Clave interna inválida"))
                .andExpect(jsonPath("$.ruta").value(DESCUENTO));
    }

    @Test
    void claveIncorrectaDa401() throws Exception {
        mvc.perform(post(REPOSICION).header("X-API-KEY", "otra-clave")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"items": [{"codigo": "MC001", "cantidad": 1}]}"""))
                .andExpect(status().isUnauthorized());
    }

    /** La clave se revisa antes de leer el cuerpo: sin clave no se aprende nada de la API, ni siquiera un 400. */
    @Test
    void sinClaveYConCuerpoInvalidoIgualDa401() throws Exception {
        mvc.perform(post(DESCUENTO).contentType(MediaType.APPLICATION_JSON).content("{esto no es json"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeAdministradorNoReemplazaLaClave() throws Exception {
        mvc.perform(post(DESCUENTO).header(HttpHeaders.AUTHORIZATION, "Bearer " + TokenDePrueba.access("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"items": [{"codigo": "MC001", "cantidad": 1}]}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Clave interna inválida"));
    }

    // ── Reposición ──

    @Test
    void reposicionDa204YSubeElStock() throws Exception {
        int antes = stock("MC001");

        mvc.perform(conClave(post(REPOSICION)).content("""
                        {"items": [{"codigo": "mc001", "cantidad": 3}]}"""))
                .andExpect(status().isNoContent());

        assertThat(stock("MC001")).isEqualTo(antes + 3);
    }

    @Test
    void reposicionDeUnProductoQueYaNoExisteDa204() throws Exception {
        mvc.perform(conClave(post(REPOSICION)).content("""
                        {"items": [{"codigo": "XX999", "cantidad": 1}]}"""))
                .andExpect(status().isNoContent());
    }

    // ── Swagger ──

    @Test
    void swaggerDocumentaElHeaderXApiKey() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/interno/stock/descuento'].post.parameters[*].name", hasItem("X-API-KEY")))
                .andExpect(jsonPath("$.paths['/api/v1/interno/stock/reposicion'].post.parameters[*].name", hasItem("X-API-KEY")));
    }

    private int stock(String codigo) {
        return productoRepository.obtenerStock(codigo).orElseThrow();
    }

    private static MockHttpServletRequestBuilder conClave(MockHttpServletRequestBuilder peticion) {
        return peticion.header("X-API-KEY", CLAVE).contentType(MediaType.APPLICATION_JSON);
    }
}
