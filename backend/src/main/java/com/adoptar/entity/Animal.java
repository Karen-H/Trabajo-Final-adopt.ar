package com.adoptar.entity;

import com.adoptar.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "animales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SexoAnimal sexo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RangoEdad edad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAnimal tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAdopcion tipoAdopcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoAnimal estado = EstadoAnimal.EN_ADOPCION;

    @Column(nullable = false)
    private boolean amigableConGatos;

    @Column(nullable = false)
    private boolean amigableConPerros;

    @Column(nullable = false)
    private boolean amigableConNinos;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // false hasta que el admin lo apruebe
    @Column(nullable = false)
    @Builder.Default
    private boolean aprobado = false;

    // true si el admin lo rechazó
    @Column(nullable = false)
    @Builder.Default
    private boolean rechazado = false;

    @Column(columnDefinition = "TEXT")
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescatista_id", nullable = false)
    private User rescatista;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnimalFoto> fotos = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
