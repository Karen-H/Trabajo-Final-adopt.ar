package com.adoptar.entity;

import com.adoptar.enums.EstadoSolicitudTienda;
import com.adoptar.enums.MotivoReprogramacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "solicitudes_tienda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescatista_id", nullable = false)
    private User rescatista;

    // se asigna random al crear, puede resetearse al reprogramar
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_asignado_id")
    private User adminAsignado;

    @Column(nullable = false)
    private LocalDate fechaPreferida;

    @Column(nullable = false)
    private LocalTime horaPreferida;

    // lo completa el admin al aceptar la llamada
    @Column
    private String linkLlamada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoSolicitudTienda estado = EstadoSolicitudTienda.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column
    private MotivoReprogramacion motivoReprogramacion;

    @Column(columnDefinition = "TEXT")
    private String motivoRechazo;

    // se setea al rechazar, bloquea por 1 mes
    @Column
    private LocalDate bloqueadoHasta;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
