package com.adoptar.controller;

import com.adoptar.dto.request.CarritoActualizarRequest;
import com.adoptar.dto.request.CarritoAgregarRequest;
import com.adoptar.entity.User;
import com.adoptar.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// el carrito es uso normal de la plataforma
@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public ResponseEntity<?> listar(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(carritoService.listar(user));
    }

    @PostMapping("/items")
    public ResponseEntity<?> agregar(@AuthenticationPrincipal User user,
                                      @Valid @RequestBody CarritoAgregarRequest request) {
        try {
            carritoService.agregar(user, request.getItemId(), request.getCantidad());
            return ResponseEntity.ok(carritoService.listar(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> actualizar(@AuthenticationPrincipal User user,
                                         @PathVariable Long itemId,
                                         @Valid @RequestBody CarritoActualizarRequest request) {
        try {
            carritoService.actualizarCantidad(user, itemId, request.getCantidad());
            return ResponseEntity.ok(carritoService.listar(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> eliminar(@AuthenticationPrincipal User user,
                                       @PathVariable Long itemId) {
        carritoService.eliminar(user, itemId);
        return ResponseEntity.ok(carritoService.listar(user));
    }
}
