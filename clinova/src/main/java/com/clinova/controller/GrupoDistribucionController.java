package com.clinova.controller;

import com.clinova.dto.GrupoDistribucionDTO;
import com.clinova.service.GrupoDistribucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/grupos-distribucion", "/api/grupos-distribucion"})
@RequiredArgsConstructor
public class GrupoDistribucionController {

    private final GrupoDistribucionService grupoService;

    @GetMapping
    public ResponseEntity<List<GrupoDistribucionDTO>> obtenerTodos() {
        return ResponseEntity.ok(grupoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoDistribucionDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(grupoService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<GrupoDistribucionDTO> crear(@RequestBody GrupoDistribucionDTO dto) {
        return ResponseEntity.ok(grupoService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GrupoDistribucionDTO> actualizar(@PathVariable Long id, @RequestBody GrupoDistribucionDTO dto) {
        return ResponseEntity.ok(grupoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        grupoService.eliminar(id);
        return ResponseEntity.ok().build();
    }
}
