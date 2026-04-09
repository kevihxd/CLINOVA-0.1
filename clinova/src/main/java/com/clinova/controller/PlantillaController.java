package com.clinova.controller;

import com.clinova.dto.PlantillaDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.PlantillaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plantillas")
@RequiredArgsConstructor
public class PlantillaController {

    private final PlantillaService plantillaService;

    @PostMapping
    public StructureResponses<PlantillaDTO> crearPlantilla(@RequestBody PlantillaDTO request) {
        PlantillaDTO response = plantillaService.crearPlantilla(request);
        return new StructureResponses<>("SUCCESS", "Plantilla creada exitosamente", response);
    }

    @PutMapping("/{id}")
    public StructureResponses<PlantillaDTO> actualizarPlantilla(@PathVariable Long id, @RequestBody PlantillaDTO request) {
        PlantillaDTO response = plantillaService.actualizarPlantilla(id, request);
        return new StructureResponses<>("SUCCESS", "Plantilla actualizada exitosamente", response);
    }

    @DeleteMapping("/{id}")
    public StructureResponses<Void> eliminarPlantilla(@PathVariable Long id) {
        plantillaService.eliminarPlantilla(id);
        return new StructureResponses<>("SUCCESS", "Plantilla eliminada exitosamente", null);
    }

    @GetMapping
    public StructureResponses<List<PlantillaDTO>> obtenerTodas() {
        List<PlantillaDTO> response = plantillaService.obtenerTodas();
        return new StructureResponses<>("SUCCESS", "Plantillas obtenidas", response);
    }

    @GetMapping("/{id}")
    public StructureResponses<PlantillaDTO> obtenerPorId(@PathVariable Long id) {
        PlantillaDTO response = plantillaService.obtenerPorId(id);
        return new StructureResponses<>("SUCCESS", "Plantilla obtenida", response);
    }
}