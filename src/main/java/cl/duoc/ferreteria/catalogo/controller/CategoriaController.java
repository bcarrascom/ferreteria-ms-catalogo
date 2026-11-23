package cl.duoc.ferreteria.catalogo.controller;

import cl.duoc.ferreteria.catalogo.dto.CategoriaEntrada;
import cl.duoc.ferreteria.catalogo.dto.CategoriaRespuesta;
import cl.duoc.ferreteria.catalogo.exception.ErrorRespuesta;
import cl.duoc.ferreteria.catalogo.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Sin lógica: valida la entrada (@Valid), llama al service y arma la respuesta HTTP. */
@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Lectura pública; crear, editar y eliminar solo ADMINISTRADOR")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Operation(summary = "Lista las categorías", description = "Ordenadas por id, con la cantidad de productos de cada una.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de categorías"),
            @ApiResponse(responseCode = "401", description = "Se envió un token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping
    public List<CategoriaRespuesta> listar() {
        return categoriaService.listar();
    }

    @Operation(summary = "Detalle de una categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "400", description = "El id no es un número", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Se envió un token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "No existe la categoría", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/{id}")
    public CategoriaRespuesta obtener(@PathVariable Long id) {
        return categoriaService.obtener(id);
    }

    @Operation(summary = "Crea una categoría", description = "Solo ADMINISTRADOR. El nombre no puede repetirse (sin importar mayúsculas).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada; el header Location apunta a ella"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Sin token, o token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<CategoriaRespuesta> crear(@Valid @RequestBody CategoriaEntrada entrada) {
        CategoriaRespuesta creada = categoriaService.crear(entrada);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + creada.id())).body(creada);
    }

    @Operation(summary = "Actualiza una categoría", description = "Solo ADMINISTRADOR.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "401", description = "Sin token, o token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "No existe la categoría", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", description = "Otra categoría ya usa ese nombre", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    public CategoriaRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaEntrada entrada) {
        return categoriaService.actualizar(id, entrada);
    }

    @Operation(summary = "Elimina una categoría", description = "Solo ADMINISTRADOR. Solo si no tiene productos.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoría eliminada"),
            @ApiResponse(responseCode = "401", description = "Sin token, o token inválido o expirado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", description = "No existe la categoría", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", description = "La categoría tiene productos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
