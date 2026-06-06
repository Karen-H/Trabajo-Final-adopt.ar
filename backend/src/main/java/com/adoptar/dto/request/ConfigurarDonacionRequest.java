package com.adoptar.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigurarDonacionRequest {
    private boolean aceptaDonaciones;
    private String descripcionDonacion;
}
