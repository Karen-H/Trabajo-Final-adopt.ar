package com.adoptar.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatResumenResponse {
    private Long id;
    private Long otroUsuarioId;
    private String otroUsuarioNombre;
    private String ultimoMensaje;
    private LocalDateTime ultimoMensajeEn;
    private long noLeidos;
    private String rolEnChat; // "ADOPTANTE" o "RESCATISTA"
}
