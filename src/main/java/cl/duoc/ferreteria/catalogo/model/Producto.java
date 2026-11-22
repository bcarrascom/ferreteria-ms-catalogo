package cl.duoc.ferreteria.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla "producto". El código (por ejemplo MC001) es el identificador que usan el front y ms-ventas;
 * el id numérico es interno. La imagen no se guarda: el front la arma como /img/productos/{codigo}.jpg.
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;                                // siempre en MAYÚSCULAS

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 60)
    private String marca;

    @Column(length = 30)
    private String unidad;

    @Column(length = 60)
    private String subcategoria;

    // Montos en pesos chilenos enteros: nunca double (los decimales binarios no son exactos)
    @Column(name = "precio_compra", nullable = false)
    private Integer precioCompra;

    @Column(name = "precio_venta", nullable = false)
    private Integer precioVenta;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    /**
     * LAZY: la categoría se lee de la base solo si se usa. Con open-in-view=false eso tiene que
     * ocurrir dentro de una transacción (en el service) o con JOIN FETCH en el repository.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)  // foreign key hacia categoria.id
    private Categoria categoria;
}
