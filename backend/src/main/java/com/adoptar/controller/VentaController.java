package com.adoptar.controller;

import com.adoptar.dto.request.EnvioDomicilioRequest;
import com.adoptar.dto.request.EnvioHorarioRequest;
import com.adoptar.dto.request.EnvioMetodoRequest;
import com.adoptar.dto.request.VentaPreferenciaRequest;
import com.adoptar.entity.User;
import com.adoptar.service.VentaService;
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
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    // crear preferencia de pago con el carrito del comprador para un rescatista
    @PostMapping("/preferencia")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> crearPreferencia(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VentaPreferenciaRequest request) {
        try {
            return ResponseEntity.ok(ventaService.crearPreferencia(user, request.getRescatistaId()));
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

    // ventas pendientes de pago del comprador
    @GetMapping("/pendientes-pago")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getVentasPendientesDePago(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ventaService.getVentasPendientesDePago(user));
    }

    // confirmar pago desde el return URL de MP
    @GetMapping("/confirmar")
    public ResponseEntity<?> confirmarPago(@RequestParam String paymentId) {
        ventaService.confirmarPago(paymentId);
        return ResponseEntity.ok().build();
    }

    // confirmar pago por venta ID (polling del frontend, sin depender del redirect de MP)
    @GetMapping("/confirmar-venta/{ventaId}")
    public ResponseEntity<?> confirmarPorVenta(@PathVariable Long ventaId) {
        String estado = ventaService.confirmarPorVenta(ventaId);
        return ResponseEntity.ok(Map.of("estado", estado));
    }

    // webhook de MercadoPago (publico, llamado por MP)
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> body) {
        ventaService.procesarWebhook(body);
        return ResponseEntity.ok().build();
    }

    // rescatista marca la venta como enviada
    @PostMapping("/{ventaId}/marcar-enviada")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> marcarEnviada(@AuthenticationPrincipal User user, @PathVariable Long ventaId) {
        try {
            ventaService.marcarEnviada(ventaId, user);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // historial de ventas del rescatista autenticado
    @GetMapping("/mis-ventas")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMisVentas(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ventaService.getMisVentas(user));
    }

    // historial de compras del comprador autenticado
    @GetMapping("/mis-compras")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMisCompras(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ventaService.getMisCompras(user));
    }

    // bot de envio (lo usa el comprador desde el chat)

    @GetMapping("/envio-pendiente/{rescatistaId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getEnvioPendiente(@AuthenticationPrincipal User user, @PathVariable Long rescatistaId) {
        var pendiente = ventaService.getEnvioPendiente(user, rescatistaId);
        return pendiente != null ? ResponseEntity.ok(pendiente) : ResponseEntity.noContent().build();
    }

    @PostMapping("/{ventaId}/envio/metodo")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> elegirMetodoEnvio(
            @AuthenticationPrincipal User user,
            @PathVariable Long ventaId,
            @Valid @RequestBody EnvioMetodoRequest request) {
        try {
            return ResponseEntity.ok(ventaService.elegirMetodoEnvio(ventaId, user, request.getMetodo()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{ventaId}/envio/volver")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> volverAElegirMetodo(@AuthenticationPrincipal User user, @PathVariable Long ventaId) {
        try {
            return ResponseEntity.ok(ventaService.volverAElegirMetodo(ventaId, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{ventaId}/envio/horario")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> elegirHorarioRetiro(
            @AuthenticationPrincipal User user,
            @PathVariable Long ventaId,
            @Valid @RequestBody EnvioHorarioRequest request) {
        try {
            return ResponseEntity.ok(ventaService.elegirHorarioRetiro(ventaId, user, request.getBloqueId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{ventaId}/envio/domicilio")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> completarDomicilio(
            @AuthenticationPrincipal User user,
            @PathVariable Long ventaId,
            @Valid @RequestBody EnvioDomicilioRequest request) {
        try {
            return ResponseEntity.ok(ventaService.completarDomicilio(ventaId, user, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
