package com.adoptar.repository;

import com.adoptar.entity.Chat;
import com.adoptar.entity.Mensaje;
import com.adoptar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByChatOrderByCreadoEnAsc(Chat chat);

    // mensajes no leídos enviados por otro usuario en todos los chats del usuario
    @Query("""
        SELECT COUNT(m) FROM Mensaje m
        WHERE m.chat IN (SELECT c FROM Chat c WHERE c.adoptante = :user OR c.rescatista = :user)
        AND (m.emisor IS NULL OR m.emisor <> :user)
        AND m.leido = false
    """)
    long countNoLeidosByUser(@Param("user") User user);

    // marcar como leídos los mensajes de otro en un chat específico
    @Modifying
    @Query("UPDATE Mensaje m SET m.leido = true WHERE m.chat = :chat AND (m.emisor IS NULL OR m.emisor <> :lector)")
    void marcarLeidosEnChat(@Param("chat") Chat chat, @Param("lector") User lector);
}
