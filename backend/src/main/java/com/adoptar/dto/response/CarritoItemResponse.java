package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CarritoItemResponse {
    private Long itemId;
    private String titulo;
    private BigDecimal precio;
    private Integer cantidad;
    private Integer stock;
    private String fotoUrl;
}
