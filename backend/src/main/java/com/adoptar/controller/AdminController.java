package com.adoptar.controller;

import com.adoptar.dto.request.RechazarRequest;
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

    @GetMapping("/animales/pendientes")
    public ResponseEntity<?> getAnimalesPendientes(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminService.getAnimalesPendientes());
    }

    @PutMapping("/animales/{id}/aprobar")
    public ResponseEntity<?> aprobarAnimal(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(adminService.aprobarAnimal(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/animales/{id}/rechazar")
    public ResponseEntity<?> rechazarAnimal(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RechazarRequest request) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(adminService.rechazarAnimal(id, request.getMotivo()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/fotos/pendientes")
    public ResponseEntity<?> getFotosPendientes(@AuthenticationPrincipal User user) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(adminService.getFotosPendientes());
    }

    @PutMapping("/fotos/{id}/aprobar")
    public ResponseEntity<?> aprobarFoto(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(adminService.aprobarFoto(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/fotos/{id}/rechazar")
    public ResponseEntity<?> rechazarFoto(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RechazarRequest request) {
        if (user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(adminService.rechazarFoto(id, request.getMotivo()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
