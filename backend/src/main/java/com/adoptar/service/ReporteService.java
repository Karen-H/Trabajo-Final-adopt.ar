package com.adoptar.service;

import com.adoptar.dto.request.ReporteRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.User;
import com.adoptar.enums.CategoriaAnimal;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.enums.UserRole;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final AnimalRepository animalRepository;
    private final AnimalFotoRepository animalFotoRepository;

    @Value("${uploads.path}")
    private String uploadsPath;

    private static final Set<String> TIPOS_IMAGEN = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Transactional
    public AnimalResponse crearReporte(User publicador, ReporteRequest request, List<MultipartFile> fotos) {
        if (request.getEstadoInicial() != EstadoAnimal.PERDIDO && request.getEstadoInicial() != EstadoAnimal.ENCONTRADO) {
            throw new IllegalArgumentException("El estado inicial debe ser PERDIDO o ENCONTRADO");
        }
        if (fotos == null || fotos.isEmpty()) {
            throw new IllegalArgumentException("Debés subir al menos una foto");
        }
        if (fotos.size() > 5) {
            throw new IllegalArgumentException("No podés subir más de 5 fotos");
        }
        for (MultipartFile foto : fotos) {
            String contentType = foto.getContentType();
            if (contentType == null || !TIPOS_IMAGEN.contains(contentType)) {
                throw new IllegalArgumentException("Solo se aceptan imágenes (jpg, png, webp, gif)");
            }
        }

        boolean esAdmin = publicador.getRole() == UserRole.ADMIN;

        Animal animal = Animal.builder()
                .categoria(CategoriaAnimal.PERDIDO_ENCONTRADO)
                .tipo(request.getTipo())
                .estado(request.getEstadoInicial())
                .direccion(request.getDireccion())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .enPosesionDelPublicador(request.getEnPosesionDelPublicador())
                .provincia(request.getProvincia())
                .ciudad(request.getCiudad())
                .fechaAvistamiento(request.getFechaAvistamiento())
                .descripcion(request.getDescripcion())
                .publicador(publicador)
                .build();

        if (esAdmin) {
            animal.setAprobado(true);
        }

        animalRepository.save(animal);
        guardarFotos(animal, fotos);

        // si es admin, aprobar las fotos automaticamente
        if (esAdmin) {
            animal.getFotos().forEach(f -> f.setEstado(EstadoFoto.APROBADA));
        }

        return toResponse(animal);
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getPerdidos() {
        return animalRepository.findByCategoriaAndAprobadoTrueAndEstado(
                CategoriaAnimal.PERDIDO_ENCONTRADO, EstadoAnimal.PERDIDO)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getEncontrados() {
        return animalRepository.findByCategoriaAndAprobadoTrueAndEstado(
                CategoriaAnimal.PERDIDO_ENCONTRADO, EstadoAnimal.ENCONTRADO)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getMisReportes(User publicador) {
        return animalRepository.findByPublicador(publicador)
                .stream()
                .filter(a -> a.getCategoria() == CategoriaAnimal.PERDIDO_ENCONTRADO)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnimalResponse resolver(Long id, User publicador) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));
        if (!animal.getPublicador().getId().equals(publicador.getId())) {
            throw new IllegalArgumentException("No tenés permiso para modificar este reporte");
        }
        if (animal.getCategoria() != CategoriaAnimal.PERDIDO_ENCONTRADO) {
            throw new IllegalArgumentException("Este endpoint es solo para reportes");
        }
        if (animal.getEstado() == EstadoAnimal.RESUELTO) {
            throw new IllegalArgumentException("Este reporte ya está resuelto");
        }
        animal.setEstado(EstadoAnimal.RESUELTO);
        animalRepository.save(animal);
        return toResponse(animal);
    }

    // fotos pendientes de aprobación (solo perdido/encontrado)
    @Transactional(readOnly = true)
    public List<Animal> getPendientesAdmin() {
        return animalRepository.findByCategoriaAndAprobadoFalseAndRechazadoFalse(CategoriaAnimal.PERDIDO_ENCONTRADO);
    }

    private void guardarFotos(Animal animal, List<MultipartFile> fotos) {
        for (MultipartFile foto : fotos) {
            String extension = getExtension(foto.getOriginalFilename());
            String nombreArchivo = UUID.randomUUID() + extension;
            try {
                foto.transferTo(Paths.get(uploadsPath).resolve(nombreArchivo));
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar la foto", e);
            }
            AnimalFoto animalFoto = AnimalFoto.builder()
                    .animal(animal)
                    .nombreArchivo(nombreArchivo)
                    .build();
            animalFotoRepository.save(animalFoto);
            animal.getFotos().add(animalFoto);
        }
    }

    private AnimalResponse toResponse(Animal animal) {
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
                .categoria(animal.getCategoria())
                .tipo(animal.getTipo())
                .estado(animal.getEstado())
                .descripcion(animal.getDescripcion())
                .direccion(animal.getDireccion())
                .latitud(animal.getLatitud())
                .longitud(animal.getLongitud())
                .enPosesionDelPublicador(animal.getEnPosesionDelPublicador())
                .provincia(animal.getProvincia() != null ? animal.getProvincia() : animal.getPublicador().getProvincia())
                .ciudad(animal.getCiudad() != null ? animal.getCiudad() : animal.getPublicador().getCiudad())
                .fechaAvistamiento(animal.getFechaAvistamiento())
                .rescatistaNombre(animal.getPublicador().getNombre() + " " + animal.getPublicador().getApellido())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .rechazado(animal.isRechazado())
                .motivoRechazo(animal.getMotivoRechazo())
                .creadoEn(animal.getCreadoEn())
                .build();
    }

    // solo fotos aprobadas, para vista pública
    private AnimalResponse toPublicResponse(Animal animal) {
        List<FotoResponse> fotos = animal.getFotos().stream()
                .filter(f -> f.getEstado() == EstadoFoto.APROBADA)
                .map(f -> FotoResponse.builder()
                        .id(f.getId())
                        .url("/uploads/" + f.getNombreArchivo())
                        .estado(f.getEstado())
                        .build())
                .toList();
        return AnimalResponse.builder()
                .id(animal.getId())
                .categoria(animal.getCategoria())
                .tipo(animal.getTipo())
                .estado(animal.getEstado())
                .descripcion(animal.getDescripcion())
                .direccion(animal.getDireccion())
                .latitud(animal.getLatitud())
                .longitud(animal.getLongitud())
                .enPosesionDelPublicador(animal.getEnPosesionDelPublicador())
                .provincia(animal.getProvincia() != null ? animal.getProvincia() : animal.getPublicador().getProvincia())
                .ciudad(animal.getCiudad() != null ? animal.getCiudad() : animal.getPublicador().getCiudad())
                .fechaAvistamiento(animal.getFechaAvistamiento())
                .rescatistaNombre(animal.getPublicador().getNombre() + " " + animal.getPublicador().getApellido())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .creadoEn(animal.getCreadoEn())
                .build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
