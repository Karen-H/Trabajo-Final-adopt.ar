package com.adoptar.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DonacionRequest {

    @NotNull(message = "El rescatista es obligatorio")
    private Long rescatistaId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "100", message = "El monto mínimo de donación es $100")
    private BigDecimal monto;
}
