package com.adoptar.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EnvioDomicilioRequest {

    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    private String altura;
    private String piso;
    private String depto;
    private String descripcion;
}
