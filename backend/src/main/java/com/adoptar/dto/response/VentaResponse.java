package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class VentaResponse {
    private Long id;
    private String checkoutUrl;
    private BigDecimal montoTotal;
    private String rescatistaNombre;
}
