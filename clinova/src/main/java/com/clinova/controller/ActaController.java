package com.clinova.controller;

import com.clinova.dto.ActaDTO;
import com.clinova.dto.StructureResponses;
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
    public StructureResponses<ActaDTO> crearActa(@RequestBody ActaDTO request) {
        ActaDTO response = actaService.crearActa(request);
        return new StructureResponses<>("exito", "Acta creada exitosamente", response);
    }

    @GetMapping
    public StructureResponses<List<ActaDTO>> obtenerTodas() {
        List<ActaDTO> response = actaService.obtenerTodas();
        return new StructureResponses<>("exito", "Actas obtenidas", response);
    }

    @GetMapping("/{id}")
    public StructureResponses<ActaDTO> obtenerPorId(@PathVariable Long id) {
        ActaDTO response = actaService.obtenerPorId(id);
        return new StructureResponses<>("exito", "Acta obtenida", response);
    }

    @DeleteMapping("/{id}")
    public StructureResponses<Void> eliminarActa(@PathVariable Long id) {
        actaService.eliminarActa(id);
        return new StructureResponses<>("exito", "Acta eliminada exitosamente", null);
    }
}