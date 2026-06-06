package com.adoptar.controller;

import com.adoptar.dto.request.ConfigurarDonacionRequest;
import com.adoptar.dto.request.DonacionRequest;
import com.adoptar.entity.User;
import com.adoptar.service.DonacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(required = false) String provincia,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(donacionService.listarRescatistas(provincia, q));
    }

    // configurar si acepta donaciones y descripcion
    @PutMapping("/configurar")
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

    // crear preferencia de pago (publica, con auth opcional)
    @PostMapping("/preferencia")
    public ResponseEntity<?> crearPreferencia(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DonacionRequest request) {
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
    public ResponseEntity<?> getMisDonaciones(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(donacionService.getMisDonaciones(user));
    }
}
