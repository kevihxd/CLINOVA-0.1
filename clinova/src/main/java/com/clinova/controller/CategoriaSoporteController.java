package com.clinova.controller;

import com.clinova.dto.CategoriaSoporteDTO;
import com.clinova.service.CategoriaSoporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias-soportes")
@RequiredArgsConstructor
public class CategoriaSoporteController {

    private final CategoriaSoporteService categoriaSoporteService;

    @GetMapping
    public ResponseEntity<List<CategoriaSoporteDTO>> obtenerTodas() {
        return ResponseEntity.ok(categoriaSoporteService.obtenerTodas());
    }

    @PostMapping
    public ResponseEntity<CategoriaSoporteDTO> crear(@RequestBody CategoriaSoporteDTO dto) {
        return ResponseEntity.ok(categoriaSoporteService.crear(dto));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        categoriaSoporteService.eliminarPorNombre(nombre);
        return ResponseEntity.ok().build();
    }
}