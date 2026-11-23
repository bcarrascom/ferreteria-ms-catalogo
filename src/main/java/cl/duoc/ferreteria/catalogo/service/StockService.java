package cl.duoc.ferreteria.catalogo.service;

import cl.duoc.ferreteria.catalogo.dto.ItemDescontado;
import cl.duoc.ferreteria.catalogo.dto.ItemStock;
import cl.duoc.ferreteria.catalogo.exception.ConflictoException;
import cl.duoc.ferreteria.catalogo.exception.NoEncontradoException;
import cl.duoc.ferreteria.catalogo.model.Producto;
import cl.duoc.ferreteria.catalogo.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Descuento y reposición de stock para ms-ventas (reglas 5 y 6 del contrato). */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductoRepository productoRepository;

    public StockService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Todo o nada: si un ítem no existe o no alcanza, la excepción revierte la transacción
     * y se deshacen también los descuentos de los ítems anteriores.
     */
    @Transactional
    public List<ItemDescontado> descontar(List<ItemStock> items) {
        List<ItemDescontado> resultado = new ArrayList<>();
        agrupar(items).forEach((codigo, cantidad) -> {
            Producto producto = productoRepository.findByCodigo(codigo)
                    .orElseThrow(() -> new NoEncontradoException("No existe el producto " + codigo));
            if (productoRepository.descontarStock(codigo, cantidad) == 0) {
                int disponible = productoRepository.obtenerStock(codigo).orElse(0);
                throw new ConflictoException("Stock insuficiente para %s: solicitado %d, disponible %d"
                        .formatted(codigo, cantidad, disponible));
            }
            resultado.add(new ItemDescontado(codigo, producto.getNombre(), producto.getPrecioVenta(), cantidad));
        });
        return resultado;
    }

    /**
     * Devuelve stock al anular o eliminar una boleta, o como compensación si ms-ventas no pudo guardarla.
     * Un producto que ya no existe se omite (aclaración C9.3): si no, nunca se podría anular esa boleta.
     */
    @Transactional
    public void reponer(List<ItemStock> items) {
        agrupar(items).forEach((codigo, cantidad) -> {
            if (productoRepository.reponerStock(codigo, cantidad) == 0) {
                log.warn("Reposición omitida: el producto {} ya no existe", codigo);
            }
        });
    }

    /**
     * Códigos en mayúsculas y repetidos sumados. TreeMap los ordena por código: dos compras con los
     * mismos productos bloquean las filas en el mismo orden, así una no espera a la otra para siempre (deadlock).
     */
    private static Map<String, Integer> agrupar(List<ItemStock> items) {
        Map<String, Integer> agrupados = new TreeMap<>();
        for (ItemStock item : items) {
            agrupados.merge(item.codigo().trim().toUpperCase(), item.cantidad(), Integer::sum);
        }
        return agrupados;
    }
}
