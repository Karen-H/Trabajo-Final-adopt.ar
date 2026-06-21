package com.adoptar.controller;

import com.adoptar.dto.request.DenunciaRequest;
import com.adoptar.entity.User;
import com.adoptar.enums.UserRole;
import com.adoptar.service.DenunciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/denuncias")
@RequiredArgsConstructor
public class DenunciaController {

    private final DenunciaService denunciaService;

    private boolean noEsModerador(User user) {
        return user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.MODERADOR;
    }

    @PostMapping("/{animalId}")
    public ResponseEntity<?> denunciar(
            @AuthenticationPrincipal User user,
            @PathVariable Long animalId,
            @Valid @RequestBody DenunciaRequest request) {
        if (!noEsModerador(user)) {
            return ResponseEntity.status(403).build();
        }
        try {
            denunciaService.denunciar(animalId, user, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> listarPendientes(@AuthenticationPrincipal User user) {
        if (noEsModerador(user)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(denunciaService.listarPendientes());
    }

    @PatchMapping("/{id}/desestimar")
    public ResponseEntity<?> desestimar(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        if (noEsModerador(user)) {
            return ResponseEntity.status(403).build();
        }
        try {
            denunciaService.desestimar(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/eliminar-publicacion")
    public ResponseEntity<?> eliminarPublicacion(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        if (noEsModerador(user)) {
            return ResponseEntity.status(403).build();
        }
        try {
            denunciaService.eliminarPublicacion(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
