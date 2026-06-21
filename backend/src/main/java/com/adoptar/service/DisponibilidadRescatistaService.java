package com.adoptar.service;

import com.adoptar.dto.request.DisponibilidadRescatistaRequest;
import com.adoptar.dto.response.DisponibilidadRescatistaResponse;
import com.adoptar.entity.DisponibilidadRescatista;
import com.adoptar.entity.User;
import com.adoptar.repository.DisponibilidadRescatistaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadRescatistaService {

    private final DisponibilidadRescatistaRepository disponibilidadRepository;

    @Transactional
    public List<DisponibilidadRescatistaResponse> agregar(DisponibilidadRescatistaRequest request, User rescatista) {
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

        // combinar bloques existentes del mismo dia con el nuevo y fusionar solapados/contiguos
        List<DisponibilidadRescatista> existentes = disponibilidadRepository
                .findByRescatistaIdAndDiaSemana(rescatista.getId(), request.getDiaSemana());

        List<LocalTime[]> intervalos = new ArrayList<>();
        for (DisponibilidadRescatista d : existentes) {
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
        List<DisponibilidadRescatista> nuevos = merged.stream()
                .map(iv -> DisponibilidadRescatista.builder()
                        .rescatista(rescatista)
                        .diaSemana(request.getDiaSemana())
                        .horaInicio(iv[0])
                        .horaFin(iv[1])
                        .build())
                .toList();
        disponibilidadRepository.saveAll(nuevos);

        return nuevos.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void eliminar(Long id, User rescatista) {
        if (!disponibilidadRepository.existsByIdAndRescatistaId(id, rescatista.getId())) {
            throw new IllegalArgumentException("Disponibilidad no encontrada");
        }
        disponibilidadRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadRescatistaResponse> listarPropias(User rescatista) {
        return disponibilidadRepository.findByRescatistaId(rescatista.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadRescatistaResponse> listarDe(Long rescatistaId) {
        return disponibilidadRepository.findByRescatistaId(rescatistaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean tieneDisponibilidad(Long rescatistaId) {
        return disponibilidadRepository.existsByRescatistaId(rescatistaId);
    }

    private DisponibilidadRescatistaResponse toResponse(DisponibilidadRescatista d) {
        return DisponibilidadRescatistaResponse.builder()
                .id(d.getId())
                .diaSemana(d.getDiaSemana())
                .horaInicio(d.getHoraInicio())
                .horaFin(d.getHoraFin())
                .build();
    }
}
