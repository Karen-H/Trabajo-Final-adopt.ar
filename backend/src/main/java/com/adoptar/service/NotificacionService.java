package com.adoptar.service;

import com.adoptar.dto.response.NotificacionResponse;
import com.adoptar.entity.Notificacion;
import com.adoptar.entity.User;
import com.adoptar.enums.TipoNotificacion;
import com.adoptar.enums.UserRole;
import com.adoptar.repository.FavoritoRepository;
import com.adoptar.repository.NotificacionRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UserRepository userRepository;
    private final FavoritoRepository favoritoRepository;

    @Transactional
    public void crear(User usuario, TipoNotificacion tipo, String mensaje, String url) {
        notificacionRepository.save(Notificacion.builder()
                .usuario(usuario)
                .tipo(tipo)
                .mensaje(mensaje)
                .url(url)
                .build());
    }

    // crea la notif solo si no hay una no-leída igual (para chat: evitar spam)
    @Transactional
    public void crearSiNoExisteNoLeida(User usuario, TipoNotificacion tipo, String mensaje, String url) {
        if (!notificacionRepository.existsByUsuarioAndTipoAndUrlAndLeidaFalse(usuario, tipo, url)) {
            crear(usuario, tipo, mensaje, url);
        }
    }

    // crea la notif solo si no existe ninguna igual, leída o no (para recordatorios: evitar duplicados por reinicios)
    @Transactional
    public void crearSiNoExiste(User usuario, TipoNotificacion tipo, String mensaje, String url) {
        if (!notificacionRepository.existsByUsuarioAndTipoAndUrl(usuario, tipo, url)) {
            crear(usuario, tipo, mensaje, url);
        }
    }

    @Transactional
    public void crearParaAdminsYMods(TipoNotificacion tipo, String mensaje, String url) {
        List<User> destinatarios = new ArrayList<>();
        destinatarios.addAll(userRepository.findByRole(UserRole.ADMIN));
        destinatarios.addAll(userRepository.findByRole(UserRole.MODERADOR));
        for (User u : destinatarios) {
            crear(u, tipo, mensaje, url);
        }
    }

    @Transactional
    public void crearParaFavoritosDeAnimal(Long animalId, TipoNotificacion tipo, String mensaje, String url) {
        favoritoRepository.findByAnimalId(animalId)
                .forEach(f -> crear(f.getUsuario(), tipo, mensaje, url));
    }

    // limpia recordatorios de tienda del rescatista cuando cambia el slot
    @Transactional
    public void limpiarRecordatoriosTienda(User usuario) {
        notificacionRepository.deleteByUsuarioAndTipoIn(usuario, List.of(
                TipoNotificacion.RECORDATORIO_LLAMADA_7,
                TipoNotificacion.RECORDATORIO_LLAMADA_3,
                TipoNotificacion.RECORDATORIO_LLAMADA_1
        ));
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> getMias(User usuario) {
        return notificacionRepository.findByUsuarioOrderByCreadoEnDesc(usuario)
                .stream()
                .map(n -> NotificacionResponse.builder()
                        .id(n.getId())
                        .tipo(n.getTipo())
                        .mensaje(n.getMensaje())
                        .url(n.getUrl())
                        .leida(n.isLeida())
                        .creadoEn(n.getCreadoEn())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(User usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }

    @Transactional
    public void marcarLeida(Long id, User usuario) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));
        if (!n.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("No autorizado");
        }
        n.setLeida(true);
        notificacionRepository.save(n);
    }

    @Transactional
    public void eliminar(Long id, User usuario) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));
        if (!n.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("No autorizado");
        }
        notificacionRepository.delete(n);
    }
}
