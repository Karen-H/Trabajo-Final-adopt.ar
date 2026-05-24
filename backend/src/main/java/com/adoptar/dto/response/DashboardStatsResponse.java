package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DashboardStatsResponse {

    // adopciones
    private long totalAdoptados;
    private List<MesCountResponse> adoptadosPorMes;
    private long enAdopcionActivos;

    // tránsito vs permanente (animales activos en adopción)
    private long transitoActivos;
    private long permanenteActivos;

    // reportes activos
    private long perdidosActivos;
    private long encontradosActivos;

    // usuarios
    private long totalUsuarios;
    private List<MesCountResponse> usuariosPorMes;

    // por especie (solo adopciones)
    private List<EspecieStatsResponse> animalPorEspecie;

    // tasa de éxito
    private long totalHistoricoAdopcion;
    private long totalHistoricoPerdidos;
    private long totalHistoricoEncontrados;
    private long resueltosPerdidos;
    private long resueltosEncontrados;
    private long totalEliminados;
}
