package com.adoptar.dto.request;

import com.adoptar.enums.MetodoEnvio;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EnvioMetodoRequest {

    @NotNull(message = "El método de envío es obligatorio")
    private MetodoEnvio metodo;
}
