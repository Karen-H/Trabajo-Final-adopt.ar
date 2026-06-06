package com.adoptar.dto.response;

import com.adoptar.enums.EstadoDonacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DonacionRecibidaResponse {
    private Long id;
    private String donanteNombre;
    private BigDecimal monto;
    private EstadoDonacion estado;
    private LocalDateTime creadoEn;
}
