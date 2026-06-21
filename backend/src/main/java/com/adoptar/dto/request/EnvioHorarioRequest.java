package com.adoptar.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EnvioHorarioRequest {

    @NotNull(message = "El bloque de horario es obligatorio")
    private Long bloqueId;
}
