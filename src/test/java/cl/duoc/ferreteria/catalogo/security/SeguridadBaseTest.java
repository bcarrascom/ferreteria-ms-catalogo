package cl.duoc.ferreteria.catalogo.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la cadena de seguridad completa (filtro JWT + reglas por rol + JSON de error).
 * Todavía no hay controllers: una ruta permitida sin controller responde 404, y eso
 * demuestra que la petición pasó la seguridad.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadBaseTest {

    private static final String PRODUCTOS = "/api/v1/productos";

    @Autowired
    private MockMvc mvc;

    @Test
    void swaggerEsPublico() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicoSinTokenPasaLaSeguridad() throws Exception {
        mvc.perform(get(PRODUCTOS))
                .andExpect(status().isNotFound())                  // pasó la seguridad; aún no hay controller
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.ruta").value(PRODUCTOS));
    }

    @Test
    void escribirSinTokenDa401() throws Exception {
        mvc.perform(post(PRODUCTOS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión"))
                .andExpect(jsonPath("$.ruta").value(PRODUCTOS))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void escribirConRolClienteDa403() throws Exception {
        mvc.perform(post(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.access("CLIENTE")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tienes permiso para esta acción"));
    }

    @Test
    void escribirConRolVendedorDa403() throws Exception {
        mvc.perform(post(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.access("VENDEDOR")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void escribirConRolAdministradorPasaLaSeguridad() throws Exception {
        mvc.perform(post(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.access("ADMINISTRADOR")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is(not(oneOf(401, 403))));     // la seguridad lo deja pasar
    }

    @Test
    void tokenExpiradoDa401AunqueElGetSeaPublico() throws Exception {
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.expirado("ADMINISTRADOR"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token inválido o expirado"));
    }

    @Test
    void refreshTokenNoSirveComoAccess() throws Exception {
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.refresh())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtraClaveDa401() throws Exception {
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.firmadoConOtraClave("ADMINISTRADOR"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOtroEmisorDa401() throws Exception {
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.deOtroEmisor("ADMINISTRADOR"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenMalFormadoDa401() throws Exception {
        mvc.perform(get(PRODUCTOS).header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaNoListadaSeRechaza() throws Exception {
        mvc.perform(get("/api/v1/cualquier-cosa"))
                .andExpect(status().isUnauthorized());             // denyAll: sin token pide iniciar sesión
        mvc.perform(get("/api/v1/cualquier-cosa").header(HttpHeaders.AUTHORIZATION, bearer(TokenDePrueba.access("ADMINISTRADOR"))))
                .andExpect(status().isForbidden());                // denyAll: ni el admin entra
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
