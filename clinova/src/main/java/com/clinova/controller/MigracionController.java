package com.clinova.controller;

import com.clinova.service.MigracionHojaVidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/migracion")
@RequiredArgsConstructor
public class MigracionController {

    private final MigracionHojaVidaService migracionHojaVidaService;
    private final com.clinova.service.MigracionIncapacidadesService migracionIncapacidadesService;

    @PostMapping("/hojas-vida/cargar")
    public ResponseEntity<String> migrarHojasDeVida(@RequestParam("file") MultipartFile file) {
        String resultado = migracionHojaVidaService.migrarHojasDeVida(file);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/incapacidades/cargar")
    public ResponseEntity<String> migrarIncapacidades(@RequestParam("file") MultipartFile file) {
        String resultado = migracionIncapacidadesService.migrarIncapacidades(file);
        return ResponseEntity.ok(resultado);
    }
}