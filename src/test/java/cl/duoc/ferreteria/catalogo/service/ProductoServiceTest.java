package cl.duoc.ferreteria.catalogo.service;

import cl.duoc.ferreteria.catalogo.dto.ProductoEntrada;
import cl.duoc.ferreteria.catalogo.dto.ProductoRespuesta;
import cl.duoc.ferreteria.catalogo.exception.ConflictoException;
import cl.duoc.ferreteria.catalogo.exception.NoEncontradoException;
import cl.duoc.ferreteria.catalogo.model.Categoria;
import cl.duoc.ferreteria.catalogo.model.Producto;
import cl.duoc.ferreteria.catalogo.repository.CategoriaRepository;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Test unitario de ProductoService: Mockito simula los repositories, sin base de datos. */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProductoService productoService;

    private final Categoria construccion = categoria(1L, "Mat. Construcción");

    // ── precioCompra solo para ADMINISTRADOR ──

    @Test
    void sinSerAdminNoSeEntregaElPrecioDeCompra() {
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento()));

        ProductoRespuesta respuesta = productoService.obtener("MC001", false);

        assertThat(respuesta.precioCompra()).isNull();
        assertThat(respuesta.precioVenta()).isEqualTo(5990);
    }

    @Test
    void siendoAdminSeEntregaElPrecioDeCompra() {
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento()));

        assertThat(productoService.obtener("MC001", true).precioCompra()).isEqualTo(3200);
    }

    @Test
    void listarAplicaLaMismaReglaATodosLosProductos() {
        when(productoRepository.buscar(null, null)).thenReturn(List.of(cemento(), cemento()));

        assertThat(productoService.listar(null, null, false))
                .extracting(ProductoRespuesta::precioCompra).containsOnlyNulls();
    }

    // ── stockBajo ──

    @Test
    void stockBajoCuandoElStockLlegaAlMinimo() {
        Producto cemento = cemento();
        cemento.setStock(20);                              // stockMinimo = 20
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento));

        assertThat(productoService.obtener("MC001", false).stockBajo()).isTrue();
    }

    @Test
    void noHayStockBajoSobreElMinimo() {
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento()));   // 80 > 20

        assertThat(productoService.obtener("MC001", false).stockBajo()).isFalse();
    }

    // ── listar / obtener ──

    @Test
    void listarConTextoEnBlancoNoFiltraPorTexto() {
        productoService.listar(2L, "   ", false);

        verify(productoRepository).buscar(2L, null);
    }

    @Test
    void obtenerAceptaElCodigoEnMinusculas() {
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento()));

        assertThat(productoService.obtener(" mc001 ", false).codigo()).isEqualTo("MC001");
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(productoRepository.findByCodigo("XX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtener("xx999", false))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessage("No existe el producto XX999");
    }

    // ── crear ──

    @Test
    void crearGuardaElCodigoEnMayusculasYDevuelveElPrecioDeCompra() {
        when(productoRepository.existsByCodigo("MC011")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(construccion));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoRespuesta creado = productoService.crear(entrada(" mc011 ", 1L));

        ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getCodigo()).isEqualTo("MC011");
        assertThat(guardado.getValue().getCategoria()).isSameAs(construccion);
        assertThat(creado.precioCompra()).isEqualTo(3500);
    }

    @Test
    void crearConCodigoDuplicadoLanza409YNoGuarda() {
        when(productoRepository.existsByCodigo("MC001")).thenReturn(true);

        assertThatThrownBy(() -> productoService.crear(entrada("mc001", 1L)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un producto con código MC001");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crearConCategoriaInexistenteLanza404YNoGuarda() {
        when(productoRepository.existsByCodigo("MC011")).thenReturn(false);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crear(entrada("MC011", 99L)))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessage("No existe la categoría 99");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crearGuardaLosOpcionalesVaciosComoNull() {
        when(productoRepository.existsByCodigo("MC011")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(construccion));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        productoService.crear(new ProductoEntrada("MC011", " Cemento ", 1L, "  ", "", null, 6490, 3500, 40, 10));

        ArgumentCaptor<Producto> guardado = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(guardado.capture());
        assertThat(guardado.getValue().getNombre()).isEqualTo("Cemento");
        assertThat(guardado.getValue().getSubcategoria()).isNull();
        assertThat(guardado.getValue().getMarca()).isNull();
    }

    // ── actualizar ──

    @Test
    void actualizarNoCambiaElCodigoAunqueElCuerpoTraigaOtro() {
        Producto cemento = cemento();
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(construccion));

        ProductoRespuesta actualizado = productoService.actualizar("MC001", entrada("ZZ999", 1L));

        assertThat(cemento.getCodigo()).isEqualTo("MC001");
        assertThat(actualizado.codigo()).isEqualTo("MC001");
        assertThat(actualizado.precioVenta()).isEqualTo(6490);
    }

    @Test
    void actualizarInexistenteLanza404() {
        when(productoRepository.findByCodigo("XX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar("XX999", entrada("XX999", 1L)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void actualizarConCategoriaInexistenteLanza404() {
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento()));
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar("MC001", entrada("MC001", 99L)))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessage("No existe la categoría 99");
    }

    // ── eliminar ──

    @Test
    void eliminarExistente() {
        Producto cemento = cemento();
        when(productoRepository.findByCodigo("MC001")).thenReturn(Optional.of(cemento));

        productoService.eliminar("mc001");

        verify(productoRepository).delete(cemento);
    }

    @Test
    void eliminarInexistenteLanza404() {
        when(productoRepository.findByCodigo("XX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.eliminar("XX999")).isInstanceOf(NoEncontradoException.class);
        verify(productoRepository, never()).delete(any());
    }

    // ── Datos de prueba ──

    private Producto cemento() {
        Producto p = new Producto();
        p.setCodigo("MC001");
        p.setNombre("Cemento Polpaico gris 25 kg");
        p.setMarca("Polpaico");
        p.setUnidad("Saco");
        p.setSubcategoria("Cementos");
        p.setPrecioCompra(3200);
        p.setPrecioVenta(5990);
        p.setStock(80);
        p.setStockMinimo(20);
        p.setCategoria(construccion);
        return p;
    }

    private static ProductoEntrada entrada(String codigo, Long categoriaId) {
        return new ProductoEntrada(codigo, "Cemento especial 25 kg", categoriaId, "Cementos", "Polpaico", "Saco",
                6490, 3500, 40, 10);
    }

    private static Categoria categoria(Long id, String nombre) {
        Categoria c = new Categoria(nombre, null);
        c.setId(id);
        return c;
    }
}
