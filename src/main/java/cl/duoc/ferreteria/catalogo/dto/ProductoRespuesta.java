package cl.duoc.ferreteria.catalogo.dto;

import cl.duoc.ferreteria.catalogo.model.Producto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lo que devuelve la API para un producto (contrato, sección 6.5).
 * precioCompra solo se llena si quien consulta es ADMINISTRADOR; para los demás queda null
 * y @JsonInclude(NON_NULL) lo saca del JSON: el margen de la ferretería no es público.
 */
public record ProductoRespuesta(
        String codigo,
        String nombre,
        String categoria,
        Long categoriaId,
        String subcategoria,
        String marca,
        String unidad,
        Integer precioVenta,
        @Schema(description = "Solo aparece si el token es de ADMINISTRADOR")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer precioCompra,
        Integer stock,
        Integer stockMinimo,
        @Schema(description = "true si stock <= stockMinimo (alerta en la vista del vendedor)")
        boolean stockBajo) {

    /** La categoría debe venir cargada (JOIN FETCH o dentro de una transacción). */
    public static ProductoRespuesta desde(Producto p, boolean incluirPrecioCompra) {
        return new ProductoRespuesta(
                p.getCodigo(),
                p.getNombre(),
                p.getCategoria().getNombre(),
                p.getCategoria().getId(),
                p.getSubcategoria(),
                p.getMarca(),
                p.getUnidad(),
                p.getPrecioVenta(),
                incluirPrecioCompra ? p.getPrecioCompra() : null,
                p.getStock(),
                p.getStockMinimo(),
                p.getStock() <= p.getStockMinimo());
    }
}
