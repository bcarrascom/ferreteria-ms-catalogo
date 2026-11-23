package cl.duoc.ferreteria.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Un producto y cuántas unidades descontar o reponer (API interna, la usa ms-ventas). */
public record ItemStock(
        @Schema(example = "MC001")
        @NotBlank(message = "El código es obligatorio")
        String codigo,

        @Schema(example = "2")
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe estar entre 1 y 999")
        @Max(value = 999, message = "La cantidad debe estar entre 1 y 999")
        Integer cantidad) { }
