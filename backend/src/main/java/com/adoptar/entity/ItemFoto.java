package com.adoptar.entity;

import com.adoptar.enums.EstadoFoto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item_fotos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemTienda item;

    @Column(nullable = false)
    private String nombreArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoFoto estado = EstadoFoto.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String motivoRechazo;
}
