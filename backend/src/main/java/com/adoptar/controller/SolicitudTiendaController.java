package com.adoptar.controller;

import com.adoptar.dto.request.AceptarSolicitudRequest;
import com.adoptar.dto.request.ReprogramarSolicitudRequest;
import com.adoptar.dto.request.SolicitudTiendaRequest;
import com.adoptar.entity.User;
import com.adoptar.service.DisponibilidadAdminService;
import com.adoptar.service.SolicitudTiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tienda")
@RequiredArgsConstructor
public class SolicitudTiendaController {

    private final SolicitudTiendaService solicitudService;
    private final DisponibilidadAdminService disponibilidadService;

    // slots disponibles: cualquier usuario autenticado puede verlos para elegir
    @GetMapping("/slots")
    public ResponseEntity<?> getSlots() {
        return ResponseEntity.ok(disponibilidadService.getSlotsDisponibles());
    }

    // rescatista: crear solicitud
    @PostMapping("/solicitud")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> crear(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SolicitudTiendaRequest request) {
        try {
            return ResponseEntity.ok(solicitudService.crear(request, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // rescatista: ver su solicitud actual
    @GetMapping("/solicitud")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMiSolicitud(@AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(solicitudService.getMiSolicitud(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // rescatista: editar slot (solo si PENDIENTE)
    @PutMapping("/solicitud")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> editar(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SolicitudTiendaRequest request) {
        try {
            return ResponseEntity.ok(solicitudService.editar(request, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // rescatista: cancelar (solo si PENDIENTE)
    @DeleteMapping("/solicitud")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> cancelar(@AuthenticationPrincipal User user) {
        try {
            solicitudService.cancelar(user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // rescatista: elegir nuevo slot tras reprogramacion
    @PutMapping("/solicitud/reprogramar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> reprogramarComoRescatista(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SolicitudTiendaRequest request) {
        try {
            return ResponseEntity.ok(solicitudService.reprogramarComoRescatista(request, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
