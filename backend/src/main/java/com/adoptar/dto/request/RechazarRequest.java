package com.adoptar.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RechazarRequest {

    @NotBlank(message = "El motivo de rechazo es obligatorio")
    private String motivo;
}
