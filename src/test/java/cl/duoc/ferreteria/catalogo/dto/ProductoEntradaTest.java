package cl.duoc.ferreteria.catalogo.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regla 2 del contrato: precio de venta > 0; precio de compra, stock y stock mínimo >= 0.
 * Se prueba con el mismo Validator que usa @Valid en el controller, pero sin levantar Spring.
 */
class ProductoEntradaTest {

    private static ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void crearValidator() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    @Test
    void productoValidoNoTieneErrores() {
        assertThat(validator.validate(entrada("MC011", 6490, 3500, 40, 10))).isEmpty();
    }

    @Test
    void precioDeCompraYStockEnCeroSonValidos() {
        assertThat(validator.validate(entrada("MC011", 6490, 0, 0, 0))).isEmpty();
    }

    @Test
    void precioDeVentaCeroEsInvalido() {
        assertThat(camposConError(entrada("MC011", 0, 3500, 40, 10))).containsExactly("precioVenta");
    }

    @Test
    void precioDeCompraNegativoEsInvalido() {
        assertThat(camposConError(entrada("MC011", 6490, -1, 40, 10))).containsExactly("precioCompra");
    }

    @Test
    void stockNegativoEsInvalido() {
        assertThat(camposConError(entrada("MC011", 6490, 3500, -1, 10))).containsExactly("stock");
    }

    @Test
    void stockMinimoNegativoEsInvalido() {
        assertThat(camposConError(entrada("MC011", 6490, 3500, 40, -1))).containsExactly("stockMinimo");
    }

    @Test
    void codigoConEspaciosInternosOSimbolosEsInvalido() {
        assertThat(camposConError(entrada("MC 011", 6490, 3500, 40, 10))).containsExactly("codigo");
        assertThat(camposConError(entrada("MC-011", 6490, 3500, 40, 10))).containsExactly("codigo");
    }

    @Test
    void codigoDeMasDe10CaracteresEsInvalido() {
        assertThat(camposConError(entrada("ABCDEFGHIJK", 6490, 3500, 40, 10))).containsExactly("codigo");
    }

    @Test
    void camposObligatoriosVacios() {
        ProductoEntrada vacia = new ProductoEntrada(null, " ", null, null, null, null, null, null, null, null);

        assertThat(camposConError(vacia)).containsExactlyInAnyOrder(
                "codigo", "nombre", "categoriaId", "precioVenta", "precioCompra", "stock", "stockMinimo");
    }

    private static ProductoEntrada entrada(String codigo, Integer precioVenta, Integer precioCompra,
                                           Integer stock, Integer stockMinimo) {
        return new ProductoEntrada(codigo, "Cemento especial 25 kg", 1L, "Cementos", "Polpaico", "Saco",
                precioVenta, precioCompra, stock, stockMinimo);
    }

    private static Set<String> camposConError(ProductoEntrada entrada) {
        Set<ConstraintViolation<ProductoEntrada>> errores = validator.validate(entrada);
        return errores.stream().map(e -> e.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
