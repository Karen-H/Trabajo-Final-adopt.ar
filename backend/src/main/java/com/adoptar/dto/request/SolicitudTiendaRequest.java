package com.adoptar.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class SolicitudTiendaRequest {

    @NotNull(message = "La fecha preferida es obligatoria")
    private LocalDate fechaPreferida;

    @NotNull(message = "La hora preferida es obligatoria")
    private LocalTime horaPreferida;
}
