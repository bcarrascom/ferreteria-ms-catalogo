package cl.duoc.ferreteria.catalogo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Datos generales de Swagger y el botón "Authorize" para pegar el access token. */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "ms-catalogo · Ferretería Los Maestros",
        version = "v1",
        description = "Catálogo de productos y categorías, y control de stock"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig { }
