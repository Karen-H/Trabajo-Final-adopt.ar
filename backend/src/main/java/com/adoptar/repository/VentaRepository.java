package com.adoptar.repository;

import com.adoptar.entity.User;
import com.adoptar.entity.Venta;
import com.adoptar.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByExternalRef(String externalRef);

    List<Venta> findByRescatistaAndEstadoInOrderByCreadoEnDesc(User rescatista, List<EstadoVenta> estados);

    List<Venta> findByCompradorAndEstadoInOrderByCreadoEnDesc(User comprador, List<EstadoVenta> estados);

    // venta mas reciente del comprador con ese rescatista que todavia necesita una respuesta del bot de envio
    @Query("SELECT v FROM Venta v WHERE v.comprador = :comprador AND v.rescatista.id = :rescatistaId " +
           "AND v.estadoEnvio IS NOT NULL AND v.estadoEnvio <> 'CONFIRMADO' ORDER BY v.creadoEn DESC")
    List<Venta> findPendientesEnvio(@Param("comprador") User comprador, @Param("rescatistaId") Long rescatistaId);
}
