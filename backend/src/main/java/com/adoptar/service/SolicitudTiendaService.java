package com.adoptar.service;

import com.adoptar.dto.request.AceptarSolicitudRequest;
import com.adoptar.dto.request.ReprogramarSolicitudRequest;
import com.adoptar.dto.request.SolicitudTiendaRequest;
import com.adoptar.dto.response.SolicitudTiendaResponse;
import com.adoptar.entity.SolicitudTienda;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoSolicitudTienda;
import com.adoptar.enums.TipoNotificacion;
import com.adoptar.repository.SolicitudTiendaRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SolicitudTiendaService {

    private final SolicitudTiendaRepository solicitudRepository;
    private final DisponibilidadAdminService disponibilidadService;
    private final UserRepository userRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public SolicitudTiendaResponse crear(SolicitudTiendaRequest request, User rescatista) {
        // verificar que no tenga ya una solicitud activa
        Optional<SolicitudTienda> existente = solicitudRepository.findByRescatistaId(rescatista.getId());
        if (existente.isPresent()) {
            EstadoSolicitudTienda estado = existente.get().getEstado();
            if (estado != EstadoSolicitudTienda.RECHAZADA) {
                throw new IllegalArgumentException("Ya tenés una solicitud de tienda activa");
            }
            // si fue rechazada, verificar bloqueo
            SolicitudTienda rechazada = existente.get();
            if (rechazada.getBloqueadoHasta() != null && LocalDate.now().isBefore(rechazada.getBloqueadoHasta())) {
                throw new IllegalArgumentException("Estás bloqueado para solicitar una tienda hasta el " + rechazada.getBloqueadoHasta());
            }
            // bloqueo vencido: eliminar la anterior para poder crear una nueva
            solicitudRepository.delete(rechazada);
        }

        // buscar admins disponibles para el slot elegido
        List<User> candidatos = disponibilidadService.getAdminsDisponiblesParaSlot(
                request.getFechaPreferida(), request.getHoraPreferida());
        if (candidatos.isEmpty()) {
            throw new IllegalArgumentException("El slot elegido no está disponible");
        }

        // asignar random entre los candidatos
        User adminAsignado = candidatos.get(new Random().nextInt(candidatos.size()));

        SolicitudTienda solicitud = SolicitudTienda.builder()
                .rescatista(rescatista)
                .adminAsignado(adminAsignado)
                .fechaPreferida(request.getFechaPreferida())
                .horaPreferida(request.getHoraPreferida())
                .build();
        solicitudRepository.save(solicitud);
        notificacionService.crear(adminAsignado, TipoNotificacion.NUEVA_SOLICITUD_TIENDA,
                rescatista.getNombre() + " " + rescatista.getApellido() + " solicitó abrir una tienda",
                "/admin");
        return toResponse(solicitud);
    }

    @Transactional
    public SolicitudTiendaResponse editar(SolicitudTiendaRequest request, User rescatista) {
        SolicitudTienda solicitud = getSolicitudDelRescatista(rescatista);
        if (solicitud.getEstado() != EstadoSolicitudTienda.PENDIENTE) {
            throw new IllegalArgumentException("Solo podés editar una solicitud en estado PENDIENTE");
        }

        List<User> candidatos = disponibilidadService.getAdminsDisponiblesParaSlot(
                request.getFechaPreferida(), request.getHoraPreferida());
        if (candidatos.isEmpty()) {
            throw new IllegalArgumentException("El slot elegido no está disponible");
        }

        User adminAsignado = candidatos.get(new Random().nextInt(candidatos.size()));
        solicitud.setFechaPreferida(request.getFechaPreferida());
        solicitud.setHoraPreferida(request.getHoraPreferida());
        solicitud.setAdminAsignado(adminAsignado);
        solicitudRepository.save(solicitud);
        return toResponse(solicitud);
    }

    @Transactional
    public void cancelar(User rescatista) {
        SolicitudTienda solicitud = getSolicitudDelRescatista(rescatista);
        if (solicitud.getEstado() != EstadoSolicitudTienda.PENDIENTE) {
            throw new IllegalArgumentException("Solo podés cancelar una solicitud en estado PENDIENTE");
        }
        solicitudRepository.delete(solicitud);
    }

    @Transactional(readOnly = true)
    public SolicitudTiendaResponse getMiSolicitud(User rescatista) {
        SolicitudTienda solicitud = getSolicitudDelRescatista(rescatista);
        return toResponse(solicitud);
    }

    // para la reprogramacion: el rescatista elige un nuevo slot
    @Transactional
    public SolicitudTiendaResponse reprogramarComoRescatista(SolicitudTiendaRequest request, User rescatista) {
        SolicitudTienda solicitud = getSolicitudDelRescatista(rescatista);
        if (solicitud.getEstado() != EstadoSolicitudTienda.REPROGRAMADA) {
            throw new IllegalArgumentException("Solo podés reprogramar una solicitud en estado REPROGRAMADA");
        }

        List<User> candidatos = disponibilidadService.getAdminsDisponiblesParaSlot(
                request.getFechaPreferida(), request.getHoraPreferida());
        if (candidatos.isEmpty()) {
            throw new IllegalArgumentException("El slot elegido no está disponible");
        }

        User nuevoAdmin = candidatos.get(new Random().nextInt(candidatos.size()));
        solicitud.setFechaPreferida(request.getFechaPreferida());
        solicitud.setHoraPreferida(request.getHoraPreferida());
        solicitud.setAdminAsignado(nuevoAdmin);
        solicitud.setEstado(EstadoSolicitudTienda.PENDIENTE);
        solicitud.setMotivoReprogramacion(null);
        solicitud.setLinkLlamada(null);
        solicitudRepository.save(solicitud);
        notificacionService.limpiarRecordatoriosTienda(rescatista);
        notificacionService.crear(nuevoAdmin, TipoNotificacion.SOLICITUD_RESCATISTA_REPROGRAMADA,
                rescatista.getNombre() + " " + rescatista.getApellido() + " reprogramó la llamada para el " + request.getFechaPreferida() + " a las " + request.getHoraPreferida(),
                "/admin");
        return toResponse(solicitud);
    }

    // acciones del admin

    @Transactional
    public SolicitudTiendaResponse aceptar(Long id, AceptarSolicitudRequest request, User admin) {
        SolicitudTienda solicitud = getSolicitudDelAdmin(id, admin);
        if (solicitud.getEstado() != EstadoSolicitudTienda.PENDIENTE) {
            throw new IllegalArgumentException("Solo podés aceptar una solicitud en estado PENDIENTE");
        }
        solicitud.setEstado(EstadoSolicitudTienda.ACEPTADA);
        solicitud.setLinkLlamada(request.getLinkLlamada());
        solicitudRepository.save(solicitud);
        notificacionService.crear(solicitud.getRescatista(), TipoNotificacion.SOLICITUD_TIENDA_ACEPTADA,
                "Tu llamada fue confirmada para el " + solicitud.getFechaPreferida() + " a las " + solicitud.getHoraPreferida(),
                "/abrir-tienda");
        return toResponse(solicitud);
    }

    @Transactional
    public SolicitudTiendaResponse editarLink(Long id, AceptarSolicitudRequest request, User admin) {
        SolicitudTienda solicitud = getSolicitudDelAdmin(id, admin);
        if (solicitud.getEstado() != EstadoSolicitudTienda.ACEPTADA) {
            throw new IllegalArgumentException("Solo podés editar el link de una solicitud ACEPTADA");
        }
        solicitud.setLinkLlamada(request.getLinkLlamada());
        solicitudRepository.save(solicitud);
        return toResponse(solicitud);
    }

    @Transactional
    public SolicitudTiendaResponse aprobar(Long id, User admin) {
        SolicitudTienda solicitud = getSolicitudDelAdmin(id, admin);
        if (solicitud.getEstado() != EstadoSolicitudTienda.ACEPTADA) {
            throw new IllegalArgumentException("Solo podés aprobar una solicitud ACEPTADA");
        }
        solicitud.setEstado(EstadoSolicitudTienda.APROBADA);
        // habilitar la tienda del rescatista
        User rescatista = solicitud.getRescatista();
        rescatista.setTieneTienda(true);
        userRepository.save(rescatista);
        solicitudRepository.save(solicitud);
        notificacionService.limpiarRecordatoriosTienda(rescatista);
        notificacionService.crear(rescatista, TipoNotificacion.TIENDA_APROBADA,
                "¡Tu cuenta fue verificada! Ya podés vender en tu tienda y aceptar donaciones cuando quieras.",
                "/mi-tienda");
        return toResponse(solicitud);
    }

    @Transactional
    public SolicitudTiendaResponse rechazar(Long id, String motivo, User admin) {
        SolicitudTienda solicitud = getSolicitudDelAdmin(id, admin);
        if (solicitud.getEstado() != EstadoSolicitudTienda.ACEPTADA && solicitud.getEstado() != EstadoSolicitudTienda.PENDIENTE) {
            throw new IllegalArgumentException("Solo podés rechazar una solicitud PENDIENTE o ACEPTADA");
        }
        solicitud.setEstado(EstadoSolicitudTienda.RECHAZADA);
        solicitud.setMotivoRechazo(motivo);
        solicitud.setBloqueadoHasta(LocalDate.now().plusMonths(1));
        solicitudRepository.save(solicitud);
        notificacionService.limpiarRecordatoriosTienda(solicitud.getRescatista());
        notificacionService.crear(solicitud.getRescatista(), TipoNotificacion.SOLICITUD_TIENDA_RECHAZADA,
                "Tu solicitud de tienda fue rechazada: " + motivo,
                "/abrir-tienda");
        return toResponse(solicitud);
    }

    @Transactional
    public SolicitudTiendaResponse reprogramar(Long id, ReprogramarSolicitudRequest request, User admin) {
        SolicitudTienda solicitud = getSolicitudDelAdmin(id, admin);
        if (solicitud.getEstado() != EstadoSolicitudTienda.ACEPTADA && solicitud.getEstado() != EstadoSolicitudTienda.PENDIENTE) {
            throw new IllegalArgumentException("Solo podés reprogramar una solicitud PENDIENTE o ACEPTADA");
        }
        solicitud.setEstado(EstadoSolicitudTienda.REPROGRAMADA);
        solicitud.setMotivoReprogramacion(request.getMotivo());
        // resetear admin asignado y link para que vuelva al rescatista
        solicitud.setAdminAsignado(null);
        solicitud.setLinkLlamada(null);
        solicitudRepository.save(solicitud);
        notificacionService.limpiarRecordatoriosTienda(solicitud.getRescatista());
        notificacionService.crear(solicitud.getRescatista(), TipoNotificacion.SOLICITUD_TIENDA_REPROGRAMADA,
                "El administrador pidió reprogramar tu llamada: " + request.getMotivo() + ". Elegí un nuevo horario.",
                "/abrir-tienda");
        return toResponse(solicitud);
    }

    @Transactional(readOnly = true)
    public List<SolicitudTiendaResponse> listarTodas() {
        return solicitudRepository.findAllByOrderByCreadoEnDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private SolicitudTienda getSolicitudDelRescatista(User rescatista) {
        return solicitudRepository.findByRescatistaId(rescatista.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenés una solicitud de tienda"));
    }

    private SolicitudTienda getSolicitudDelAdmin(Long id, User admin) {
        SolicitudTienda solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        if (solicitud.getAdminAsignado() == null || !solicitud.getAdminAsignado().getId().equals(admin.getId())) {
            throw new IllegalArgumentException("No tenés permiso para gestionar esta solicitud");
        }
        return solicitud;
    }

    // avisa a rescatista y admin cuando la llamada está en 7, 3 o 1 día
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void enviarRecordatoriosLlamada() {
        LocalDate hoy = LocalDate.now();
        List<LocalDate> fechasObjetivo = List.of(hoy.plusDays(7), hoy.plusDays(3), hoy.plusDays(1));
        List<SolicitudTienda> proximas = solicitudRepository.findAceptadasConFechaEn(fechasObjetivo);
        for (SolicitudTienda s : proximas) {
            long dias = ChronoUnit.DAYS.between(hoy, s.getFechaPreferida());
            TipoNotificacion tipo = dias == 7 ? TipoNotificacion.RECORDATORIO_LLAMADA_7
                    : dias == 3 ? TipoNotificacion.RECORDATORIO_LLAMADA_3
                    : TipoNotificacion.RECORDATORIO_LLAMADA_1;
            String msgRescatista = "Tenés una llamada programada el " + s.getFechaPreferida() + " a las " + s.getHoraPreferida() + " (en " + dias + " días)";
            notificacionService.crearSiNoExiste(s.getRescatista(), tipo, msgRescatista, "/abrir-tienda");
            if (s.getAdminAsignado() != null) {
                String msgAdmin = "Llamada con " + s.getRescatista().getNombre() + " " + s.getRescatista().getApellido()
                        + " el " + s.getFechaPreferida() + " a las " + s.getHoraPreferida() + " (en " + dias + " días)";
                notificacionService.crearSiNoExiste(s.getAdminAsignado(), tipo, msgAdmin, "/admin");
            }
        }
    }

    // expira automáticamente las solicitudes PENDIENTE cuyo horario ya pasó
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirarSolicitudesVencidas() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        List<SolicitudTienda> vencidas = solicitudRepository.findVencidasPendientes(
                EstadoSolicitudTienda.PENDIENTE, hoy, ahora);
        for (SolicitudTienda s : vencidas) {
            s.setEstado(EstadoSolicitudTienda.RECHAZADA);
            s.setMotivoRechazo("La solicitud no fue aceptada antes del horario propuesto");
            // sin bloqueadoHasta: el usuario puede volver a solicitar inmediatamente
        }
        if (!vencidas.isEmpty()) {
            solicitudRepository.saveAll(vencidas);
        }
    }

    private SolicitudTiendaResponse toResponse(SolicitudTienda s) {
        User admin = s.getAdminAsignado();
        return SolicitudTiendaResponse.builder()
                .id(s.getId())
                .estado(s.getEstado())
                .fechaPreferida(s.getFechaPreferida())
                .horaPreferida(s.getHoraPreferida())
                .linkLlamada(s.getLinkLlamada())
                .motivoReprogramacion(s.getMotivoReprogramacion())
                .motivoRechazo(s.getMotivoRechazo())
                .bloqueadoHasta(s.getBloqueadoHasta())
                .creadoEn(s.getCreadoEn())
                .rescatistaId(s.getRescatista().getId())
                .rescatistaNombre(s.getRescatista().getNombre())
                .rescatistaApellido(s.getRescatista().getApellido())
                .rescatistaEmail(s.getRescatista().getEmail())
                .rescatistaTel(s.getRescatista().getTel())
                .rescatistaOrganizacion(s.getRescatista().getOrganizacion())
                .adminAsignadoId(admin != null ? admin.getId() : null)
                .adminAsignadoNombre(admin != null ? admin.getNombre() : null)
                .adminAsignadoApellido(admin != null ? admin.getApellido() : null)
                .build();
    }
}
