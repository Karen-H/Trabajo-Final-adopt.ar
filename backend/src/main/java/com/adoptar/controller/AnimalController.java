package com.adoptar.controller;

import com.adoptar.dto.request.AnimalRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.UserProfile;
import com.adoptar.service.AnimalService;
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
}
