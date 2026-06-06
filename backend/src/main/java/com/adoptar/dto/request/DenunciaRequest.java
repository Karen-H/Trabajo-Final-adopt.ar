package com.adoptar.dto.request;

import com.adoptar.enums.RazonDenuncia;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DenunciaRequest {

    @NotNull(message = "La razón es obligatoria")
    private RazonDenuncia razon;

    private String descripcion;
}
