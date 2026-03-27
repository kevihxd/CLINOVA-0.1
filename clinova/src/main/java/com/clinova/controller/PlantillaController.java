package com.clinova.controller;

import com.clinova.dto.PlantillaDTO;
import com.clinova.service.PlantillaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plantillas")
@RequiredArgsConstructor
public class PlantillaController {

    private final PlantillaService plantillaService;

    @PostMapping
    public ResponseEntity<PlantillaDTO> crearPlantilla(@RequestBody PlantillaDTO request) {
        return new ResponseEntity<>(plantillaService.crearPlantilla(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PlantillaDTO>> obtenerTodas() {
        return ResponseEntity.ok(plantillaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaService.obtenerPorId(id));
    }
}