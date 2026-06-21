package com.adoptar.controller;

import com.adoptar.dto.request.ItemTiendaRequest;
import com.adoptar.entity.User;
import com.adoptar.service.ItemTiendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemTiendaController {

    private final ItemTiendaService itemTiendaService;

    // catalogo publico de tiendas con items en venta
    @GetMapping("/tiendas")
    public ResponseEntity<?> listarTiendas(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String provincia,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(itemTiendaService.listarTiendas(provincia, q, user));
    }

    // catalogo publico de items aprobados de una tienda
    @GetMapping("/tiendas/{rescatistaId}")
    public ResponseEntity<?> listarItemsDeTienda(@PathVariable Long rescatistaId) {
        try {
            return ResponseEntity.ok(itemTiendaService.listarItemsDeTienda(rescatistaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // catalogo publico de items aprobados de todos los rescatistas juntos
    @GetMapping("/todos")
    public ResponseEntity<?> listarTodosLosItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String provincia) {
        return ResponseEntity.ok(itemTiendaService.listarTodosLosItems(q, tipo, provincia));
    }

    @GetMapping("/mis-items")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMisItems(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itemTiendaService.getMisItems(user));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> crear(
            @AuthenticationPrincipal User user,
            @Valid @ModelAttribute ItemTiendaRequest request,
            @RequestParam("fotos") List<MultipartFile> fotos) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(itemTiendaService.crear(user, request, fotos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> editar(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ItemTiendaRequest request) {
        try {
            return ResponseEntity.ok(itemTiendaService.editar(id, user, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> agregarFotos(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam("fotos") List<MultipartFile> fotos) {
        try {
            return ResponseEntity.ok(itemTiendaService.agregarFotos(id, user, fotos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> eliminarFoto(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable Long fotoId) {
        try {
            return ResponseEntity.ok(itemTiendaService.eliminarFoto(id, fotoId, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> eliminar(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            itemTiendaService.eliminar(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
