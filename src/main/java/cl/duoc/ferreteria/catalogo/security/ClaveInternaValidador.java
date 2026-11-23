package cl.duoc.ferreteria.catalogo.security;

import cl.duoc.ferreteria.catalogo.exception.NoAutorizadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege /api/v1/interno/** con el header X-API-KEY (regla 7 del contrato).
 * Es un HandlerInterceptor: corre antes de que Spring lea y valide el cuerpo, así una petición
 * sin clave recibe 401 siempre, y no un 400 que revele cómo es la API.
 */
@Component
public class ClaveInternaValidador implements HandlerInterceptor {

    public static final String HEADER = "X-API-KEY";

    private final byte[] esperada;

    public ClaveInternaValidador(@Value("${interno.api-key}") String clave) {
        if (clave == null || clave.isBlank()) {
            // Con una clave vacía, un header X-API-KEY vacío pasaría la validación.
            throw new IllegalStateException("INTERNAL_API_KEY no puede estar vacía");
        }
        this.esperada = clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        validar(request.getHeader(HEADER));
        return true;
    }

    /**
     * MessageDigest.isEqual compara en tiempo constante. Con equals, la comparación termina en el primer
     * carácter distinto, y midiendo cuánto tarda la respuesta se podría adivinar la clave de a un carácter.
     */
    public void validar(String recibida) {
        if (recibida == null || !MessageDigest.isEqual(esperada, recibida.getBytes(StandardCharsets.UTF_8))) {
            throw new NoAutorizadoException("Clave interna inválida");
        }
    }
}
