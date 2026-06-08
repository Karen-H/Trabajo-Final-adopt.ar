package com.adoptar.dto.request;

import lombok.Getter;

@Getter
public class IniciarChatRequest {
    private Long rescatistaId;
    private Long animalId;
    private String animalNombre;
}
