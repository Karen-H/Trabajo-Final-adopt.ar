package com.adoptar.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VentaPreferenciaRequest {

    @NotNull(message = "El rescatista es obligatorio")
    private Long rescatistaId;
}
