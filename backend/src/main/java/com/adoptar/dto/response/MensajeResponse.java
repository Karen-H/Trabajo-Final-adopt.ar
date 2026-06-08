package com.adoptar.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MensajeResponse {
    private Long id;
    private Long emisorId;       // null = mensaje del sistema
    private String emisorNombre;
    private String contenido;
    private LocalDateTime creadoEn;
    private boolean esPropio;
}
