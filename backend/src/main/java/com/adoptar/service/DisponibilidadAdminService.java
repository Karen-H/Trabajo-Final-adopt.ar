package com.adoptar.service;

import com.adoptar.dto.request.DisponibilidadAdminRequest;
import com.adoptar.dto.response.DisponibilidadAdminResponse;
import com.adoptar.dto.response.SlotDisponibleResponse;
import com.adoptar.entity.DisponibilidadAdmin;
import com.adoptar.entity.User;
import com.adoptar.enums.DiaSemana;
import com.adoptar.enums.UserRole;
import com.adoptar.repository.DisponibilidadAdminRepository;
import com.adoptar.repository.SolicitudTiendaRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadAdminService {

    private final DisponibilidadAdminRepository disponibilidadRepository;
    private final SolicitudTiendaRepository solicitudRepository;
    private final UserRepository userRepository;

    @Transactional
    public DisponibilidadAdminResponse agregar(DisponibilidadAdminRequest request, User admin) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        DisponibilidadAdmin disponibilidad = DisponibilidadAdmin.builder()
                .admin(admin)
                .diaSemana(request.getDiaSemana())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .build();
        disponibilidadRepository.save(disponibilidad);
        return toResponse(disponibilidad);
    }

    @Transactional
    public void eliminar(Long id, User admin) {
        if (!disponibilidadRepository.existsByIdAndAdminId(id, admin.getId())) {
            throw new IllegalArgumentException("Disponibilidad no encontrada");
        }
        disponibilidadRepository.deleteById(id);
    }

    public List<DisponibilidadAdminResponse> listarPropias(User admin) {
        return disponibilidadRepository.findByAdminId(admin.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    // genera todos los slots disponibles de todos los admins para las proximas 4 semanas
    public List<SlotDisponibleResponse> getSlotsDisponibles() {
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        List<SlotDisponibleResponse> slots = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        LocalDate hasta = hoy.plusMonths(1);

        for (LocalDate fecha = hoy.plusDays(1); !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            final LocalDate fechaFinal = fecha;
            DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());
            List<DisponibilidadAdmin> bloques = disponibilidadRepository.findByDiaSemana(dia);

            for (DisponibilidadAdmin bloque : bloques) {
                // generar slots de 30 minutos dentro del bloque
                LocalTime hora = bloque.getHoraInicio();
                while (hora.plusMinutes(30).compareTo(bloque.getHoraFin()) <= 0) {
                    // verificar que ese slot no esté ya ocupado para ese admin en esa fecha
                    if (!solicitudRepository.existsSlotOcupado(bloque.getAdmin().getId(), fechaFinal, hora)) {
                        final LocalTime horaFinal = hora;
                        // evitar duplicados (si dos admins tienen el mismo slot, se muestra una sola vez)
                        boolean yaCargado = slots.stream().anyMatch(
                                s -> s.getFecha().equals(fechaFinal) && s.getHora().equals(horaFinal));
                        if (!yaCargado) {
                            slots.add(SlotDisponibleResponse.builder()
                                    .fecha(fechaFinal)
                                    .hora(horaFinal)
                                    .build());
                        }
                    }
                    hora = hora.plusMinutes(30);
                }
            }
        }

        slots.sort(Comparator.comparing(SlotDisponibleResponse::getFecha)
                .thenComparing(SlotDisponibleResponse::getHora));
        return slots;
    }

    // dado un slot (fecha+hora), devuelve los admins que lo tienen disponible y libre
    public List<User> getAdminsDisponiblesParaSlot(LocalDate fecha, LocalTime hora) {
        DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());
        List<DisponibilidadAdmin> bloques = disponibilidadRepository.findByDiaSemana(dia);
        List<User> candidatos = new ArrayList<>();
        for (DisponibilidadAdmin bloque : bloques) {
            // el slot debe caer dentro del bloque
            boolean dentroDelBloque = !hora.isBefore(bloque.getHoraInicio())
                    && hora.plusMinutes(30).compareTo(bloque.getHoraFin()) <= 0;
            if (dentroDelBloque && !solicitudRepository.existsSlotOcupado(bloque.getAdmin().getId(), fecha, hora)) {
                candidatos.add(bloque.getAdmin());
            }
        }
        return candidatos;
    }

    private DiaSemana toDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> DiaSemana.LUNES;
            case TUESDAY -> DiaSemana.MARTES;
            case WEDNESDAY -> DiaSemana.MIERCOLES;
            case THURSDAY -> DiaSemana.JUEVES;
            case FRIDAY -> DiaSemana.VIERNES;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    private DisponibilidadAdminResponse toResponse(DisponibilidadAdmin d) {
        return DisponibilidadAdminResponse.builder()
                .id(d.getId())
                .diaSemana(d.getDiaSemana())
                .horaInicio(d.getHoraInicio())
                .horaFin(d.getHoraFin())
                .build();
    }
}
