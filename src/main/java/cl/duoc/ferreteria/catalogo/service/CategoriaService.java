package cl.duoc.ferreteria.catalogo.service;

import cl.duoc.ferreteria.catalogo.dto.CategoriaEntrada;
import cl.duoc.ferreteria.catalogo.dto.CategoriaRespuesta;
import cl.duoc.ferreteria.catalogo.exception.ConflictoException;
import cl.duoc.ferreteria.catalogo.exception.NoEncontradoException;
import cl.duoc.ferreteria.catalogo.model.Categoria;
import cl.duoc.ferreteria.catalogo.repository.CategoriaRepository;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reglas de negocio de las categorías. El controller solo traduce HTTP; todo lo que decide está aquí. */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    /** Ordenadas por id: el mismo orden del JSON original, que es el que muestra el front. */
    @Transactional(readOnly = true)
    public List<CategoriaRespuesta> listar() {
        return categoriaRepository.findAll(Sort.by("id")).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaRespuesta obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    /** Nombre único sin importar mayúsculas: "pinturas" choca con "Pinturas" → 409. */
    @Transactional
    public CategoriaRespuesta crear(CategoriaEntrada entrada) {
        String nombre = entrada.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe la categoría " + nombre);
        }
        Categoria categoria = categoriaRepository.save(new Categoria(nombre, limpiar(entrada.descripcion())));
        return CategoriaRespuesta.desde(categoria, 0);
    }

    /** El nombre nuevo no puede ser el de OTRA categoría; repetir el propio sí se permite. */
    @Transactional
    public CategoriaRespuesta actualizar(Long id, CategoriaEntrada entrada) {
        Categoria categoria = buscar(id);
        String nombre = entrada.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoException("Ya existe la categoría " + nombre);
        }
        categoria.setNombre(nombre);
        categoria.setDescripcion(limpiar(entrada.descripcion()));
        return aRespuesta(categoria);   // la entidad está gestionada por JPA: el cambio se guarda al terminar la transacción
    }

    /** Regla 4: no se elimina una categoría con productos, porque esos productos quedarían sin categoría. */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        long cantidad = productoRepository.countByCategoriaId(id);
        if (cantidad > 0) {
            throw new ConflictoException("No se puede eliminar la categoría %s: tiene %d %s"
                    .formatted(categoria.getNombre(), cantidad, cantidad == 1 ? "producto" : "productos"));
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la categoría " + id));
    }

    private CategoriaRespuesta aRespuesta(Categoria categoria) {
        return CategoriaRespuesta.desde(categoria, productoRepository.countByCategoriaId(categoria.getId()));
    }

    /** Descripción vacía o solo espacios se guarda como null. */
    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
