package cl.duoc.ferreteria.catalogo.service;

import cl.duoc.ferreteria.catalogo.dto.ProductoEntrada;
import cl.duoc.ferreteria.catalogo.dto.ProductoRespuesta;
import cl.duoc.ferreteria.catalogo.exception.ConflictoException;
import cl.duoc.ferreteria.catalogo.exception.NoEncontradoException;
import cl.duoc.ferreteria.catalogo.model.Categoria;
import cl.duoc.ferreteria.catalogo.model.Producto;
import cl.duoc.ferreteria.catalogo.repository.CategoriaRepository;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de los productos (contrato, sección 6.5):
 * 1. código único y en mayúsculas; 2. precios y stock válidos (en ProductoEntrada);
 * 3. la categoría debe existir. Además, precioCompra solo se entrega al ADMINISTRADOR.
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /** Filtros opcionales: un texto vacío se trata como "sin filtro". */
    @Transactional(readOnly = true)
    public List<ProductoRespuesta> listar(Long categoriaId, String q, boolean esAdmin) {
        return productoRepository.buscar(categoriaId, limpiar(q)).stream()
                .map(producto -> ProductoRespuesta.desde(producto, esAdmin))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoRespuesta obtener(String codigo, boolean esAdmin) {
        return ProductoRespuesta.desde(buscar(codigo), esAdmin);
    }

    /** Solo lo llama un ADMINISTRADOR (lo exige SecurityConfig), por eso la respuesta incluye precioCompra. */
    @Transactional
    public ProductoRespuesta crear(ProductoEntrada entrada) {
        String codigo = normalizarCodigo(entrada.codigo());
        if (productoRepository.existsByCodigo(codigo)) {
            throw new ConflictoException("Ya existe un producto con código " + codigo);
        }
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        copiarDatos(entrada, producto);
        return ProductoRespuesta.desde(productoRepository.save(producto), true);
    }

    /** El código no cambia: identifica al producto en el front, en el carrito y en las boletas de ms-ventas. */
    @Transactional
    public ProductoRespuesta actualizar(String codigo, ProductoEntrada entrada) {
        Producto producto = buscar(codigo);
        copiarDatos(entrada, producto);
        return ProductoRespuesta.desde(producto, true);   // JPA guarda los cambios al cerrar la transacción
    }

    /** Se puede eliminar aunque tenga ventas: las boletas guardan una copia del nombre y del precio. */
    @Transactional
    public void eliminar(String codigo) {
        productoRepository.delete(buscar(codigo));
    }

    private Producto buscar(String codigo) {
        String normalizado = normalizarCodigo(codigo);
        return productoRepository.findByCodigo(normalizado)
                .orElseThrow(() -> new NoEncontradoException("No existe el producto " + normalizado));
    }

    /** Todo menos el código. Lanza 404 si la categoría no existe (regla 3). */
    private void copiarDatos(ProductoEntrada entrada, Producto producto) {
        Categoria categoria = categoriaRepository.findById(entrada.categoriaId())
                .orElseThrow(() -> new NoEncontradoException("No existe la categoría " + entrada.categoriaId()));
        producto.setNombre(entrada.nombre().trim());
        producto.setCategoria(categoria);
        producto.setSubcategoria(limpiar(entrada.subcategoria()));
        producto.setMarca(limpiar(entrada.marca()));
        producto.setUnidad(limpiar(entrada.unidad()));
        producto.setPrecioVenta(entrada.precioVenta());
        producto.setPrecioCompra(entrada.precioCompra());
        producto.setStock(entrada.stock());
        producto.setStockMinimo(entrada.stockMinimo());
    }

    /** "  mc001 " → "MC001" (regla 1: el código se guarda en mayúsculas). */
    private static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private static String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
