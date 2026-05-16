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
}
