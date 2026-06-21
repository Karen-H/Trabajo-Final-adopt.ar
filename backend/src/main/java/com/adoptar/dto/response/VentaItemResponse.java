package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class VentaItemResponse {
    private Long itemId;
    private String titulo;
    private Integer cantidad;
    private BigDecimal precioUnitario;
}
