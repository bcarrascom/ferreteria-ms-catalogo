package cl.duoc.ferreteria.catalogo.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/** 400: un dato no cumple una regla de negocio. Indica el campo para que el front muestre el error junto a él. */
public class SolicitudInvalidaException extends NegocioException {

    private final String campo;

    public SolicitudInvalidaException(String campo, String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
        this.campo = campo;
    }

    @Override
    public Map<String, String> getDetalles() {
        return campo == null ? null : Map.of(campo, getMessage());
    }
}
