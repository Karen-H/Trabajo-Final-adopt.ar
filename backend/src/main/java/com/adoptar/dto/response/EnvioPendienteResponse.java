package com.adoptar.dto.response;

import com.adoptar.enums.EstadoEnvio;
import com.adoptar.enums.MetodoEnvio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class EnvioPendienteResponse {

    private Long ventaId;
    private EstadoEnvio estadoEnvio;
    private MetodoEnvio metodoEnvio;

    // solo relevante en PENDIENTE_METODO
    private boolean retiroDisponible;

    // solo relevante en PENDIENTE_HORARIO
    private List<DisponibilidadRescatistaResponse> bloquesRetiro;
}
