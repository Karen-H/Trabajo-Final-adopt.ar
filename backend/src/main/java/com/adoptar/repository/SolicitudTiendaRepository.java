package com.adoptar.repository;

import com.adoptar.entity.SolicitudTienda;
import com.adoptar.enums.EstadoSolicitudTienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;

public interface SolicitudTiendaRepository extends JpaRepository<SolicitudTienda, Long> {

    Optional<SolicitudTienda> findByRescatistaId(Long rescatistaId);

    List<SolicitudTienda> findAllByOrderByCreadoEnDesc();

    List<SolicitudTienda> findByAdminAsignadoIdOrderByCreadoEnDesc(Long adminId);

    // verifica si un slot ya está ocupado para un admin dado (excluyendo RECHAZADA)
    @Query("SELECT COUNT(s) > 0 FROM SolicitudTienda s " +
           "WHERE s.adminAsignado.id = :adminId " +
           "AND s.fechaPreferida = :fecha " +
           "AND s.horaPreferida = :hora " +
           "AND s.estado NOT IN ('RECHAZADA')")
    boolean existsSlotOcupado(@Param("adminId") Long adminId,
                               @Param("fecha") LocalDate fecha,
                               @Param("hora") LocalTime hora);

    // solicitudes PENDIENTE cuyo horario ya pasó (para auto-expirar)
    @Query("SELECT s FROM SolicitudTienda s " +
           "WHERE s.estado = :estado " +
           "AND (s.fechaPreferida < :hoy " +
           "OR (s.fechaPreferida = :hoy AND s.horaPreferida < :ahora))")
    List<SolicitudTienda> findVencidasPendientes(@Param("estado") EstadoSolicitudTienda estado,
                                                 @Param("hoy") LocalDate hoy,
                                                 @Param("ahora") LocalTime ahora);

    // carga todos los slots ocupados en un rango de fechas de una sola vez
    @Query("SELECT s.adminAsignado.id, s.fechaPreferida, s.horaPreferida " +
           "FROM SolicitudTienda s " +
           "WHERE s.fechaPreferida BETWEEN :desde AND :hasta " +
           "AND s.adminAsignado IS NOT NULL " +
           "AND s.estado <> :estadoExcluido")
    List<Object[]> findSlotsOcupadosEnRango(@Param("desde") LocalDate desde,
                                             @Param("hasta") LocalDate hasta,
                                             @Param("estadoExcluido") EstadoSolicitudTienda estadoExcluido);

    @Query("SELECT s FROM SolicitudTienda s WHERE s.estado = 'ACEPTADA' AND s.fechaPreferida IN :fechas")
    List<SolicitudTienda> findAceptadasConFechaEn(@Param("fechas") Collection<LocalDate> fechas);
}
