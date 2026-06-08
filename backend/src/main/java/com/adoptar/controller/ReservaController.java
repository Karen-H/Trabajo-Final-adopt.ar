package com.adoptar.controller;

import com.adoptar.entity.User;
import com.adoptar.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    // rescatista propone reserva
    @PostMapping("/proponer")
    public ResponseEntity<?> proponer(@AuthenticationPrincipal User user,
                                      @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(reservaService.proponer(user, body.get("animalId"), body.get("adoptanteId")));
    }

    // adoptante acepta
    @PostMapping("/{reservaId}/aceptar")
    public ResponseEntity<?> aceptar(@AuthenticationPrincipal User user,
                                     @PathVariable Long reservaId) {
        reservaService.aceptar(user, reservaId);
        return ResponseEntity.ok().build();
    }

    // adoptante rechaza
    @PostMapping("/{reservaId}/rechazar")
    public ResponseEntity<?> rechazar(@AuthenticationPrincipal User user,
                                      @PathVariable Long reservaId) {
        reservaService.rechazar(user, reservaId);
        return ResponseEntity.ok().build();
    }

    // rescatista concreta la adopción
    @PostMapping("/{reservaId}/concretar")
    public ResponseEntity<?> concretar(@AuthenticationPrincipal User user,
                                       @PathVariable Long reservaId) {
        reservaService.concretar(user, reservaId);
        return ResponseEntity.ok().build();
    }

    // rescatista cancela (motivo: NO_CONCRETA, ERROR_RESERVA, PROBLEMA_ANIMAL)
    @PostMapping("/{reservaId}/cancelar")
    public ResponseEntity<?> cancelar(@AuthenticationPrincipal User user,
                                      @PathVariable Long reservaId,
                                      @RequestParam String motivo) {
        reservaService.cancelar(user, reservaId, motivo);
        return ResponseEntity.ok().build();
    }

    // reservas pendientes del adoptante con un rescatista
    @GetMapping("/pendiente")
    public ResponseEntity<?> getPendientes(@AuthenticationPrincipal User user,
                                           @RequestParam Long rescatistaId) {
        return ResponseEntity.ok(reservaService.getReservasPendientes(user, rescatistaId));
    }

    // animales del chat disponibles para reservar (filtrados por lo que el adoptante mencionó)
    @GetMapping("/mis-animales-disponibles")
    public ResponseEntity<?> getMisAnimalesDisponibles(@AuthenticationPrincipal User user,
                                                       @RequestParam Long chatId) {
        return ResponseEntity.ok(reservaService.getAnimalesDisponibles(user, chatId));
    }

    // IDs de animales bloqueados para el adoptante actual
    @GetMapping("/mis-bloqueos")
    public ResponseEntity<?> getMisBloqueos(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.getMisBloqueos(user));
    }

    // rescatista: sus reservas activas
    @GetMapping("/mis-reservas-activas")
    public ResponseEntity<?> getMisReservasActivas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.getMisReservasActivas(user));
    }

    // adoptante: sus reservas activas
    @GetMapping("/mis-reservas-adoptante")
    public ResponseEntity<?> getMisReservasComoAdoptante(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reservaService.getMisReservasComoAdoptante(user));
    }
}
