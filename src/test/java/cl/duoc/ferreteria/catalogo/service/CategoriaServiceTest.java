package cl.duoc.ferreteria.catalogo.service;

import cl.duoc.ferreteria.catalogo.dto.CategoriaEntrada;
import cl.duoc.ferreteria.catalogo.dto.CategoriaRespuesta;
import cl.duoc.ferreteria.catalogo.exception.ConflictoException;
import cl.duoc.ferreteria.catalogo.exception.NoEncontradoException;
import cl.duoc.ferreteria.catalogo.model.Categoria;
import cl.duoc.ferreteria.catalogo.repository.CategoriaRepository;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario: sin base de datos ni Spring. Mockito reemplaza los repositories por objetos
 * simulados, así se prueba solo la lógica de CategoriaService.
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    // ── listar / obtener ──

    @Test
    void listarIncluyeLaCantidadDeProductos() {
        when(categoriaRepository.findAll(any(Sort.class))).thenReturn(List.of(categoria(1L, "Pinturas")));
        when(productoRepository.countByCategoriaId(1L)).thenReturn(9L);

        List<CategoriaRespuesta> lista = categoriaService.listar();

        assertThat(lista).singleElement().satisfies(c -> {
            assertThat(c.nombre()).isEqualTo("Pinturas");
            assertThat(c.cantidadProductos()).isEqualTo(9);
        });
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.obtener(99L))
                .isInstanceOf(NoEncontradoException.class)
                .hasMessage("No existe la categoría 99");
    }

    // ── crear ──

    @Test
    void crearConNombreDuplicadoLanza409YNoGuarda() {
        when(categoriaRepository.existsByNombreIgnoreCase("pinturas")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crear(new CategoriaEntrada("pinturas", null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe la categoría pinturas");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void crearRecortaEspaciosYGuardaDescripcionVaciaComoNull() {
        when(categoriaRepository.existsByNombreIgnoreCase("Aseo")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaRespuesta creada = categoriaService.crear(new CategoriaEntrada("  Aseo  ", "   "));

        ArgumentCaptor<Categoria> guardada = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(guardada.capture());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Aseo");
        assertThat(guardada.getValue().getDescripcion()).isNull();
        assertThat(creada.cantidadProductos()).isZero();
    }

    // ── actualizar ──

    @Test
    void actualizarInexistenteLanza404() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.actualizar(99L, new CategoriaEntrada("Aseo", null)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void actualizarConElNombreDeOtraCategoriaLanza409() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Pinturas")));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Madera", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.actualizar(1L, new CategoriaEntrada("Madera", null)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void actualizarManteniendoSuPropioNombreSePermite() {
        Categoria pinturas = categoria(1L, "Pinturas");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(pinturas));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Pinturas", 1L)).thenReturn(false);

        CategoriaRespuesta actualizada = categoriaService.actualizar(1L, new CategoriaEntrada("Pinturas", "Látex y esmaltes"));

        assertThat(actualizada.descripcion()).isEqualTo("Látex y esmaltes");
        assertThat(pinturas.getDescripcion()).isEqualTo("Látex y esmaltes");
    }

    // ── eliminar ──

    @Test
    void eliminarConProductosLanza409YNoElimina() {
        Categoria electricidad = categoria(5L, "Electricidad");
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(electricidad));
        when(productoRepository.countByCategoriaId(5L)).thenReturn(13L);

        assertThatThrownBy(() -> categoriaService.eliminar(5L))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("No se puede eliminar la categoría Electricidad: tiene 13 productos");
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void eliminarSinProductosElimina() {
        Categoria vacia = categoria(10L, "Aseo");
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(vacia));
        when(productoRepository.countByCategoriaId(10L)).thenReturn(0L);

        categoriaService.eliminar(10L);

        verify(categoriaRepository).delete(vacia);
    }

    @Test
    void eliminarInexistenteLanza404() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.eliminar(99L)).isInstanceOf(NoEncontradoException.class);
    }

    private static Categoria categoria(Long id, String nombre) {
        Categoria c = new Categoria(nombre, null);
        c.setId(id);
        return c;
    }
}
