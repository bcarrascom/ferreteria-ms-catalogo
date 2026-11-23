package cl.duoc.ferreteria.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST y PUT /api/v1/productos. La regla 2 del contrato (precio de venta > 0,
 * precio de compra, stock y stock mínimo >= 0) vive en estas anotaciones.
 * En PUT el código del cuerpo se ignora: el código de un producto no se puede cambiar.
 */
public record ProductoEntrada(

        @Schema(example = "MC011", description = "Se guarda en mayúsculas. En PUT se ignora")
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 10, message = "El código puede tener hasta 10 caracteres")
        @Pattern(regexp = "^\\s*[A-Za-z0-9]+\\s*$", message = "El código solo puede tener letras y números")
        String codigo,

        @Schema(example = "Cemento especial 25 kg")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
        String nombre,

        @Schema(example = "1")
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @Schema(example = "Cementos")
        @Size(max = 60, message = "La subcategoría puede tener hasta 60 caracteres")
        String subcategoria,

        @Schema(example = "Polpaico")
        @Size(max = 60, message = "La marca puede tener hasta 60 caracteres")
        String marca,

        @Schema(example = "Saco")
        @Size(max = 30, message = "La unidad puede tener hasta 30 caracteres")
        String unidad,

        @Schema(example = "6490")
        @NotNull(message = "El precio de venta es obligatorio")
        @Positive(message = "El precio de venta debe ser mayor a 0")
        Integer precioVenta,

        @Schema(example = "3500")
        @NotNull(message = "El precio de compra es obligatorio")
        @PositiveOrZero(message = "El precio de compra no puede ser negativo")
        Integer precioCompra,

        @Schema(example = "40")
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @Schema(example = "10")
        @NotNull(message = "El stock mínimo es obligatorio")
        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo) { }
