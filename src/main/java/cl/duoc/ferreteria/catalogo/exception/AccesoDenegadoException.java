package cl.duoc.ferreteria.catalogo.exception;

import org.springframework.http.HttpStatus;

/** 403: La credencial es válida pero no tiene permiso. */
public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
