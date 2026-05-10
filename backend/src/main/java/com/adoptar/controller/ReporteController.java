package com.adoptar.controller;

import com.adoptar.dto.request.ReporteRequest;
import com.adoptar.dto.response.AnimalResponse;
import com.adoptar.entity.User;
import com.adoptar.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @AuthenticationPrincipal User user,
            @Valid @ModelAttribute ReporteRequest request,
            @RequestParam("fotos") List<MultipartFile> fotos) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(reporteService.crearReporte(user, request, fotos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/perdidos")
    public ResponseEntity<List<AnimalResponse>> getPerdidos() {
        return ResponseEntity.ok(reporteService.getPerdidos());
    }

    @GetMapping("/encontrados")
    public ResponseEntity<List<AnimalResponse>> getEncontrados() {
        return ResponseEntity.ok(reporteService.getEncontrados());
    }

    @GetMapping("/mis-reportes")
    public ResponseEntity<List<AnimalResponse>> getMisReportes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reporteService.getMisReportes(user));
    }

    @PutMapping("/{id}/resolver")
    public ResponseEntity<?> resolver(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(reporteService.resolver(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
