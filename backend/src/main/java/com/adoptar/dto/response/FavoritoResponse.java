package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FavoritoResponse {

    private Long animalId;
    // true si el animal sigue disponible (no eliminado ni rechazado)
    private boolean disponible;
    private AnimalResponse animal;
}
