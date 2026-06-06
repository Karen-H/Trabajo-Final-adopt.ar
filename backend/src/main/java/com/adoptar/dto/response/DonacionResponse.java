package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class DonacionResponse {
    private Long id;
    private String checkoutUrl;
    private BigDecimal monto;
    private String rescatistaNombre;
}
