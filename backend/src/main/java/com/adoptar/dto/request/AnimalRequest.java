package com.adoptar.dto.request;

import com.adoptar.enums.RangoEdad;
import com.adoptar.enums.SexoAnimal;
import com.adoptar.enums.TipoAdopcion;
import com.adoptar.enums.TipoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnimalRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El sexo es obligatorio")
    private SexoAnimal sexo;

    @NotNull(message = "La edad es obligatoria")
    private RangoEdad edad;

    @NotNull(message = "El tipo es obligatorio")
    private TipoAnimal tipo;

    @NotNull(message = "El tipo de adopción es obligatorio")
    private TipoAdopcion tipoAdopcion;

    private boolean amigableConGatos;
    private boolean amigableConPerros;
    private boolean amigableConNinos;

    private String descripcion;
}
