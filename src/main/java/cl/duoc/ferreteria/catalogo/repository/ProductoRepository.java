package cl.duoc.ferreteria.catalogo.repository;

import cl.duoc.ferreteria.catalogo.model.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Busca por código y trae la categoría en la misma consulta (evita la carga perezosa fuera de transacción). */
    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    /** Para no eliminar una categoría que tiene productos (regla 4). */
    long countByCategoriaId(Long categoriaId);

    /**
     * Lista con filtros opcionales: si un parámetro es null, ese filtro no se aplica.
     * q busca en nombre, marca y código sin importar mayúsculas. JOIN FETCH trae la categoría junto.
     */
    @Query("""
           SELECT p FROM Producto p JOIN FETCH p.categoria c
           WHERE (:categoriaId IS NULL OR c.id = :categoriaId)
             AND (:q IS NULL
                  OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(p.marca)  LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY p.codigo
           """)
    List<Producto> buscar(@Param("categoriaId") Long categoriaId, @Param("q") String q);

    /**
     * Descuento atómico (regla 5): la base de datos revisa y resta en una sola operación.
     * Devuelve 1 si descontó y 0 si no alcanzaba. Dos compras simultáneas de la última unidad:
     * la segunda actualiza 0 filas, en vez de dejar el stock negativo.
     */
    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.codigo = :codigo AND p.stock >= :cantidad")
    int descontarStock(@Param("codigo") String codigo, @Param("cantidad") int cantidad);

    /** Devuelve stock (compensación o anulación de una boleta). 0 si el producto ya no existe. */
    @Modifying
    @Query("UPDATE Producto p SET p.stock = p.stock + :cantidad WHERE p.codigo = :codigo")
    int reponerStock(@Param("codigo") String codigo, @Param("cantidad") int cantidad);

    /** Stock actual leído directo de la base, para el mensaje "solicitado X, disponible Y". */
    @Query("SELECT p.stock FROM Producto p WHERE p.codigo = :codigo")
    Optional<Integer> obtenerStock(@Param("codigo") String codigo);
}
