package cl.duoc.ferreteria.catalogo.security;

import cl.duoc.ferreteria.catalogo.exception.NoAutorizadoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaveInternaValidadorTest {

    private final ClaveInternaValidador validador = new ClaveInternaValidador("clave-correcta");

    @Test
    void laClaveCorrectaPasa() {
        assertThatCode(() -> validador.validar("clave-correcta")).doesNotThrowAnyException();
    }

    @Test
    void sinClaveOClaveIncorrectaLanza401() {
        for (String recibida : new String[]{null, "", "clave-incorrecta", "clave-correct", "clave-correcta "}) {
            assertThatThrownBy(() -> validador.validar(recibida))
                    .as("clave recibida: [%s]", recibida)
                    .isInstanceOf(NoAutorizadoException.class)
                    .hasMessage("Clave interna inválida");
        }
    }

    @Test
    void elServicioNoArrancaConUnaClaveVacia() {
        assertThatThrownBy(() -> new ClaveInternaValidador("  ")).isInstanceOf(IllegalStateException.class);
    }
}
