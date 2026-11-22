package cl.duoc.ferreteria.catalogo.repository;

import cl.duoc.ferreteria.catalogo.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data genera la implementación a partir del nombre de cada método. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Para el 409 al crear: ¿ya existe una categoría con ese nombre? (sin importar mayúsculas) */
    boolean existsByNombreIgnoreCase(String nombre);

    /** Para el 409 al actualizar: el nombre no puede repetirse en OTRA categoría. */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /** Para la carga inicial desde productos.json. */
    Optional<Categoria> findByNombreIgnoreCase(String nombre);
}
