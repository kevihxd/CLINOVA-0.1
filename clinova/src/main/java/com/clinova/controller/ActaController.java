package com.clinova.controller;

import com.clinova.dto.ActaDTO;
import com.clinova.entity.Usuario;
import com.clinova.service.ActaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actas")
@RequiredArgsConstructor
public class ActaController {

    private final ActaService actaService;

    @PostMapping
    public ResponseEntity<ActaDTO> crearActa(@RequestBody ActaDTO actaDTO) {
        return ResponseEntity.ok(actaService.crearActa(actaDTO));
    }

    @GetMapping
    public ResponseEntity<List<ActaDTO>> obtenerTodas() {
        return ResponseEntity.ok(actaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(actaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActaDTO> actualizarActa(
            @PathVariable Long id,
            @RequestBody ActaDTO actaDTO,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        return ResponseEntity.ok(actaService.actualizarActa(id, actaDTO, usuarioAutenticado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarActa(@PathVariable Long id) {
        actaService.eliminarActa(id);
        return ResponseEntity.noContent().build();
    }
}