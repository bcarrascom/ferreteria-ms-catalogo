package cl.duoc.ferreteria.catalogo.repository;

import cl.duoc.ferreteria.catalogo.model.Categoria;
import cl.duoc.ferreteria.catalogo.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest levanta solo JPA con H2 (sin web ni seguridad). Cada test corre en una
 * transacción que se revierte al terminar, así los tests no se afectan entre sí.
 */
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TestEntityManager em;

    private Categoria herramientas;
    private Categoria pinturas;

    @BeforeEach
    void prepararDatos() {
        herramientas = em.persist(new Categoria("Herramientas", null));
        pinturas = em.persist(new Categoria("Pinturas", null));
        em.persist(producto("HE004", "Sierra circular 7-1/4\" 1200W", "Bauker", 72990, 4, herramientas));
        em.persist(producto("HE001", "Taladro percutor 650W 13mm", "Bosch", 79990, 8, herramientas));
        em.persist(producto("PI001", "Látex blanco 1 galón", "Sipa", 15990, 30, pinturas));
        em.flush();
    }

    // ── Descuento atómico de stock ──

    @Test
    void descontarConStockSuficienteRestaYDevuelve1() {
        int filas = productoRepository.descontarStock("HE004", 3);

        assertThat(filas).isEqualTo(1);
        assertThat(stockEnBase("HE004")).isEqualTo(1);
    }

    @Test
    void descontarTodoElStockDejaCero() {
        assertThat(productoRepository.descontarStock("HE004", 4)).isEqualTo(1);
        assertThat(stockEnBase("HE004")).isZero();
    }

    @Test
    void descontarConStockInsuficienteDevuelve0YNoCambiaElStock() {
        int filas = productoRepository.descontarStock("HE004", 5);

        assertThat(filas).isZero();
        assertThat(stockEnBase("HE004")).isEqualTo(4);
    }

    @Test
    void descontarProductoInexistenteDevuelve0() {
        assertThat(productoRepository.descontarStock("XX999", 1)).isZero();
    }

    @Test
    void reponerSumaStock() {
        assertThat(productoRepository.reponerStock("HE004", 2)).isEqualTo(1);
        assertThat(stockEnBase("HE004")).isEqualTo(6);
    }

    @Test
    void reponerProductoInexistenteDevuelve0() {
        assertThat(productoRepository.reponerStock("XX999", 2)).isZero();
    }

    @Test
    void obtenerStockLeeElValorActual() {
        assertThat(productoRepository.obtenerStock("HE001")).contains(8);
        assertThat(productoRepository.obtenerStock("XX999")).isEmpty();
    }

    // ── Búsqueda con filtros opcionales ──

    @Test
    void buscarSinFiltrosDevuelveTodosOrdenadosPorCodigo() {
        List<Producto> resultado = productoRepository.buscar(null, null);

        assertThat(resultado).extracting(Producto::getCodigo).containsExactly("HE001", "HE004", "PI001");
    }

    @Test
    void buscarPorCategoria() {
        List<Producto> resultado = productoRepository.buscar(pinturas.getId(), null);

        assertThat(resultado).extracting(Producto::getCodigo).containsExactly("PI001");
    }

    @Test
    void buscarPorTextoEnNombreSinImportarMayusculas() {
        assertThat(productoRepository.buscar(null, "TALADRO"))
                .extracting(Producto::getCodigo).containsExactly("HE001");
    }

    @Test
    void buscarPorTextoEnMarcaYCodigo() {
        assertThat(productoRepository.buscar(null, "bauker")).extracting(Producto::getCodigo).containsExactly("HE004");
        assertThat(productoRepository.buscar(null, "pi0")).extracting(Producto::getCodigo).containsExactly("PI001");
    }

    @Test
    void buscarCombinaCategoriaYTexto() {
        assertThat(productoRepository.buscar(herramientas.getId(), "látex")).isEmpty();
    }

    @Test
    void buscarTraeLaCategoriaCargada() {
        em.clear();                                    // obliga a leer desde la base, no desde la memoria de JPA
        Producto producto = productoRepository.buscar(null, "PI001").get(0);
        em.clear();                                    // desconecta el producto: si la categoría no vino, fallaría aquí

        assertThat(producto.getCategoria().getNombre()).isEqualTo("Pinturas");
    }

    @Test
    void findByCodigoTraeLaCategoriaCargada() {
        em.clear();
        Producto producto = productoRepository.findByCodigo("HE001").orElseThrow();
        em.clear();

        assertThat(producto.getCategoria().getNombre()).isEqualTo("Herramientas");
    }

    // ── Consultas derivadas del nombre del método ──

    @Test
    void contarProductosPorCategoria() {
        assertThat(productoRepository.countByCategoriaId(herramientas.getId())).isEqualTo(2);
        assertThat(productoRepository.countByCategoriaId(pinturas.getId())).isEqualTo(1);
    }

    @Test
    void findByCodigoYExistsByCodigo() {
        assertThat(productoRepository.findByCodigo("HE001")).isPresent();
        assertThat(productoRepository.existsByCodigo("HE001")).isTrue();
        assertThat(productoRepository.existsByCodigo("XX999")).isFalse();
    }

    // ── Utilidades ──

    /** Lee el stock desde la base y no desde la memoria de JPA, que no se entera de los UPDATE masivos. */
    private Integer stockEnBase(String codigo) {
        em.clear();
        return productoRepository.findByCodigo(codigo).orElseThrow().getStock();
    }

    private static Producto producto(String codigo, String nombre, String marca, int precioVenta,
                                     int stock, Categoria categoria) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setMarca(marca);
        p.setUnidad("Unidad");
        p.setSubcategoria("General");
        p.setPrecioCompra(precioVenta / 2);
        p.setPrecioVenta(precioVenta);
        p.setStock(stock);
        p.setStockMinimo(2);
        p.setCategoria(categoria);
        return p;
    }
}
