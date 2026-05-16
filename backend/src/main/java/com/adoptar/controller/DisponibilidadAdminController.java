package com.adoptar.controller;

import com.adoptar.dto.request.DisponibilidadAdminRequest;
import com.adoptar.entity.User;
import com.adoptar.service.DisponibilidadAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadAdminController {

    private final DisponibilidadAdminService disponibilidadService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listar(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(disponibilidadService.listarPropias(admin));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> agregar(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody DisponibilidadAdminRequest request) {
        try {
            return ResponseEntity.ok(disponibilidadService.agregar(request, admin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        try {
            disponibilidadService.eliminar(id, admin);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
