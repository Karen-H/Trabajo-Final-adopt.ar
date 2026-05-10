package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FotoPendienteResponse {

    private Long id;
    private String url;
    private Long animalId;
    private String animalNombre;
    private String animalTipo;
    private String animalCategoria;
    private String animalEstado;
    private String rescatistaNombre;
}
