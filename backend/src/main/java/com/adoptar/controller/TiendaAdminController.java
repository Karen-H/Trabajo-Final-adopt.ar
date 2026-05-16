package com.adoptar.controller;

import com.adoptar.dto.request.AceptarSolicitudRequest;
import com.adoptar.dto.request.RechazarRequest;
import com.adoptar.dto.request.ReprogramarSolicitudRequest;
import com.adoptar.entity.User;
import com.adoptar.service.SolicitudTiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tienda")
@RequiredArgsConstructor
public class TiendaAdminController {

    private final SolicitudTiendaService solicitudService;

    // ver todas las solicitudes
    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(solicitudService.listarTodas());
    }

    // aceptar la llamada + completar link
    @PutMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aceptar(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody AceptarSolicitudRequest request) {
        try {
            return ResponseEntity.ok(solicitudService.aceptar(id, request, admin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // editar solo el link de llamada
    @PutMapping("/{id}/link")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editarLink(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody AceptarSolicitudRequest request) {
        try {
            return ResponseEntity.ok(solicitudService.editarLink(id, request, admin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // aprobar la tienda tras la llamada
    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aprobar(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(solicitudService.aprobar(id, admin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // rechazar la solicitud + motivo + bloqueo 1 mes
    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rechazar(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody RechazarRequest request) {
        try {
            return ResponseEntity.ok(solicitudService.rechazar(id, request.getMotivo(), admin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // reprogramar: el admin marca que hay que volver a coordinar
    @PutMapping("/{id}/reprogramar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reprogramar(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody ReprogramarSolicitudRequest request) {
        try {
            return ResponseEntity.ok(solicitudService.reprogramar(id, request, admin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
