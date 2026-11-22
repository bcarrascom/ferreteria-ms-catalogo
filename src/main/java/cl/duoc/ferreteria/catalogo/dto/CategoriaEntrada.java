package cl.duoc.ferreteria.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST y PUT /api/v1/categorias. Las anotaciones se revisan con @Valid en el controller. */
public record CategoriaEntrada(

        @Schema(example = "Ferretería eléctrica")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre puede tener hasta 60 caracteres")
        String nombre,

        @Schema(example = "Cables, enchufes e interruptores")
        @Size(max = 255, message = "La descripción puede tener hasta 255 caracteres")
        String descripcion) { }
