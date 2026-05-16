package com.adoptar.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AceptarSolicitudRequest {

    @NotBlank(message = "El link de llamada es obligatorio")
    private String linkLlamada;
}
