package com.adoptar.service;

import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.FotoPendienteResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AnimalRepository animalRepository;
    private final AnimalFotoRepository animalFotoRepository;

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalesPendientes() {
        return animalRepository.findByAprobadoFalseAndRechazadoFalse()
                .stream()
                .map(this::toAnimalResponse)
                .toList();
    }

    @Transactional
    public AnimalResponse aprobarAnimal(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (animal.isAprobado()) {
            throw new IllegalArgumentException("El animal ya está aprobado");
        }
        animal.setAprobado(true);
        animal.setRechazado(false);
        animal.setMotivoRechazo(null);
        // aprueba todas las fotos pendientes del animal
        animal.getFotos().forEach(f -> {
            if (f.getEstado() == EstadoFoto.PENDIENTE) {
                f.setEstado(EstadoFoto.APROBADA);
            }
        });
        animalRepository.save(animal);
        return toAnimalResponse(animal);
    }

    @Transactional
    public AnimalResponse rechazarAnimal(Long id, String motivo) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (animal.isAprobado()) {
            throw new IllegalArgumentException("El animal ya fue aprobado, no puede rechazarse");
        }
        animal.setRechazado(true);
        animal.setMotivoRechazo(motivo);
        animalRepository.save(animal);
        return toAnimalResponse(animal);
    }

    @Transactional(readOnly = true)
    public List<FotoPendienteResponse> getFotosPendientes() {
        return animalFotoRepository.findByEstadoAndAnimal_Aprobado(EstadoFoto.PENDIENTE, true)
                .stream()
                .map(this::toFotoPendienteResponse)
                .toList();
    }

    @Transactional
    public FotoResponse aprobarFoto(Long id) {
        AnimalFoto foto = animalFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (!foto.getAnimal().isAprobado()) {
            throw new IllegalArgumentException("Esta foto pertenece a un animal que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.APROBADA);
        foto.setMotivoRechazo(null);
        animalFotoRepository.save(foto);
        return toFotoResponse(foto);
    }

    @Transactional
    public FotoResponse rechazarFoto(Long id, String motivo) {
        AnimalFoto foto = animalFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (!foto.getAnimal().isAprobado()) {
            throw new IllegalArgumentException("Esta foto pertenece a un animal que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.RECHAZADA);
        foto.setMotivoRechazo(motivo);
        animalFotoRepository.save(foto);
        return toFotoResponse(foto);
    }

    private AnimalResponse toAnimalResponse(Animal animal) {
        List<FotoResponse> fotos = animal.getFotos().stream()
                .map(f -> FotoResponse.builder()
                        .id(f.getId())
                        .url("/uploads/" + f.getNombreArchivo())
                        .estado(f.getEstado())
                        .motivoRechazo(f.getMotivoRechazo())
                        .build())
                .toList();
        return AnimalResponse.builder()
                .id(animal.getId())
                .nombre(animal.getNombre())
                .sexo(animal.getSexo())
                .edad(animal.getEdad())
                .tipo(animal.getTipo())
                .tipoAdopcion(animal.getTipoAdopcion())
                .estado(animal.getEstado())
                .amigableConGatos(animal.isAmigableConGatos())
                .amigableConPerros(animal.isAmigableConPerros())
                .amigableConNinos(animal.isAmigableConNinos())
                .descripcion(animal.getDescripcion())
                .provincia(animal.getRescatista().getProvincia())
                .ciudad(animal.getRescatista().getCiudad())
                .rescatistaNombre(animal.getRescatista().getNombre() + " " + animal.getRescatista().getApellido())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .rechazado(animal.isRechazado())
                .motivoRechazo(animal.getMotivoRechazo())
                .creadoEn(animal.getCreadoEn())
                .build();
    }

    private FotoPendienteResponse toFotoPendienteResponse(AnimalFoto foto) {
        return FotoPendienteResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .animalId(foto.getAnimal().getId())
                .animalNombre(foto.getAnimal().getNombre())
                .animalTipo(foto.getAnimal().getTipo().name())
                .rescatistaNombre(foto.getAnimal().getRescatista().getNombre() + " " + foto.getAnimal().getRescatista().getApellido())
                .build();
    }

    private FotoResponse toFotoResponse(AnimalFoto foto) {
        return FotoResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .estado(foto.getEstado())
                .motivoRechazo(foto.getMotivoRechazo())
                .build();
    }
}
