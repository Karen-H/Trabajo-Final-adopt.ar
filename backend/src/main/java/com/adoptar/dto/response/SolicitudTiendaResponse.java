package com.adoptar.dto.response;

import com.adoptar.enums.EstadoSolicitudTienda;
import com.adoptar.enums.MotivoReprogramacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class SolicitudTiendaResponse {

    private Long id;
    private EstadoSolicitudTienda estado;
    private LocalDate fechaPreferida;
    private LocalTime horaPreferida;
    private String linkLlamada;
    private MotivoReprogramacion motivoReprogramacion;
    private String motivoRechazo;
    private LocalDate bloqueadoHasta;
    private LocalDateTime creadoEn;

    // datos del rescatista (visible para el admin)
    private Long rescatistaId;
    private String rescatistaNombre;
    private String rescatistaApellido;
    private String rescatistaEmail;
    private String rescatistaTel;
    private String rescatistaOrganizacion;

    // admin asignado (visible para el admin)
    private Long adminAsignadoId;
    private String adminAsignadoNombre;
    private String adminAsignadoApellido;
}
