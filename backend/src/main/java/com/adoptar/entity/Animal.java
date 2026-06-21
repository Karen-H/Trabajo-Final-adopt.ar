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

    @Column
    private java.time.LocalDate fechaAvistamiento;

    // provincia/ciudad del lugar donde se vio el animal (solo para PERDIDO_ENCONTRADO)
    @Column
    private String provincia;

    @Column
    private String ciudad;

    // las publicaciones se publican directo, sin aprobacion previa; la moderacion es post-publicacion (denuncias)
    @Column(nullable = false)
    @Builder.Default
    private boolean aprobado = true;

    // true si el admin lo rechazó
    @Column(nullable = false)
    @Builder.Default
    private boolean rechazado = false;

    @Column(columnDefinition = "TEXT")
    private String motivoRechazo;

    @Column(columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean eliminado = false;

    // true si lo eliminó un admin (el publicador no puede republicar)
    @Column(columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean eliminadoPorAdmin = false;

    // true si el publicador lo eliminó de forma permanente (no se puede reactivar)
    @Column(columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean eliminadoPermanente = false;

    @Column(columnDefinition = "TEXT")
    private String motivoEliminacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicador_id", nullable = false)
    private User publicador;

    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnimalFoto> fotos = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    // se setea cuando el estado cambia a ADOPTADO
    @Column
    private LocalDateTime adoptadoEn;

    // estado original de un reporte P/E (PERDIDO o ENCONTRADO, nunca cambia)
    @Enumerated(EnumType.STRING)
    @Column
    private EstadoAnimal estadoInicial;

    // se setea cuando el estado de un reporte cambia a RESUELTO
    @Column
    private LocalDateTime resueltoEn;

    @Column(columnDefinition = "bigint not null default 0")
    @Builder.Default
    private long vistas = 0L;
}

