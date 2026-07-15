package com.clinova.controller;

import com.clinova.entity.PlantillaCorreo;
import com.clinova.repository.PlantillaCorreoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plantillas-correo")
@RequiredArgsConstructor
public class PlantillaCorreoController {

    private final PlantillaCorreoRepository plantillaCorreoRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PlantillaCorreo>> getAllPlantillas() {
        return ResponseEntity.ok(plantillaCorreoRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlantillaCorreo> createPlantilla(@RequestBody PlantillaCorreo plantilla) {
        return ResponseEntity.ok(plantillaCorreoRepository.save(plantilla));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlantillaCorreo> updatePlantilla(@PathVariable Long id, @RequestBody PlantillaCorreo plantillaActualizada) {
        return plantillaCorreoRepository.findById(id)
                .map(plantilla -> {
                    plantilla.setNombre(plantillaActualizada.getNombre());
                    plantilla.setAsunto(plantillaActualizada.getAsunto());
                    plantilla.setCuerpo(plantillaActualizada.getCuerpo());
                    return ResponseEntity.ok(plantillaCorreoRepository.save(plantilla));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlantilla(@PathVariable Long id) {
        if (plantillaCorreoRepository.existsById(id)) {
            plantillaCorreoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
