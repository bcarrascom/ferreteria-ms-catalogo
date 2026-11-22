package cl.duoc.ferreteria.catalogo.exception;

import org.springframework.http.HttpStatus;

/** 401: Falta una credencial válida (por ejemplo, la clave interna). */
public class NoAutorizadoException extends NegocioException {

    public NoAutorizadoException(String mensaje) {
        super(HttpStatus.UNAUTHORIZED, mensaje);
    }
}
