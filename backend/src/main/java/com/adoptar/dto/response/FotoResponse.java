package com.adoptar.dto.response;

import com.adoptar.enums.EstadoFoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FotoResponse {

    private Long id;
    private String url;
    private EstadoFoto estado;
}
