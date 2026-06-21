package com.adoptar.repository;

import com.adoptar.entity.Notificacion;
import com.adoptar.entity.User;
import com.adoptar.enums.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioOrderByCreadoEnDesc(User usuario);

    long countByUsuarioAndLeidaFalse(User usuario);

    // para dedup de mensajes de chat (solo si no hay una no-leída igual)
    boolean existsByUsuarioAndTipoAndUrlAndLeidaFalse(User usuario, TipoNotificacion tipo, String url);

    // para dedup de recordatorios (evitar duplicados por reinicios, independiente de si fue leída)
    boolean existsByUsuarioAndTipoAndUrl(User usuario, TipoNotificacion tipo, String url);

    void deleteByUsuarioAndTipoIn(User usuario, List<TipoNotificacion> tipos);
}
