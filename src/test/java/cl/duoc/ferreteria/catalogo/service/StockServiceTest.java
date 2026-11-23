package cl.duoc.ferreteria.catalogo.service;

import cl.duoc.ferreteria.catalogo.dto.ItemDescontado;
import cl.duoc.ferreteria.catalogo.dto.ItemStock;
import cl.duoc.ferreteria.catalogo.exception.ConflictoException;
import cl.duoc.ferreteria.catalogo.exception.NoEncontradoException;
import cl.duoc.ferreteria.catalogo.model.Producto;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario de StockService: Mockito simula el repository.
 * El "todo o nada" real (rollback) se prueba contra la base en StockInternoControllerTest.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private StockService stockService;

    // ── descontar ──

    @Test
    void descuentaTodosLosItemsYDevuelveNombreYPrecioDeVentaOrdenadosPorCodigo() {
        existe("MC001", "Cemento Polpaico gris 25 kg", 5990);
        existe("EL004", "Enchufe empotrar 16A c/tierra (schuko)", 3690);
        when(productoRepository.descontarStock(anyString(), anyInt())).thenReturn(1);

        List<ItemDescontado> resultado = stockService.descontar(List.of(item("MC001", 2), item("EL004", 1)));

        assertThat(resultado).containsExactly(
                new ItemDescontado("EL004", "Enchufe empotrar 16A c/tierra (schuko)", 3690, 1),
                new ItemDescontado("MC001", "Cemento Polpaico gris 25 kg", 5990, 2));
    }

    @Test
    void descuentaEnOrdenDeCodigoParaEvitarDeadlocks() {
        existe("MC001", "Cemento", 5990);
        existe("EL004", "Enchufe", 3690);
        when(productoRepository.descontarStock(anyString(), anyInt())).thenReturn(1);

        stockService.descontar(List.of(item("MC001", 2), item("EL004", 1)));

        InOrder orden = inOrder(productoRepository);
        orden.verify(productoRepository).descontarStock("EL004", 1);
        orden.verify(productoRepository).descontarStock("MC001", 2);
    }

    @Test
    void sumaLasCantidadesDeCodigosRepetidosYNormalizaMayusculas() {
        existe("MC001", "Cemento", 5990);
        when(productoRepository.descontarStock("MC001", 5)).thenReturn(1);

        List<ItemDescontado> resultado = stockService.descontar(List.of(item("MC001", 2), item(" mc001 ", 3)));

        assertThat(resultado).singleElement().extracting(ItemDescontado::cantidad).isEqualTo(5);
        verify(productoRepository).descontarStock("MC001", 5);
    }

    @Test
    void productoInexistenteLanza404YNoDescuenta() {
        when(productoRepository.findByCodigo("XX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.descontar(List.of(item("xx999", 1))))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessage("No existe el producto XX999");
        verify(productoRepository, never()).descontarStock(anyString(), anyInt());
    }

    @Test
    void stockInsuficienteLanza409ConSolicitadoYDisponible() {
        existe("HE004", "Sierra circular", 72990);
        when(productoRepository.descontarStock("HE004", 5)).thenReturn(0);        // el UPDATE no afectó filas
        when(productoRepository.obtenerStock("HE004")).thenReturn(Optional.of(4));

        assertThatThrownBy(() -> stockService.descontar(List.of(item("HE004", 5))))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Stock insuficiente para HE004: solicitado 5, disponible 4");
    }

    // ── reponer ──

    @Test
    void reponerSumaRepetidos() {
        when(productoRepository.reponerStock("MC001", 3)).thenReturn(1);

        stockService.reponer(List.of(item("MC001", 1), item("mc001", 2)));

        verify(productoRepository).reponerStock("MC001", 3);
    }

    @Test
    void reponerOmiteCodigosInexistentesSinLanzarError() {
        when(productoRepository.reponerStock("XX999", 1)).thenReturn(0);           // ya no existe
        when(productoRepository.reponerStock("MC001", 2)).thenReturn(1);

        assertThatCode(() -> stockService.reponer(List.of(item("XX999", 1), item("MC001", 2))))
                .doesNotThrowAnyException();
        verify(productoRepository).reponerStock("MC001", 2);                       // el resto sí se repone
    }

    // ── Datos de prueba ──

    private void existe(String codigo, String nombre, int precioVenta) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecioVenta(precioVenta);
        when(productoRepository.findByCodigo(codigo)).thenReturn(Optional.of(p));
    }

    private static ItemStock item(String codigo, int cantidad) {
        return new ItemStock(codigo, cantidad);
    }
}
