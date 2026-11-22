package cl.duoc.ferreteria.catalogo.exception;

import org.springframework.http.HttpStatus;

/** 404: El recurso pedido no existe. */
public class NoEncontradoException extends NegocioException {

    public NoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
