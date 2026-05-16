package com.adoptar.service;

import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.FavoritoResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.Favorito;
import com.adoptar.entity.User;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.FavoritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final AnimalRepository animalRepository;

    @Transactional
    public void agregar(Long animalId, User usuario) {
        if (favoritoRepository.existsByUsuarioIdAndAnimalId(usuario.getId(), animalId)) {
            return; // ya existe, no hacer nada
        }
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        Favorito favorito = Favorito.builder()
                .usuario(usuario)
                .animal(animal)
                .build();
        favoritoRepository.save(favorito);
    }

    @Transactional
    public void quitar(Long animalId, User usuario) {
        favoritoRepository.deleteByUsuarioIdAndAnimalId(usuario.getId(), animalId);
    }

    public List<FavoritoResponse> listar(User usuario) {
        return favoritoRepository.findByUsuarioId(usuario.getId()).stream()
                .map(f -> {
                    Animal animal = f.getAnimal();
                    boolean disponible = !animal.isEliminado() && !animal.isRechazado();
                    return FavoritoResponse.builder()
                            .animalId(animal.getId())
                            .disponible(disponible)
                            .animal(toResponse(animal))
                            .build();
                })
                .toList();
    }

    public boolean esFavorito(Long animalId, User usuario) {
        return favoritoRepository.existsByUsuarioIdAndAnimalId(usuario.getId(), animalId);
    }

    private AnimalResponse toResponse(Animal animal) {
        List<FotoResponse> fotos = animal.getFotos().stream()
                .filter(f -> f.getEstado().name().equals("APROBADA"))
                .map(f -> FotoResponse.builder()
                        .id(f.getId())
                        .url("/uploads/" + f.getNombreArchivo())
                        .estado(f.getEstado())
                        .build())
                .toList();
        return AnimalResponse.builder()
                .id(animal.getId())
                .categoria(animal.getCategoria())
                .nombre(animal.getNombre())
                .tipo(animal.getTipo())
                .sexo(animal.getSexo())
                .edad(animal.getEdad())
                .tipoAdopcion(animal.getTipoAdopcion())
                .estado(animal.getEstado())
                .descripcion(animal.getDescripcion())
                .provincia(animal.getPublicador().getProvincia())
                .ciudad(animal.getPublicador().getCiudad())
                .rescatistaNombre(animal.getPublicador().getNombre() + " " + animal.getPublicador().getApellido())
                .direccion(animal.getDireccion())
                .fechaAvistamiento(animal.getFechaAvistamiento())
                .enPosesionDelPublicador(animal.getEnPosesionDelPublicador())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .rechazado(animal.isRechazado())
                .eliminado(animal.isEliminado())
                .eliminadoPorAdmin(animal.isEliminadoPorAdmin())
                .motivoEliminacion(animal.getMotivoEliminacion())
                .build();
    }
}
