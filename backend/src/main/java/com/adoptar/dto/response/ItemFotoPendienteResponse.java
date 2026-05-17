package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ItemFotoPendienteResponse {

    private Long id;
    private String url;
    private Long itemId;
    private String itemTitulo;
    private String itemTipo;
    private String rescatistaNombre;
}
