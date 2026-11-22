package cl.duoc.ferreteria.catalogo.config;

import cl.duoc.ferreteria.catalogo.model.Categoria;
import cl.duoc.ferreteria.catalogo.model.Producto;
import cl.duoc.ferreteria.catalogo.repository.CategoriaRepository;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Levanta la aplicación completa sobre H2: el CommandLineRunner ya corrió al arrancar. */
@SpringBootTest
class CargaDatosInicialesTest {

    @Autowired
    private CargaDatosIniciales cargaDatosIniciales;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void cargaLas9CategoriasYLos85Productos() {
        assertThat(categoriaRepository.count()).isEqualTo(9);
        assertThat(productoRepository.count()).isEqualTo(85);
    }

    @Test
    void lasCategoriasQuedanEnElOrdenDelJson() {
        List<String> nombres = categoriaRepository.findAll(Sort.by("id")).stream()
                .map(Categoria::getNombre).toList();

        assertThat(nombres).containsExactly("Mat. Construcción", "Pinturas", "Herramientas", "Gasfitería",
                "Electricidad", "Tornillería", "Madera", "Seguridad", "Jardín");
    }

    @Test
    void cadaCampoDelJsonQuedaEnSuColumna() {
        Producto cemento = productoRepository.findByCodigo("MC001").orElseThrow();

        assertThat(cemento.getNombre()).isEqualTo("Cemento Polpaico gris 25 kg");
        assertThat(cemento.getCategoria().getNombre()).isEqualTo("Mat. Construcción");
        assertThat(cemento.getSubcategoria()).isEqualTo("Cementos");
        assertThat(cemento.getMarca()).isEqualTo("Polpaico");
        assertThat(cemento.getUnidad()).isEqualTo("Saco");
        assertThat(cemento.getPrecioCompra()).isEqualTo(3200);
        assertThat(cemento.getPrecioVenta()).isEqualTo(5990);
        assertThat(cemento.getStock()).isEqualTo(80);
        assertThat(cemento.getStockMinimo()).isEqualTo(20);
    }

    @Test
    void volverACorrerLaCargaNoDuplicaNada() throws Exception {
        cargaDatosIniciales.run();

        assertThat(categoriaRepository.count()).isEqualTo(9);
        assertThat(productoRepository.count()).isEqualTo(85);
    }
}
