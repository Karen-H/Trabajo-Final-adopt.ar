package com.adoptar.entity;

import com.adoptar.enums.EstadoFoto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "animal_fotos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimalFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false)
    private String nombreArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoFoto estado = EstadoFoto.PENDIENTE;
}
