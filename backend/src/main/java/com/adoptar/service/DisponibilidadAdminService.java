package com.adoptar.service;

import com.adoptar.dto.request.DisponibilidadAdminRequest;
import com.adoptar.dto.response.DisponibilidadAdminResponse;
import com.adoptar.dto.response.SlotDisponibleResponse;
import com.adoptar.entity.DisponibilidadAdmin;
import com.adoptar.entity.User;
import com.adoptar.enums.DiaSemana;
import com.adoptar.enums.EstadoSolicitudTienda;
import com.adoptar.repository.DisponibilidadAdminRepository;
import com.adoptar.repository.SolicitudTiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisponibilidadAdminService {

    private final DisponibilidadAdminRepository disponibilidadRepository;
    private final SolicitudTiendaRepository solicitudRepository;

    @Transactional
    public List<DisponibilidadAdminResponse> agregar(DisponibilidadAdminRequest request, User admin) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        int inicioMin = request.getHoraInicio().getMinute();
        int finMin = request.getHoraFin().getMinute();
        if (inicioMin != 0 && inicioMin != 30) {
            throw new IllegalArgumentException("La hora de inicio debe ser en punto (:00) o y media (:30)");
        }
        if (finMin != 0 && finMin != 30) {
            throw new IllegalArgumentException("La hora de fin debe ser en punto (:00) o y media (:30)");
        }

        // combinar bloques existentes del mismo día con el nuevo y fusionar solapados/contiguos
        List<DisponibilidadAdmin> existentes = disponibilidadRepository
                .findByAdminIdAndDiaSemana(admin.getId(), request.getDiaSemana());

        List<LocalTime[]> intervalos = new ArrayList<>();
        for (DisponibilidadAdmin d : existentes) {
            intervalos.add(new LocalTime[]{ d.getHoraInicio(), d.getHoraFin() });
        }
        intervalos.add(new LocalTime[]{ request.getHoraInicio(), request.getHoraFin() });
        intervalos.sort(Comparator.comparingInt(iv -> iv[0].toSecondOfDay()));

        List<LocalTime[]> merged = new ArrayList<>();
        for (LocalTime[] iv : intervalos) {
            if (merged.isEmpty() || iv[0].isAfter(merged.get(merged.size() - 1)[1])) {
                merged.add(new LocalTime[]{ iv[0], iv[1] });
            } else if (iv[1].isAfter(merged.get(merged.size() - 1)[1])) {
                merged.get(merged.size() - 1)[1] = iv[1];
            }
        }

        disponibilidadRepository.deleteAll(existentes);
        List<DisponibilidadAdmin> nuevos = merged.stream()
                .map(iv -> DisponibilidadAdmin.builder()
                        .admin(admin)
                        .diaSemana(request.getDiaSemana())
                        .horaInicio(iv[0])
                        .horaFin(iv[1])
                        .build())
                .toList();
        disponibilidadRepository.saveAll(nuevos);

        return nuevos.stream().map(this::toResponse).toList();
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

    // genera todos los slots disponibles de todos los admins para el proximo mes
    @Transactional(readOnly = true)
    public List<SlotDisponibleResponse> getSlotsDisponibles() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        LocalDate hasta = hoy.plusMonths(1);

        // próximo bloque de 30 min disponible hoy (ej: 18:05 → 18:30, 18:30 → 19:00)
        int minutosAhora = ahora.getHour() * 60 + ahora.getMinute();
        int siguienteBloqueMin = ((minutosAhora / 30) + 1) * 30;

        // carga todas las disponibilidades con sus admins en una sola query
        List<DisponibilidadAdmin> todasDisponibilidades = disponibilidadRepository.findAllWithAdmin();
        List<Object[]> ocupadosRaw = solicitudRepository.findSlotsOcupadosEnRango(
                hoy, hasta, EstadoSolicitudTienda.RECHAZADA);
        Set<String> slotsOcupados = ocupadosRaw.stream()
                .map(row -> row[0] + "|" + row[1] + "|" + row[2])
                .collect(Collectors.toCollection(HashSet::new));

        List<SlotDisponibleResponse> slots = new ArrayList<>();
        Set<String> slotsAgregados = new HashSet<>();
        for (LocalDate fecha = hoy; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            final LocalDate fechaFinal = fecha;
            DiaSemana dia = toDiaSemana(fecha.getDayOfWeek());
            List<DisponibilidadAdmin> bloques = todasDisponibilidades.stream()
                    .filter(d -> d.getDiaSemana() == dia)
                    .toList();

            for (DisponibilidadAdmin bloque : bloques) {
                int minutosInicio = bloque.getHoraInicio().toSecondOfDay() / 60;
                int minutosFin = bloque.getHoraFin().toSecondOfDay() / 60;
                // para hoy: empezar desde el próximo bloque de 30 min
                int inicioEfectivo = fechaFinal.equals(hoy)
                        ? Math.max(minutosInicio, siguienteBloqueMin)
                        : minutosInicio;
                for (int m = inicioEfectivo; m + 30 <= minutosFin; m += 30) {
                    LocalTime horaFinal = LocalTime.of(m / 60, m % 60);
                    String keyOcupado = bloque.getAdmin().getId() + "|" + fechaFinal + "|" + horaFinal;
                    String keySlot = fechaFinal + "|" + horaFinal;
                    if (!slotsOcupados.contains(keyOcupado) && slotsAgregados.add(keySlot)) {
                        slots.add(SlotDisponibleResponse.builder()
                                .fecha(fechaFinal)
                                .hora(horaFinal)
                                .build());
                    }
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
