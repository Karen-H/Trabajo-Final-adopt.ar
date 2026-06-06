package com.adoptar.dto.response;

import com.adoptar.enums.CategoriaAnimal;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.RazonDenuncia;
import com.adoptar.enums.TipoAnimal;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DenunciaResponse {

    private Long id;
    private RazonDenuncia razon;
    private String descripcion;
    private LocalDateTime creadoEn;

    private Long animalId;
    private CategoriaAnimal animalCategoria;
    private TipoAnimal animalTipo;
    private String animalNombre;
    private EstadoAnimal animalEstado;
    private String publicadorNombre;

    private String denuncianteNombre;
}
