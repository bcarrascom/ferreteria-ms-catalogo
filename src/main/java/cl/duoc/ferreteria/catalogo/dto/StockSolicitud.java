package cl.duoc.ferreteria.catalogo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cuerpo de /interno/stock/descuento y /interno/stock/reposicion.
 * @Valid en el elemento hace que también se validen los campos de cada ítem.
 */
public record StockSolicitud(
        @NotEmpty(message = "Debe venir al menos un ítem")
        List<@NotNull(message = "El ítem no puede ser nulo") @Valid ItemStock> items) { }
