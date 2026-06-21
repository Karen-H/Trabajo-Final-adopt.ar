package com.adoptar.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnimalPreviewResponse {
    private Long id;
    private String categoria;   // "ADOPCION" o "PERDIDO_ENCONTRADO"
    private String tipo;        // "PERRO", "GATO", "OTRO"
    private String estado;      // "EN_ADOPCION", "PERDIDO", "ENCONTRADO", etc.
    private String nombre;      // solo para adopcion
    private String descripcion;
    private String primeraFotoUrl;
}
