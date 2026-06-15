package com.adoptar.controller;

import com.adoptar.entity.User;
import com.adoptar.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<?> getMias(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificacionService.getMias(user));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<?> contarNoLeidas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificacionService.contarNoLeidas(user));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<?> marcarLeida(@AuthenticationPrincipal User user, @PathVariable Long id) {
        try {
            notificacionService.marcarLeida(id, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@AuthenticationPrincipal User user, @PathVariable Long id) {
        try {
            notificacionService.eliminar(id, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
