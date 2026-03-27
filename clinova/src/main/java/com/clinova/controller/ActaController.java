package com.clinova.controller;

import com.clinova.dto.ActaDTO;
import com.clinova.service.ActaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actas")
@RequiredArgsConstructor
public class ActaController {

    private final ActaService actaService;

    @PostMapping
    public ResponseEntity<ActaDTO> crearActa(@RequestBody ActaDTO request) {
        return new ResponseEntity<>(actaService.crearActa(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ActaDTO>> obtenerTodas() {
        return ResponseEntity.ok(actaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(actaService.obtenerPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarActa(@PathVariable Long id) {
        actaService.eliminarActa(id);
        return ResponseEntity.noContent().build();
    }
}