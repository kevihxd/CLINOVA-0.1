package com.clinova.controller;

import com.clinova.entity.Sede;
import com.clinova.service.SedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/sedes", "/api/sedes"})
@RequiredArgsConstructor
public class SedeController {

    private final SedeService sedeService;

    @GetMapping
    public ResponseEntity<List<Sede>> listarTodas() {
        return ResponseEntity.ok(sedeService.obtenerTodas());
    }

    @PostMapping
    public ResponseEntity<Sede> crear(@RequestBody Sede sede) {
        return ResponseEntity.ok(sedeService.crear(sede));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sede> actualizar(@PathVariable Long id, @RequestBody Sede sede) {
        return ResponseEntity.ok(sedeService.actualizar(id, sede));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        sedeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
