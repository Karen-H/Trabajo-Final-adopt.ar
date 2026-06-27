package com.adoptar.dto.response;

import com.adoptar.enums.EstadoItem;
import com.adoptar.enums.TipoItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ItemTiendaResponse {

    private Long id;
    private String titulo;
    private TipoItem tipo;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private List<FotoResponse> fotos;
    private EstadoItem estado;
    private String motivoRechazo;
    private String rescatistaNombre;
    private String rescatistaOrganizacion;
    private String rescatistaCiudad;
    private String rescatistaProvincia;
    private LocalDateTime creadoEn;
}
