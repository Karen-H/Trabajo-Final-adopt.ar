package com.adoptar.dto.request;

import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.TipoAnimal;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReporteRequest {

    @NotNull(message = "El tipo de animal es obligatorio")
    private TipoAnimal tipo;

    // PERDIDO o ENCONTRADO
    @NotNull(message = "El estado inicial es obligatorio")
    private EstadoAnimal estadoInicial;

    @NotNull(message = "La dirección es obligatoria")
    private String direccion;

    private Double latitud;
    private Double longitud;

    @NotNull(message = "Indicá si el animal está en tu posesión")
    private Boolean enPosesionDelPublicador;

    private String descripcion;
}
