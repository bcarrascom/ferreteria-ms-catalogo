package cl.duoc.ferreteria.catalogo.controller;

import cl.duoc.ferreteria.catalogo.dto.ProductoEntrada;
import cl.duoc.ferreteria.catalogo.dto.ProductoRespuesta;
import cl.duoc.ferreteria.catalogo.exception.ErrorRespuesta;
import cl.duoc.ferreteria.catalogo.security.UsuarioAutenticado;
import cl.duoc.ferreteria.catalogo.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Sin lógica: valida la entrada, decide si quien consulta es admin y llama al service. */
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Lectura pública (precioCompra solo para ADMINISTRADOR); crear, editar y eliminar solo ADMINISTRADOR")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @Operation(summary = "Lista los productos",
            description = "Público. Token opcional: con rol ADMINISTRADOR se incluye precioCompra. Filtros opcionales por categoría (id) y texto (nombre, marca o código).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de productos ordenada por código"),
            @ApiResponse(responseCode = "400", description = "categoria no es un número", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Se envió un token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping
    public List<ProductoRespuesta> listar(
            @Parameter(description = "Id de la categoría", example = "1") @RequestParam(required = false) Long categoria,
            @Parameter(description = "Texto a buscar en nombre, marca o código", example = "cemento") @RequestParam(required = false) String q,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return productoService.listar(categoria, q, esAdmin(usuario));
    }

    @Operation(summary = "Detalle de un producto",
            description = "Público. Token opcional: con rol ADMINISTRADOR se incluye precioCompra. El código no distingue mayúsculas.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "401", description = "Se envió un token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "No existe el producto", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/{codigo}")
    public ProductoRespuesta obtener(@Parameter(example = "MC001") @PathVariable String codigo,
                                     @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return productoService.obtener(codigo, esAdmin(usuario));
    }

    @Operation(summary = "Crea un producto", description = "Solo ADMINISTRADOR. El código se guarda en mayúsculas y no puede repetirse.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado; el header Location apunta a él"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Sin token, o token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "No existe la categoría", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un producto con ese código", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<ProductoRespuesta> crear(@Valid @RequestBody ProductoEntrada entrada) {
        ProductoRespuesta creado = productoService.crear(entrada);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.codigo())).body(creado);
    }

    @Operation(summary = "Actualiza un producto", description = "Solo ADMINISTRADOR. El código no se puede cambiar: el del cuerpo se ignora.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Sin token, o token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "No existe el producto o la categoría", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{codigo}")
    public ProductoRespuesta actualizar(@Parameter(example = "MC001") @PathVariable String codigo,
                                        @Valid @RequestBody ProductoEntrada entrada) {
        return productoService.actualizar(codigo, entrada);
    }

    @Operation(summary = "Elimina un producto", description = "Solo ADMINISTRADOR. Las boletas existentes no cambian: guardan su propia copia del nombre y del precio.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "401", description = "Sin token, o token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "No existe el producto", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> eliminar(@Parameter(example = "MC001") @PathVariable String codigo) {
        productoService.eliminar(codigo);
        return ResponseEntity.noContent().build();
    }

    /** Sin token llega null (anónimo). Solo el rol ADMINISTRADOR ve el precio de compra. */
    private static boolean esAdmin(UsuarioAutenticado usuario) {
        return usuario != null && "ADMINISTRADOR".equals(usuario.rol());
    }
}
