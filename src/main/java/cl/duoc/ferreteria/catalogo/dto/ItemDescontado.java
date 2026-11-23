package cl.duoc.ferreteria.catalogo.dto;

/**
 * Un ítem ya descontado. precioUnitario es el precio de venta actual de la base:
 * es la única fuente de precios de la boleta (el front nunca envía precios).
 */
public record ItemDescontado(String codigo, String nombre, Integer precioUnitario, Integer cantidad) { }
