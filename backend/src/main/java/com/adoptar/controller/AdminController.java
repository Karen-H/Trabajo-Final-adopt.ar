package com.adoptar.controller;

import com.adoptar.dto.request.RechazarRequest;
import com.adoptar.dto.request.ActualizarRolRequest;
import com.adoptar.entity.User;
import com.adoptar.enums.UserRole;
import com.adoptar.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // animales/fotos/items/tiendas son moderacion de contenido: admin y moderador
    private boolean noEsModerador(User user) {
        return user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.MODERADOR;
    }

    @DeleteMapping("/animales/{id}")
    public ResponseEntity<?> eliminarAnimal(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RechazarRequest request) {
        if (noEsModerador(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            adminService.eliminarAnimal(id, request.getMotivo());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/fotos/{id}")
    public ResponseEntity<?> eliminarFoto(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RechazarRequest request) {
        if (noEsModerador(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            adminService.eliminarFoto(id, request.getMotivo());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // tiendas activas

    @GetMapping("/tiendas/activas")
    public ResponseEntity<?> listarTiendasActivas(@AuthenticationPrincipal User user) {
        if (noEsModerador(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminService.listarTiendasActivas());
    }

    @DeleteMapping("/tiendas/{usuarioId}")
    public ResponseEntity<?> revocarTienda(@AuthenticationPrincipal User user, @PathVariable Long usuarioId) {
        if (noEsModerador(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            adminService.revocarTienda(usuarioId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // usuarios

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (user.getId().equals(id)) {
            return ResponseEntity.badRequest().body("No podés eliminar tu propia cuenta");
        }
        try {
            adminService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/usuarios/{id}/rol")
    public ResponseEntity<?> actualizarRol(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRolRequest request) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (user.getId().equals(id)) {
            return ResponseEntity.badRequest().body("No podés cambiar tu propio rol");
        }
        try {
            return ResponseEntity.ok(adminService.actualizarRol(id, request.getRol()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // dashboard

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminService.getDashboardStats());
    }
}
