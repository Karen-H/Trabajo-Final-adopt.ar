package com.adoptar.service;

import com.adoptar.entity.*;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.EstadoReserva;
import com.adoptar.enums.TipoNotificacion;
import com.adoptar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AnimalRepository animalRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final MensajeRepository mensajeRepository;
    private final BloqueoAdopcionRepository bloqueoRepository;
    private final NotificacionService notificacionService;

    // rescatista propone reserva de un animal para un adoptante
    @Transactional
    public Map<String, Object> proponer(User rescatista, Long animalId, Long adoptanteId) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new RuntimeException("Animal no encontrado"));

        if (!animal.getPublicador().getId().equals(rescatista.getId())) {
            throw new RuntimeException("El animal no te pertenece");
        }
        if (animal.getEstado() != EstadoAnimal.EN_ADOPCION) {
            throw new RuntimeException("El animal no está disponible");
        }

        User adoptante = userRepository.findById(adoptanteId)
                .orElseThrow(() -> new RuntimeException("Adoptante no encontrado"));

        // verificar si el adoptante está bloqueado para este animal específico
        bloqueoRepository.findByAdoptanteAndAnimal(adoptante, animal).ifPresent(b -> {
            if (b.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("El adoptante tiene una restricción para este animal hasta " + b.getBloqueadoHasta().toLocalDate());
            }
        });

        // solo una reserva por animal; no tocar reservas de otros animales
        reservaRepository.findByAnimalAndEstadoIn(animal, List.of(EstadoReserva.PENDIENTE, EstadoReserva.ACTIVA))
                .ifPresent(r -> {
                    if (r.getEstado() == EstadoReserva.ACTIVA) {
                        throw new RuntimeException("El animal ya está reservado");
                    }
                    if (!r.getAdoptante().getId().equals(adoptante.getId())) {
                        throw new RuntimeException("El animal ya tiene una reserva pendiente con otro adoptante");
                    }
                    r.setEstado(EstadoReserva.CANCELADA);
                    reservaRepository.save(r);
                });

        Reserva reserva = reservaRepository.save(Reserva.builder()
                .animal(animal)
                .adoptante(adoptante)
                .rescatista(rescatista)
                .build());

        // mensaje del sistema en el chat
        chatRepository.findByAdoptanteAndRescatista(adoptante, rescatista).ifPresent(chat -> {
            mensajeRepository.save(Mensaje.builder()
                    .chat(chat)
                    .emisor(null)
                    .contenido("RESERVA:" + reserva.getId() + ":" + rescatista.getNombre() + " " + rescatista.getApellido()
                            + " quiere reservarte a " + animal.getNombre() + " para que lo adoptes.")
                    .leido(false)
                    .build());
        });

        notificacionService.crear(adoptante, TipoNotificacion.RESERVA_PROPUESTA,
                rescatista.getNombre() + " " + rescatista.getApellido() + " quiere reservarte a " + animal.getNombre() + " para que lo adoptes",
                "/adopciones");
        return Map.of("reservaId", reserva.getId());
    }

    // adoptante acepta la reserva
    @Transactional
    public void aceptar(User adoptante, Long reservaId) {
        Reserva reserva = getReservaParaAdoptante(adoptante, reservaId);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new RuntimeException("La reserva no está pendiente");
        }
        reserva.setEstado(EstadoReserva.ACTIVA);
        reserva.getAnimal().setEstado(EstadoAnimal.RESERVADO);
        animalRepository.save(reserva.getAnimal());
        reservaRepository.save(reserva);

        chatRepository.findByAdoptanteAndRescatista(adoptante, reserva.getRescatista()).ifPresent(chat -> {
            mensajeRepository.save(Mensaje.builder()
                    .chat(chat)
                    .emisor(null)
                    .contenido(adoptante.getNombre() + " " + adoptante.getApellido()
                            + " aceptó la reserva de " + reserva.getAnimal().getNombre() + ". ¡El animal está reservado!")
                    .leido(false)
                    .build());
        });
        notificacionService.crear(reserva.getRescatista(), TipoNotificacion.RESERVA_ACEPTADA,
                adoptante.getNombre() + " " + adoptante.getApellido() + " aceptó la reserva de " + reserva.getAnimal().getNombre(),
                "/chats");
    }

    // adoptante rechaza la reserva
    @Transactional
    public void rechazar(User adoptante, Long reservaId) {
        Reserva reserva = getReservaParaAdoptante(adoptante, reservaId);
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new RuntimeException("La reserva no está pendiente");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        chatRepository.findByAdoptanteAndRescatista(adoptante, reserva.getRescatista()).ifPresent(chat -> {
            mensajeRepository.save(Mensaje.builder()
                    .chat(chat)
                    .emisor(null)
                    .contenido(adoptante.getNombre() + " " + adoptante.getApellido()
                            + " rechazó la reserva de " + reserva.getAnimal().getNombre() + ".")
                    .leido(false)
                    .build());
        });
        notificacionService.crear(reserva.getRescatista(), TipoNotificacion.RESERVA_RECHAZADA,
                adoptante.getNombre() + " " + adoptante.getApellido() + " rechazó la reserva de " + reserva.getAnimal().getNombre(),
                "/chats");
    }

    // rescatista marca la adopción como concretada
    @Transactional
    public void concretar(User rescatista, Long reservaId) {
        Reserva reserva = getReservaParaRescatista(rescatista, reservaId);
        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new RuntimeException("La reserva no está activa");
        }
        reserva.setEstado(EstadoReserva.CONCRETADA);
        reserva.getAnimal().setEstado(EstadoAnimal.ADOPTADO);
        reserva.getAnimal().setAdoptadoEn(LocalDateTime.now());
        animalRepository.save(reserva.getAnimal());
        reservaRepository.save(reserva);

        chatRepository.findByAdoptanteAndRescatista(reserva.getAdoptante(), rescatista).ifPresent(chat -> {
            mensajeRepository.save(Mensaje.builder()
                    .chat(chat)
                    .emisor(null)
                    .contenido("¡La adopción de " + reserva.getAnimal().getNombre() + " se concretó! Gracias por darle un hogar. 🎉")
                    .leido(false)
                    .build());
        });
    }

    // rescatista cancela la reserva: motivo puede ser NO_CONCRETA, ERROR_RESERVA, PROBLEMA_ANIMAL
    @Transactional
    public void cancelar(User rescatista, Long reservaId, String motivo) {
        Reserva reserva = getReservaParaRescatista(rescatista, reservaId);
        if (reserva.getEstado() != EstadoReserva.ACTIVA && reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new RuntimeException("La reserva no se puede cancelar");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.getAnimal().setEstado(EstadoAnimal.EN_ADOPCION);
        animalRepository.save(reserva.getAnimal());
        reservaRepository.save(reserva);

        String msgExtra = "";
        if ("NO_CONCRETA".equals(motivo)) {
            // bloquear adoptante para este animal por 1 mes
            BloqueoAdopcion bloqueo = bloqueoRepository
                    .findByAdoptanteAndAnimal(reserva.getAdoptante(), reserva.getAnimal())
                    .orElse(BloqueoAdopcion.builder()
                            .adoptante(reserva.getAdoptante())
                            .animal(reserva.getAnimal())
                            .build());
            bloqueo.setBloqueadoHasta(LocalDateTime.now().plusMonths(1));
            bloqueoRepository.save(bloqueo);
            msgExtra = " El adoptante no podrá volver a reservar este animal por 1 mes.";
        }

        final String msgFinal = msgExtra;
        chatRepository.findByAdoptanteAndRescatista(reserva.getAdoptante(), rescatista).ifPresent(chat -> {
            mensajeRepository.save(Mensaje.builder()
                    .chat(chat)
                    .emisor(null)
                    .contenido("La reserva de " + reserva.getAnimal().getNombre() + " fue cancelada." + msgFinal)
                    .leido(false)
                    .build());
        });
        notificacionService.crear(reserva.getAdoptante(), TipoNotificacion.RESERVA_CANCELADA,
                "La reserva de " + reserva.getAnimal().getNombre() + " fue cancelada",
                "/adopciones");
    }

    // reservas pendientes del adoptante con un rescatista (puede haber varias)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReservasPendientes(User adoptante, Long rescatistaId) {
        User rescatista = userRepository.findById(rescatistaId)
                .orElseThrow(() -> new RuntimeException("Rescatista no encontrado"));
        return reservaRepository.findByAdoptanteAndRescatistaAndEstadoIn(
                adoptante, rescatista, List.of(EstadoReserva.PENDIENTE)
        ).stream().map(r -> Map.of(
                "reservaId", (Object) r.getId(),
                "animalNombre", r.getAnimal().getNombre(),
                "rescatistaNombre", r.getRescatista().getNombre() + " " + r.getRescatista().getApellido()
        )).toList();
    }

    // animales del chat que el adoptante consultó y siguen disponibles para reservar
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAnimalesDisponibles(User rescatista, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat no encontrado"));
        if (!chat.getRescatista().getId().equals(rescatista.getId())) {
            throw new RuntimeException("No autorizado");
        }

        // IDs de animales bloqueados para el adoptante de este chat
        Set<Long> idsBloqueados = new java.util.HashSet<>(
                bloqueoRepository.findAnimalIdsBloquedosActivos(chat.getAdoptante(), LocalDateTime.now())
        );

        // solo animales que el adoptante consultó en este chat
        Set<Long> idsInteres = chat.getAnimales().stream()
                .map(a -> a.getId())
                .collect(java.util.stream.Collectors.toSet());
        if (idsInteres.isEmpty()) {
            return List.of();
        }

        return animalRepository.findByPublicador(rescatista).stream()
                .filter(a -> idsInteres.contains(a.getId())
                        && !idsBloqueados.contains(a.getId())
                        && a.getEstado() == EstadoAnimal.EN_ADOPCION
                        && a.isAprobado() && !a.isEliminado()
                        && com.adoptar.enums.CategoriaAnimal.ADOPCION.equals(a.getCategoria()))
                .map(a -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", a.getId());
                    m.put("nombre", a.getNombre());
                    m.put("tipo", a.getTipo().name());
                    m.put("edad", a.getEdad() != null ? a.getEdad().name() : "");
                    return m;
                })
                .toList();
    }

    private Reserva getReservaParaAdoptante(User adoptante, Long reservaId) {
        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        if (!r.getAdoptante().getId().equals(adoptante.getId())) throw new RuntimeException("No autorizado");
        return r;
    }

    private Reserva getReservaParaRescatista(User rescatista, Long reservaId) {
        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        if (!r.getRescatista().getId().equals(rescatista.getId())) throw new RuntimeException("No autorizado");
        return r;
    }

    // IDs de animales bloqueados activos para el adoptante
    @Transactional(readOnly = true)
    public List<Long> getMisBloqueos(User adoptante) {
        return bloqueoRepository.findAnimalIdsBloquedosActivos(adoptante, LocalDateTime.now());
    }

    // rescatista: sus reservas pendientes y activas (para la vista de Mis publicaciones)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMisReservasActivas(User rescatista) {
        return reservaRepository.findByRescatistaAndEstadoIn(rescatista, List.of(EstadoReserva.PENDIENTE, EstadoReserva.ACTIVA))
                .stream()
                .map(r -> Map.of(
                        "reservaId", (Object) r.getId(),
                        "estado", r.getEstado().name(),
                        "animalId", r.getAnimal().getId(),
                        "animalNombre", r.getAnimal().getNombre(),
                        "adoptanteId", r.getAdoptante().getId(),
                        "adoptanteNombre", r.getAdoptante().getNombre() + " " + r.getAdoptante().getApellido()
                ))
                .toList();
    }

    // adoptante: sus reservas pendientes y activas (para la vista de Adopciones)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMisReservasComoAdoptante(User adoptante) {
        List<Reserva> reservas = reservaRepository.findByAdoptanteAndEstadoIn(adoptante, List.of(EstadoReserva.PENDIENTE, EstadoReserva.ACTIVA));
        List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        for (Reserva r : reservas) {
            Animal a = r.getAnimal();
            List<Map<String, Object>> fotos = new java.util.ArrayList<>();
            for (var f : a.getFotos()) {
                if (com.adoptar.enums.EstadoFoto.APROBADA.equals(f.getEstado())) {
                    fotos.add(Map.of("url", "/uploads/" + f.getNombreArchivo()));
                }
            }
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("reservaId", r.getId());
            m.put("estado", r.getEstado().name());
            m.put("animalId", a.getId());
            m.put("animalNombre", a.getNombre());
            m.put("tipo", a.getTipo().name());
            m.put("sexo", a.getSexo() != null ? a.getSexo().name() : "");
            m.put("edad", a.getEdad() != null ? a.getEdad().name() : "");
            m.put("rescatistaNombre", r.getRescatista().getNombre() + " " + r.getRescatista().getApellido());
            m.put("ciudad", a.getCiudad() != null ? a.getCiudad() : "");
            m.put("provincia", a.getProvincia() != null ? a.getProvincia() : "");
            m.put("fotos", fotos);
            resultado.add(m);
        }
        return resultado;
    }
}
