package cl.duoc.ferreteria.catalogo.exception;

import org.springframework.http.HttpStatus;

/** 409: La operación choca con el estado actual (duplicado, stock insuficiente, etc.). */
public class ConflictoException extends NegocioException {

    public ConflictoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
