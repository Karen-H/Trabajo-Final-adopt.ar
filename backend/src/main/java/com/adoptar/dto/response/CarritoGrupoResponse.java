package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CarritoGrupoResponse {
    private Long rescatistaId;
    private String rescatistaNombre;
    private List<CarritoItemResponse> items;
    private BigDecimal total;
}
