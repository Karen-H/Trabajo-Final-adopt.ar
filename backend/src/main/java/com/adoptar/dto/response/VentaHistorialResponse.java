package com.adoptar.dto.response;

import com.adoptar.enums.EstadoVenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class VentaHistorialResponse {
    private Long id;
    private EstadoVenta estado;
    private BigDecimal montoTotal;
    private LocalDateTime creadoEn;
    private String otroUsuarioNombre;
    private List<VentaItemResponse> items;
}
