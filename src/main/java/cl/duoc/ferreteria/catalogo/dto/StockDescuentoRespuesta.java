package cl.duoc.ferreteria.catalogo.dto;

import java.util.List;

/** Respuesta 200 del descuento: los ítems ordenados por código, con nombre y precio. */
public record StockDescuentoRespuesta(List<ItemDescontado> items) { }
