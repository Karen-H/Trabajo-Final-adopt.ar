package com.adoptar.dto.request;

import com.adoptar.enums.MotivoReprogramacion;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReprogramarSolicitudRequest {

    @NotNull(message = "El motivo de reprogramación es obligatorio")
    private MotivoReprogramacion motivo;
}
