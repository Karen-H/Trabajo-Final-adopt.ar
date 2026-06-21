package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TiendaActivaResponse {

    private Long usuarioId;
    private String nombre;
    private String apellido;
    private String email;
    private String tel;
    private String organizacion;
    private String provincia;
    private String ciudad;
    private boolean aceptaDonaciones;
}
