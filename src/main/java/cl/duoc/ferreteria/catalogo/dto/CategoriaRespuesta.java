package cl.duoc.ferreteria.catalogo.dto;

import cl.duoc.ferreteria.catalogo.model.Categoria;

/**
 * Lo que devuelve la API para una categoría. cantidadProductos sirve en la vista de administración
 * y explica por qué una categoría no se puede eliminar (aclaración C9.5 de las especificaciones).
 */
public record CategoriaRespuesta(Long id, String nombre, String descripcion, long cantidadProductos) {

    public static CategoriaRespuesta desde(Categoria categoria, long cantidadProductos) {
        return new CategoriaRespuesta(categoria.getId(), categoria.getNombre(),
                categoria.getDescripcion(), cantidadProductos);
    }
}
