package com.adoptar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venta_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemTienda item;

    @Column(nullable = false)
    private Integer cantidad;

    // precio y titulo al momento de la compra, por si el item cambia despues
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private String tituloItem;
}
