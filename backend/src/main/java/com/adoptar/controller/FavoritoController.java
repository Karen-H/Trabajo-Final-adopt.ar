package com.adoptar.controller;

import com.adoptar.entity.User;
import com.adoptar.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    @GetMapping
    public ResponseEntity<?> listar(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(favoritoService.listar(user));
    }

    @PostMapping("/{animalId}")
    public ResponseEntity<?> agregar(
            @AuthenticationPrincipal User user,
            @PathVariable Long animalId) {
        try {
            favoritoService.agregar(animalId, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{animalId}")
    public ResponseEntity<?> quitar(
            @AuthenticationPrincipal User user,
            @PathVariable Long animalId) {
        favoritoService.quitar(animalId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{animalId}/es-favorito")
    public ResponseEntity<Boolean> esFavorito(
            @AuthenticationPrincipal User user,
            @PathVariable Long animalId) {
        return ResponseEntity.ok(favoritoService.esFavorito(animalId, user));
    }
}
