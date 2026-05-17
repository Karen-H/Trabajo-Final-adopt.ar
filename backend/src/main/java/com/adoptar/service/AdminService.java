package com.adoptar.service;

import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.dto.response.FotoPendienteResponse;
import com.adoptar.dto.response.FotoResponse;
import com.adoptar.dto.response.ItemFotoPendienteResponse;
import com.adoptar.dto.response.ItemTiendaResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.AnimalFoto;
import com.adoptar.entity.ItemFoto;
import com.adoptar.entity.ItemTienda;
import com.adoptar.enums.CategoriaAnimal;
import com.adoptar.enums.EstadoFoto;
import com.adoptar.enums.EstadoItem;
import com.adoptar.repository.AnimalFotoRepository;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.ItemFotoRepository;
import com.adoptar.repository.ItemTiendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AnimalRepository animalRepository;
    private final AnimalFotoRepository animalFotoRepository;
    private final ItemTiendaRepository itemTiendaRepository;
    private final ItemFotoRepository itemFotoRepository;
    private final ItemTiendaService itemTiendaService;

    @Transactional(readOnly = true)
    public List<AnimalResponse> getAnimalesPendientes() {
        List<Animal> pendientes = new ArrayList<>();
        pendientes.addAll(animalRepository.findByCategoriaAndAprobadoFalseAndRechazadoFalseAndEliminadoFalse(CategoriaAnimal.ADOPCION));
        pendientes.addAll(animalRepository.findByCategoriaAndAprobadoFalseAndRechazadoFalseAndEliminadoFalse(CategoriaAnimal.PERDIDO_ENCONTRADO));
        return pendientes.stream()
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

    @Transactional(readOnly = true)
    public List<AnimalResponse> getPublicaciones() {
        return animalRepository.findByAprobadoTrueAndEliminadoFalse()
                .stream()
                .map(this::toAnimalResponse)
                .toList();
    }

    @Transactional
    public void eliminarAnimal(Long id, String motivo) {
        Animal animal = animalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animal no encontrado"));
        if (animal.isEliminado()) {
            throw new IllegalArgumentException("El animal ya fue eliminado");
        }
        animal.setEliminado(true);
        animal.setEliminadoPorAdmin(true);
        animal.setMotivoEliminacion(motivo);
        animalRepository.save(animal);
    }

    @Transactional
    public void eliminarFoto(Long id, String motivo) {
        AnimalFoto foto = animalFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        foto.setEstado(EstadoFoto.ELIMINADA);
        foto.setMotivoRechazo(motivo);
        animalFotoRepository.save(foto);
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
                .enPosesionDelPublicador(animal.getEnPosesionDelPublicador())
                .provincia(animal.getPublicador().getProvincia())
                .ciudad(animal.getPublicador().getCiudad())
                .rescatistaNombre(animal.getPublicador().getNombre() + " " + animal.getPublicador().getApellido())
                .fotos(fotos)
                .aprobado(animal.isAprobado())
                .rechazado(animal.isRechazado())
                .motivoRechazo(animal.getMotivoRechazo())
                .eliminado(animal.isEliminado())
                .eliminadoPorAdmin(animal.isEliminadoPorAdmin())
                .motivoEliminacion(animal.getMotivoEliminacion())
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
                .animalCategoria(foto.getAnimal().getCategoria().name())
                .animalEstado(foto.getAnimal().getEstado().name())
                .rescatistaNombre(foto.getAnimal().getPublicador().getNombre() + " " + foto.getAnimal().getPublicador().getApellido())
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

    // --- items de tienda ---

    @Transactional(readOnly = true)
    public List<ItemTiendaResponse> getItemsPendientes() {
        return itemTiendaRepository.findByEstadoAndEliminadoFalse(EstadoItem.PENDIENTE)
                .stream()
                .map(itemTiendaService::toResponse)
                .toList();
    }

    @Transactional
    public ItemTiendaResponse aprobarItem(Long id) {
        ItemTienda item = itemTiendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        if (item.getEstado() == EstadoItem.APROBADO) {
            throw new IllegalArgumentException("El ítem ya está aprobado");
        }
        item.setEstado(EstadoItem.APROBADO);
        item.setMotivoRechazo(null);
        item.getFotos().forEach(f -> {
            if (f.getEstado() == EstadoFoto.PENDIENTE) {
                f.setEstado(EstadoFoto.APROBADA);
            }
        });
        itemTiendaRepository.save(item);
        return itemTiendaService.toResponse(item);
    }

    @Transactional
    public ItemTiendaResponse rechazarItem(Long id, String motivo) {
        ItemTienda item = itemTiendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ítem no encontrado"));
        if (item.getEstado() == EstadoItem.APROBADO) {
            throw new IllegalArgumentException("El ítem ya fue aprobado, no puede rechazarse");
        }
        item.setEstado(EstadoItem.RECHAZADO);
        item.setMotivoRechazo(motivo);
        itemTiendaRepository.save(item);
        return itemTiendaService.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemFotoPendienteResponse> getFotosItemPendientes() {
        return itemFotoRepository.findByEstadoAndItem_EstadoAndItem_EliminadoFalse(EstadoFoto.PENDIENTE, EstadoItem.APROBADO)
                .stream()
                .map(f -> ItemFotoPendienteResponse.builder()
                        .id(f.getId())
                        .url("/uploads/" + f.getNombreArchivo())
                        .itemId(f.getItem().getId())
                        .itemTitulo(f.getItem().getTitulo())
                        .itemTipo(f.getItem().getTipo().name())
                        .rescatistaNombre(f.getItem().getRescatista().getNombre() + " " + f.getItem().getRescatista().getApellido())
                        .build())
                .toList();
    }

    @Transactional
    public FotoResponse aprobarFotoItem(Long id) {
        ItemFoto foto = itemFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (foto.getItem().getEstado() != EstadoItem.APROBADO) {
            throw new IllegalArgumentException("Esta foto pertenece a un ítem que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.APROBADA);
        foto.setMotivoRechazo(null);
        itemFotoRepository.save(foto);
        return FotoResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .estado(foto.getEstado())
                .build();
    }

    @Transactional
    public FotoResponse rechazarFotoItem(Long id, String motivo) {
        ItemFoto foto = itemFotoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Foto no encontrada"));
        if (foto.getItem().getEstado() != EstadoItem.APROBADO) {
            throw new IllegalArgumentException("Esta foto pertenece a un ítem que aún no fue aprobado");
        }
        foto.setEstado(EstadoFoto.RECHAZADA);
        foto.setMotivoRechazo(motivo);
        itemFotoRepository.save(foto);
        return FotoResponse.builder()
                .id(foto.getId())
                .url("/uploads/" + foto.getNombreArchivo())
                .estado(foto.getEstado())
                .motivoRechazo(motivo)
                .build();
    }
}
