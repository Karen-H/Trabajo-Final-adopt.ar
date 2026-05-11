package com.adoptar.controller;

import com.adoptar.dto.request.AnimalRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.RangoEdad;
import com.adoptar.enums.SexoAnimal;
import com.adoptar.enums.TipoAdopcion;
import com.adoptar.enums.TipoAnimal;
import com.adoptar.enums.UserProfile;
import com.adoptar.service.AnimalService;
import com.adoptar.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/animales")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;
    private final ReporteService reporteService;

    @GetMapping
    public ResponseEntity<List<AnimalResponse>> buscar(
            @RequestParam(required = false) TipoAnimal tipo,
            @RequestParam(required = false) SexoAnimal sexo,
            @RequestParam(required = false) RangoEdad edad,
            @RequestParam(required = false) TipoAdopcion tipoAdopcion,
            @RequestParam(required = false) String provincia) {
        return ResponseEntity.ok(animalService.buscarAnimales(tipo, sexo, edad, tipoAdopcion, provincia));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(animalService.getAnimalById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @AuthenticationPrincipal User user,
            @Valid @ModelAttribute AnimalRequest request,
            @RequestParam("fotos") List<MultipartFile> fotos) {
        if (user.getActiveProfile() != UserProfile.RESCATISTA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(animalService.crearAnimal(user, request, fotos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mis-animales")
    public ResponseEntity<List<AnimalResponse>> getMisAnimales(@AuthenticationPrincipal User user) {
        if (user.getActiveProfile() != UserProfile.RESCATISTA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(animalService.getMisAnimales(user));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam EstadoAnimal estado) {
        if (user.getActiveProfile() != UserProfile.RESCATISTA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(animalService.cambiarEstado(id, user, estado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> agregarFotos(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam("fotos") List<MultipartFile> fotos) {
        if (user.getActiveProfile() != UserProfile.RESCATISTA) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(animalService.agregarFotos(id, user, fotos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            // intenta eliminar como animal de adopción primero, si no como reporte
            animalService.eliminarAnimal(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            try {
                reporteService.eliminarReporte(id, user);
                return ResponseEntity.noContent().build();
            } catch (IllegalArgumentException e2) {
                return ResponseEntity.badRequest().body(e2.getMessage());
            }
        }
    }

    @PostMapping("/{id}/republicar")
    public ResponseEntity<?> republicar(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(animalService.republicarAnimal(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{animalId}/fotos/{fotoId}")
    public ResponseEntity<?> eliminarFoto(
            @AuthenticationPrincipal User user,
            @PathVariable Long animalId,
            @PathVariable Long fotoId) {
        try {
            animalService.eliminarFotoPropia(animalId, fotoId, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
