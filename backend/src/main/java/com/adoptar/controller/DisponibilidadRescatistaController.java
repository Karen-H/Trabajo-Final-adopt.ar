package com.adoptar.controller;

import com.adoptar.dto.request.DisponibilidadRescatistaRequest;
import com.adoptar.entity.User;
import com.adoptar.service.DisponibilidadRescatistaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disponibilidad-rescatista")
@RequiredArgsConstructor
public class DisponibilidadRescatistaController {

    private final DisponibilidadRescatistaService disponibilidadService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> listarPropia(@AuthenticationPrincipal User rescatista) {
        return ResponseEntity.ok(disponibilidadService.listarPropias(rescatista));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> agregar(
            @AuthenticationPrincipal User rescatista,
            @Valid @RequestBody DisponibilidadRescatistaRequest request) {
        try {
            return ResponseEntity.ok(disponibilidadService.agregar(request, rescatista));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> eliminar(@AuthenticationPrincipal User rescatista, @PathVariable Long id) {
        try {
            disponibilidadService.eliminar(id, rescatista);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // publico: lo consulta el comprador para ver los bloques del rescatista al elegir retiro
    @GetMapping("/{rescatistaId}")
    public ResponseEntity<?> listarDe(@PathVariable Long rescatistaId) {
        return ResponseEntity.ok(disponibilidadService.listarDe(rescatistaId));
    }
}
