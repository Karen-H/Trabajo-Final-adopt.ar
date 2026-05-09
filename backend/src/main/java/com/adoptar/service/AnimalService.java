package com.adoptar.service;

import com.adoptar.dto.request.AnimalRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final AnimalFotoRepository animalFotoRepository;

    @Value("${uploads.path}")
    private String uploadsPath;

    private static final Set<String> TIPOS_IMAGEN = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadsPath));
    }

    @Transactional
    public AnimalResponse crearAnimal(User rescatista, AnimalRequest request, List<MultipartFile> fotos) {
        if (rescatista.getProvincia() == null || rescatista.getCiudad() == null) {
            throw new IllegalArgumentException("Debés configurar tu provincia y ciudad en el perfil antes de publicar un animal");
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

        Animal animal = Animal.builder()
                .nombre(request.getNombre())
                .sexo(request.getSexo())
                .edad(request.getEdad())
                .tipo(request.getTipo())
                .tipoAdopcion(request.getTipoAdopcion())
                .amigableConGatos(request.isAmigableConGatos())
                .amigableConPerros(request.isAmigableConPerros())
                .amigableConNinos(request.isAmigableConNinos())
                .descripcion(request.getDescripcion())
                .provincia(rescatista.getProvincia())
                .ciudad(rescatista.getCiudad())
                .rescatista(rescatista)
                .build();

        animalRepository.save(animal);

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

        return toResponse(animal);
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getMisAnimales(User rescatista) {
        return animalRepository.findByRescatista(rescatista)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnimalResponse cambiarEstado(Long animalId, User rescatista, EstadoAnimal estado) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (!animal.getRescatista().getId().equals(rescatista.getId())) {
            throw new IllegalArgumentException("No tenés permiso para modificar este animal");
        }
        animal.setEstado(estado);
        animalRepository.save(animal);
        return toResponse(animal);
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
                .provincia(animal.getProvincia())
                .ciudad(animal.getCiudad())
                .rescatistaNombre(animal.getRescatista().getNombre() + " " + animal.getRescatista().getApellido())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .rechazado(animal.isRechazado())
                .motivoRechazo(animal.getMotivoRechazo())
                .creadoEn(animal.getCreadoEn())
                .build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
