package cl.duoc.ferreteria.catalogo.config;

import cl.duoc.ferreteria.catalogo.model.Categoria;
import cl.duoc.ferreteria.catalogo.model.Producto;
import cl.duoc.ferreteria.catalogo.repository.CategoriaRepository;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Al arrancar, si la tabla de productos está vacía, carga las 9 categorías y los 85 productos
 * desde datos/productos.json (el mismo archivo que usaba el front). Si ya hay datos, no hace nada:
 * así reiniciar el servicio no duplica ni pisa los cambios hechos desde la API.
 */
@Component
public class CargaDatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaDatosIniciales.class);
    private static final String ARCHIVO = "datos/productos.json";

    /** Una fila del JSON. @JsonProperty traduce los nombres del front (snake_case) a los de Java. */
    record ProductoJson(String codigo,
                        String categoria,
                        String subcategoria,
                        @JsonProperty("nombre_producto") String nombre,
                        String marca,
                        String unidad,
                        @JsonProperty("precio_compra_clp") Integer precioCompra,
                        @JsonProperty("precio_venta_clp") Integer precioVenta,
                        Integer stock,
                        @JsonProperty("stock_minimo") Integer stockMinimo) { }

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final ObjectMapper objectMapper;

    public CargaDatosIniciales(CategoriaRepository categoriaRepository,
                               ProductoRepository productoRepository,
                               ObjectMapper objectMapper) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional   // todo o nada: si una fila falla, no queda un catálogo a medias
    public void run(String... args) throws Exception {
        if (productoRepository.count() > 0) {
            log.info("Carga inicial omitida: ya hay {} productos", productoRepository.count());
            return;
        }

        ProductoJson[] datos;
        try (InputStream entrada = new ClassPathResource(ARCHIVO).getInputStream()) {
            datos = objectMapper.readValue(entrada, ProductoJson[].class);
        }

        // LinkedHashMap: las categorías se crean en el orden en que aparecen en el JSON (mismo orden que el front)
        Map<String, Categoria> categorias = new LinkedHashMap<>();
        for (ProductoJson fila : datos) {
            Categoria categoria = categorias.computeIfAbsent(fila.categoria(), nombre ->
                    categoriaRepository.findByNombreIgnoreCase(nombre)
                            .orElseGet(() -> categoriaRepository.save(new Categoria(nombre, null))));
            productoRepository.save(aProducto(fila, categoria));
        }

        log.info("Carga inicial: {} categorías y {} productos desde {}", categorias.size(), datos.length, ARCHIVO);
    }

    private static Producto aProducto(ProductoJson fila, Categoria categoria) {
        Producto producto = new Producto();
        producto.setCodigo(fila.codigo().trim().toUpperCase());
        producto.setNombre(fila.nombre());
        producto.setMarca(fila.marca());
        producto.setUnidad(fila.unidad());
        producto.setSubcategoria(fila.subcategoria());
        producto.setPrecioCompra(fila.precioCompra());
        producto.setPrecioVenta(fila.precioVenta());
        producto.setStock(fila.stock());
        producto.setStockMinimo(fila.stockMinimo());
        producto.setCategoria(categoria);
        return producto;
    }
}
