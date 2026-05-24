package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EspecieStatsResponse {
    private String especie;
    private long totalHistorico;
    private long publicadosActuales;
}
