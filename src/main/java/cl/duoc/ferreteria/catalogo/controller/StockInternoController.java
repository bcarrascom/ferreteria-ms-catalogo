package cl.duoc.ferreteria.catalogo.controller;

import cl.duoc.ferreteria.catalogo.dto.StockDescuentoRespuesta;
import cl.duoc.ferreteria.catalogo.dto.StockSolicitud;
import cl.duoc.ferreteria.catalogo.exception.ErrorRespuesta;
import cl.duoc.ferreteria.catalogo.security.ClaveInternaValidador;
import cl.duoc.ferreteria.catalogo.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API interna de stock. Solo la llama ms-ventas: nginx no enruta /api/v1/interno.
 * La clave X-API-KEY la revisa ClaveInternaValidador antes de llegar aquí.
 */
@RestController
@RequestMapping("/api/v1/interno/stock")
@Tag(name = "Interno · Stock", description = "Solo para ms-ventas. No está expuesto por nginx. Requiere el header X-API-KEY")
public class StockInternoController {

    private final StockService stockService;

    public StockInternoController(StockService stockService) {
        this.stockService = stockService;
    }

    @Operation(summary = "Valida y descuenta stock de varios productos (todo o nada)",
            description = "Solo para ms-ventas. No está expuesto por nginx. Los códigos repetidos se suman. "
                    + "Si un producto no existe o no alcanza, no se descuenta ninguno. "
                    + "Devuelve el nombre y el precio de venta actual de cada ítem, ordenados por código.")
    @Parameter(in = ParameterIn.HEADER, name = ClaveInternaValidador.HEADER, required = true,
            description = "Clave interna compartida con ms-ventas (INTERNAL_API_KEY)", schema = @Schema(type = "string"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock descontado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-API-KEY o es incorrecta", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "Un producto no existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/descuento")
    public StockDescuentoRespuesta descontar(@Valid @RequestBody StockSolicitud solicitud) {
        return new StockDescuentoRespuesta(stockService.descontar(solicitud.items()));
    }

    @Operation(summary = "Devuelve stock (compensación o anulación de una boleta)",
            description = "Solo para ms-ventas. No está expuesto por nginx. Los códigos que ya no existen se omiten.")
    @Parameter(in = ParameterIn.HEADER, name = ClaveInternaValidador.HEADER, required = true,
            description = "Clave interna compartida con ms-ventas (INTERNAL_API_KEY)", schema = @Schema(type = "string"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Stock repuesto"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Falta X-API-KEY o es incorrecta", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/reposicion")
    public ResponseEntity<Void> reponer(@Valid @RequestBody StockSolicitud solicitud) {
        stockService.reponer(solicitud.items());
        return ResponseEntity.noContent().build();
    }
}
