package com.adoptar.controller;

import com.adoptar.dto.request.ConfigurarDonacionRequest;
import com.adoptar.dto.request.DonacionRequest;
import com.adoptar.entity.User;
import com.adoptar.enums.UserProfile;
import com.adoptar.enums.UserRole;
import com.adoptar.service.DonacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/donaciones")
@RequiredArgsConstructor
public class DonacionController {

    private final DonacionService donacionService;

    // listado publico de rescatistas que aceptan donaciones
    @GetMapping("/rescatistas")
    public ResponseEntity<?> listarRescatistas(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String provincia,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(donacionService.listarRescatistas(provincia, q, user));
    }

    // configurar si acepta donaciones y descripcion
    @PutMapping("/configurar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> configurar(
            @AuthenticationPrincipal User user,
            @RequestBody ConfigurarDonacionRequest request) {
        try {
            donacionService.configurar(user, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // crear preferencia de pago (publica, con auth opcional; admin y moderador no pueden donar)
    @PostMapping("/preferencia")
    public ResponseEntity<?> crearPreferencia(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DonacionRequest request) {
        if (user != null && user.getRole() != UserRole.USER) {
            return ResponseEntity.status(403).build();
        }
        if (user != null && user.getActiveProfile() != UserProfile.ADOPTANTE) {
            return ResponseEntity.badRequest().body("Solo podés donar con el perfil de adoptante activo");
        }
        try {
            return ResponseEntity.ok(donacionService.crearPreferencia(request, user));
        } catch (com.mercadopago.exceptions.MPApiException e) {
            log.error("Error MP API - status: {}, body: {}", e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
            return ResponseEntity.internalServerError().body("Error al crear la preferencia de pago");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error al crear preferencia MP: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error al crear la preferencia de pago");
        }
    }

    // confirmar pago desde el return URL de MP
    @GetMapping("/confirmar")
    public ResponseEntity<?> confirmarPago(@RequestParam String paymentId) {
        donacionService.confirmarPago(paymentId);
        return ResponseEntity.ok().build();
    }

    // confirmar pago por donacion ID (sin depender del redirect de MP)
    @GetMapping("/confirmar-donacion/{donacionId}")
    public ResponseEntity<?> confirmarPorDonacion(@PathVariable Long donacionId) {
        String estado = donacionService.confirmarPorDonacion(donacionId);
        return ResponseEntity.ok(Map.of("estado", estado));
    }

    // webhook de MercadoPago (publico, llamado por MP)
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> body) {
        donacionService.procesarWebhook(body);
        return ResponseEntity.ok().build();
    }

    // historial de donaciones recibidas (solo el rescatista autenticado)
    @GetMapping("/mis-donaciones")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMisDonaciones(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(donacionService.getMisDonaciones(user));
    }
}
