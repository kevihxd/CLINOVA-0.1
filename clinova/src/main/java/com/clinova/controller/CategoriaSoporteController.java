package com.clinova.controller;

import com.clinova.dto.CategoriaSoporteDTO;
import com.clinova.service.CategoriaSoporteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/categorias-soportes", "/api/categorias-soportes"})
@RequiredArgsConstructor
public class CategoriaSoporteController {

    private final CategoriaSoporteService categoriaSoporteService;

    @Data
    public static class VisibilidadRequestDTO {
        private Boolean visible;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaSoporteDTO>> obtenerTodas() {
        return ResponseEntity.ok(categoriaSoporteService.obtenerTodas());
    }

    @PostMapping
    public ResponseEntity<CategoriaSoporteDTO> crear(@RequestBody CategoriaSoporteDTO dto) {
        return ResponseEntity.ok(categoriaSoporteService.crear(dto));
    }

    @PutMapping("/{id}/visibilidad")
    public ResponseEntity<CategoriaSoporteDTO> cambiarVisibilidad(
            @PathVariable Long id,
            @RequestBody(required = false) VisibilidadRequestDTO request
    ) {
        Boolean visible = request != null ? request.getVisible() : null;
        return ResponseEntity.ok(categoriaSoporteService.toggleVisibilidad(id, visible));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        categoriaSoporteService.eliminarPorNombre(nombre);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<Void> eliminarPorId(@PathVariable Long id) {
        categoriaSoporteService.eliminarPorId(id);
        return ResponseEntity.ok().build();
    }
}