package cl.duoc.ferreteria.catalogo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Excepción base de las reglas de negocio. Guarda el código HTTP que debe recibir el cliente.
 * Los services lanzan sus subclases y ManejadorErrores las convierte en el JSON de error.
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Errores por campo (solo los usa SolicitudInvalidaException). */
    public Map<String, String> getDetalles() {
        return null;
    }
}
