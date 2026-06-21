package com.adoptar.service;

import com.adoptar.dto.request.DenunciaRequest;
import com.adoptar.dto.response.DenunciaResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.Denuncia;
import com.adoptar.entity.User;
import com.adoptar.enums.TipoNotificacion;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.DenunciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final AnimalRepository animalRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public void denunciar(Long animalId, User usuario, DenunciaRequest request) {
        if (denunciaRepository.existsByAnimalIdAndDenuncianteId(animalId, usuario.getId())) {
            throw new IllegalArgumentException("Ya denunciaste esta publicación");
        }
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Publicación no encontrada"));
        if (animal.getPublicador().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("No podés reportar tu propia publicación");
        }
        if (animal.isEliminado()) {
            throw new IllegalArgumentException("La publicación ya no está disponible");
        }
        Denuncia denuncia = Denuncia.builder()
                .animal(animal)
                .denunciante(usuario)
                .razon(request.getRazon())
                .descripcion(request.getDescripcion())
                .build();
        denunciaRepository.save(denuncia);
        notificacionService.crearParaAdminsYMods(
                TipoNotificacion.NUEVA_DENUNCIA,
                "Nueva denuncia sobre una publicación de " + animal.getPublicador().getNombre() + " " + animal.getPublicador().getApellido(),
                "/admin");
    }

    public List<DenunciaResponse> listarPendientes() {
        return denunciaRepository.findByResueltoFalse().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void desestimar(Long denunciaId) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Denuncia no encontrada"));
        denuncia.setResuelto(true);
        denunciaRepository.save(denuncia);
    }

    @Transactional
    public void eliminarPublicacion(Long denunciaId) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Denuncia no encontrada"));
        Animal animal = denuncia.getAnimal();
        animal.setEliminado(true);
        animal.setEliminadoPorAdmin(true);
        animal.setMotivoEliminacion("Eliminado por denuncia: " + denuncia.getRazon().name());
        animalRepository.save(animal);
        // resolver todas las denuncias pendientes de ese animal
        List<Denuncia> pendientes = denunciaRepository.findByAnimalIdAndResueltoFalse(animal.getId());
        pendientes.forEach(d -> d.setResuelto(true));
        denunciaRepository.saveAll(pendientes);
        notificacionService.crearParaFavoritosDeAnimal(
                animal.getId(),
                TipoNotificacion.ANIMAL_FAVORITO_NO_DISPONIBLE,
                "Un animal en tus favoritos ya no está disponible",
                "/favoritos");
    }

    private DenunciaResponse toResponse(Denuncia d) {
        Animal animal = d.getAnimal();
        User publicador = animal.getPublicador();
        return DenunciaResponse.builder()
                .id(d.getId())
                .razon(d.getRazon())
                .descripcion(d.getDescripcion())
                .creadoEn(d.getCreadoEn())
                .animalId(animal.getId())
                .animalCategoria(animal.getCategoria())
                .animalTipo(animal.getTipo())
                .animalNombre(animal.getNombre())
                .animalEstado(animal.getEstado())
                .publicadorNombre(publicador.getNombre() + " " + publicador.getApellido())
                .denuncianteNombre(d.getDenunciante().getNombre() + " " + d.getDenunciante().getApellido())
                .build();
    }
}
