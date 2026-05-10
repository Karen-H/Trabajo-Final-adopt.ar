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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaAnimal categoria;

    // solo para ADOPCION
    @Column
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column
    private SexoAnimal sexo;

    @Enumerated(EnumType.STRING)
    @Column
    private RangoEdad edad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAnimal tipo;

    // solo para ADOPCION
    @Enumerated(EnumType.STRING)
    @Column
    private TipoAdopcion tipoAdopcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAnimal estado;

    // solo para ADOPCION
    @Column
    private Boolean amigableConGatos;

    @Column
    private Boolean amigableConPerros;

    @Column
    private Boolean amigableConNinos;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // solo para PERDIDO_ENCONTRADO
    @Column
    private String direccion;

    @Column
    private Double latitud;

    @Column
    private Double longitud;

    @Column
    private Boolean enPosesionDelPublicador;

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
    @JoinColumn(name = "publicador_id", nullable = false)
    private User publicador;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnimalFoto> fotos = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}

