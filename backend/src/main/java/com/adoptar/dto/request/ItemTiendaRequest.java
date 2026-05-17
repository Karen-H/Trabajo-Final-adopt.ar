package com.adoptar.dto.request;

import com.adoptar.enums.TipoItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ItemTiendaRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotNull(message = "El tipo es obligatorio")
    private TipoItem tipo;

    private String descripcion;

    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
}
