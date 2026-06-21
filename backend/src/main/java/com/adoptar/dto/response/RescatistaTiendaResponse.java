package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RescatistaTiendaResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String organizacion;
    private String provincia;
    private String ciudad;
}
