package cl.duoc.ferreteria.catalogo.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Swagger cumple el contrato (CLAUDE.md §6.5). IE3.2.1 baja a 60% si un endpoint no aparece en Swagger
 * con sus códigos: este test falla si alguien borra un endpoint o un @ApiResponse.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContratoTest {

    @Autowired
    private MockMvc mvc;

    private String docs;

    /** protección: bearer = pide el token (botón Authorize); clave = header X-API-KEY; publico = nada. */
    @ParameterizedTest(name = "{0} {1} documenta {2}")
    @CsvSource(delimiter = '|', textBlock = """
            get    | /api/v1/categorias                | 200                     | publico
            get    | /api/v1/categorias/{id}           | 200 404                 | publico
            post   | /api/v1/categorias                | 201 400 401 403 409     | bearer
            put    | /api/v1/categorias/{id}           | 200 400 401 403 404 409 | bearer
            delete | /api/v1/categorias/{id}           | 204 401 403 404 409     | bearer
            get    | /api/v1/productos                 | 200                     | bearer
            get    | /api/v1/productos/{codigo}        | 200 404                 | bearer
            post   | /api/v1/productos                 | 201 400 401 403 404 409 | bearer
            put    | /api/v1/productos/{codigo}        | 200 400 401 403 404     | bearer
            delete | /api/v1/productos/{codigo}        | 204 401 403 404         | bearer
            post   | /api/v1/interno/stock/descuento   | 200 400 401 404 409     | clave
            post   | /api/v1/interno/stock/reposicion  | 204 400 401             | clave
            """)
    void cadaEndpointDocumentaLosCodigosDelContrato(String metodo, String ruta, String codigos, String proteccion)
            throws Exception {
        String operacion = "$.paths['" + ruta + "']." + metodo;

        Map<String, Object> respuestas = JsonPath.read(docs(), operacion + ".responses");
        assertThat(respuestas).containsKeys(codigos.split(" "));

        if (proteccion.equals("bearer")) {
            List<Map<String, Object>> seguridad = JsonPath.read(docs(), operacion + ".security");
            assertThat(seguridad).anySatisfy(requisito -> assertThat(requisito).containsKey("bearerAuth"));
        }
        if (proteccion.equals("clave")) {
            List<String> headers = JsonPath.read(docs(), operacion + ".parameters[?(@.in == 'header')].name");
            assertThat(headers).contains("X-API-KEY");
        }
    }

    @Test
    void swaggerTieneExactamenteLos12EndpointsDelContrato() throws Exception {
        List<Object> operaciones = JsonPath.read(docs(), "$.paths.*.*");
        assertThat(operaciones).hasSize(12);
    }

    @Test
    void lasRespuestasSeDocumentanComoJson() throws Exception {
        Map<String, Object> contenido = JsonPath.read(docs(), "$.paths['/api/v1/productos'].get.responses.200.content");
        assertThat(contenido).containsOnlyKeys("application/json");
    }

    private String docs() throws Exception {
        if (docs == null) {
            docs = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        }
        return docs;
    }
}
