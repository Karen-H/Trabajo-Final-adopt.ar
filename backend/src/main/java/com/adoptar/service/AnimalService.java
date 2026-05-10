package com.adoptar.service;

import com.adoptar.dto.request.AnimalRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.User;
import com.adoptar.enums.CategoriaAnimal;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.enums.RangoEdad;
import com.adoptar.enums.SexoAnimal;
import com.adoptar.enums.TipoAdopcion;
import com.adoptar.enums.TipoAnimal;
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
    public AnimalResponse crearAnimal(User publicador, AnimalRequest request, List<MultipartFile> fotos) {
        if (publicador.getProvincia() == null || publicador.getCiudad() == null) {
            throw new IllegalArgumentException("Debes configurar tu provincia y ciudad en el perfil antes de publicar un animal");
        }
        if (fotos == null || fotos.isEmpty()) {
            throw new IllegalArgumentException("Debes subir al menos una foto");
        }
        if (fotos.size() > 5) {
            throw new IllegalArgumentException("No podes subir mas de 5 fotos");
        }
        for (MultipartFile foto : fotos) {
            String contentType = foto.getContentType();
            if (contentType == null || !TIPOS_IMAGEN.contains(contentType)) {
                throw new IllegalArgumentException("Solo se aceptan imagenes (jpg, png, webp, gif)");
            }
        }

        Animal animal = Animal.builder()
                .categoria(CategoriaAnimal.ADOPCION)
                .nombre(request.getNombre())
                .sexo(request.getSexo())
                .edad(request.getEdad())
                .tipo(request.getTipo())
                .tipoAdopcion(request.getTipoAdopcion())
                .estado(EstadoAnimal.EN_ADOPCION)
                .amigableConGatos(request.isAmigableConGatos())
                .amigableConPerros(request.isAmigableConPerros())
                .amigableConNinos(request.isAmigableConNinos())
                .descripcion(request.getDescripcion())
                .direccion(request.getDireccion())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .publicador(publicador)
                .build();

        animalRepository.save(animal);
        guardarFotos(animal, fotos);
        return toResponse(animal);
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> getMisAnimales(User publicador) {
        return animalRepository.findByPublicador(publicador)
                .stream()
                .filter(a -> a.getCategoria() == CategoriaAnimal.ADOPCION)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnimalResponse cambiarEstado(Long animalId, User publicador, EstadoAnimal estado) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (!animal.getPublicador().getId().equals(publicador.getId())) {
            throw new IllegalArgumentException("No tenes permiso para modificar este animal");
        }
        if (animal.getCategoria() != CategoriaAnimal.ADOPCION) {
            throw new IllegalArgumentException("Usa el endpoint de reportes para modificar este animal");
        }
        if (estado != EstadoAnimal.EN_ADOPCION && estado != EstadoAnimal.ADOPTADO) {
            throw new IllegalArgumentException("Estado no valido para un animal en adopcion");
        }
        animal.setEstado(estado);
        animalRepository.save(animal);
        return toResponse(animal);
    }

    @Transactional
    public AnimalResponse agregarFotos(Long animalId, User publicador, List<MultipartFile> fotosNuevas) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (!animal.getPublicador().getId().equals(publicador.getId())) {
            throw new IllegalArgumentException("No tenes permiso para modificar este animal");
        }
        if (animal.isRechazado()) {
            throw new IllegalArgumentException("No podes agregar fotos a un animal rechazado");
        }
        if (fotosNuevas == null || fotosNuevas.isEmpty()) {
            throw new IllegalArgumentException("Debes subir al menos una foto");
        }
        if (animal.getFotos().size() + fotosNuevas.size() > 5) {
            throw new IllegalArgumentException("No podes tener mas de 5 fotos por animal");
        }
        for (MultipartFile foto : fotosNuevas) {
            String contentType = foto.getContentType();
            if (contentType == null || !TIPOS_IMAGEN.contains(contentType)) {
                throw new IllegalArgumentException("Solo se aceptan imagenes (jpg, png, webp, gif)");
            }
        }
        guardarFotos(animal, fotosNuevas);
        return toResponse(animal);
    }

    @Transactional(readOnly = true)
    public List<AnimalResponse> buscarAnimales(TipoAnimal tipo, SexoAnimal sexo, RangoEdad edad,
                                               TipoAdopcion tipoAdopcion, String provincia) {
        return animalRepository.buscarAdopcionAprobados(tipo, sexo, edad, tipoAdopcion, provincia)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnimalResponse getAnimalById(Long id) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (!animal.isAprobado() || animal.getCategoria() != CategoriaAnimal.ADOPCION) {
            throw new IllegalArgumentException("Animal no encontrado");
        }
        return toPublicResponse(animal);
    }

    @Transactional(readOnly = true)
    public List<Animal> getPendientesAdmin() {
        return animalRepository.findByCategoriaAndAprobadoFalseAndRechazadoFalse(CategoriaAnimal.ADOPCION);
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
                .nombre(animal.getNombre())
                .sexo(animal.getSexo())
                .edad(animal.getEdad())
                .tipo(animal.getTipo())
                .tipoAdopcion(animal.getTipoAdopcion())
                .estado(animal.getEstado())
                .amigableConGatos(animal.getAmigableConGatos())
                .amigableConPerros(animal.getAmigableConPerros())
                .amigableConNinos(animal.getAmigableConNinos())
                .descripcion(animal.getDescripcion())
                .direccion(animal.getDireccion())
                .latitud(animal.getLatitud())
                .longitud(animal.getLongitud())
                .enPosesionDelPublicador(animal.getEnPosesionDelPublicador())
                .provincia(animal.getPublicador().getProvincia())
                .ciudad(animal.getPublicador().getCiudad())
                .rescatistaNombre(animal.getPublicador().getNombre() + " " + animal.getPublicador().getApellido())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .rechazado(animal.isRechazado())
                .motivoRechazo(animal.getMotivoRechazo())
                .creadoEn(animal.getCreadoEn())
                .build();
    }

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
                .nombre(animal.getNombre())
                .sexo(animal.getSexo())
                .edad(animal.getEdad())
                .tipo(animal.getTipo())
                .tipoAdopcion(animal.getTipoAdopcion())
                .estado(animal.getEstado())
                .amigableConGatos(animal.getAmigableConGatos())
                .amigableConPerros(animal.getAmigableConPerros())
                .amigableConNinos(animal.getAmigableConNinos())
                .descripcion(animal.getDescripcion())
                .provincia(animal.getPublicador().getProvincia())
                .ciudad(animal.getPublicador().getCiudad())
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
