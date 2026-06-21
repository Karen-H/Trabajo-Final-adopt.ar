package com.adoptar.dto.response;

import com.adoptar.enums.TipoNotificacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificacionResponse {
    private Long id;
    private TipoNotificacion tipo;
    private String mensaje;
    private String url;
    private boolean leida;
    private LocalDateTime creadoEn;
}
