package com.clinova.controller;

import com.clinova.entity.Cargo;
import com.clinova.entity.Permiso;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.PermisoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cargos")
@RequiredArgsConstructor
public class CargoController {

    private final CargoRepository cargoRepository;
    private final PermisoRepository permisoRepository;
    
    @Data
    static class CargoDTO {
        Long id;
        String nombre;
        Long reportaAId;
        String reportaANombre;
        List<Map<String, Object>> permisos;
    }

    @Data
    static class PermisoDTO {
        Long id;
        String nombre;
        String descripcion;
    }

    private CargoDTO toDTO(Cargo c) {
        CargoDTO dto = new CargoDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        if (c.getReportaA() != null) {
            dto.setReportaAId(c.getReportaA().getId());
            dto.setReportaANombre(c.getReportaA().getNombre());
        }
        if (c.getPermisos() != null) {
            dto.setPermisos(c.getPermisos().stream().map(p -> {
                Map<String, Object> pm = new java.util.LinkedHashMap<>();
                pm.put("id", p.getId());
                pm.put("nombre", p.getNombre());
                return pm;
            }).collect(Collectors.toList()));
        } else {
            dto.setPermisos(new java.util.ArrayList<>());
        }
        return dto;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<CargoDTO>> listarCargos() {
        return ResponseEntity.ok(
                cargoRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList())
        );
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearCargo(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre del cargo es obligatorio");
            }
            if (cargoRepository.findByNombre(nombre.trim()).isPresent()) {
                return ResponseEntity.badRequest().body("Ya existe un cargo con ese nombre");
            }
            Cargo nuevo = new Cargo();
            nuevo.setNombre(nombre.trim());
            nuevo.setPermisos(new HashSet<>());
            Cargo guardado = cargoRepository.save(nuevo);
            return ResponseEntity.ok(toDTO(guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al crear cargo: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizarCargo(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Cargo cargo = cargoRepository.findById(id).orElse(null);
            if (cargo == null) return ResponseEntity.notFound().build();
            String nuevoNombre = payload.get("nombre");
            if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre del cargo es obligatorio");
            }
            cargo.setNombre(nuevoNombre.trim());
            return ResponseEntity.ok(toDTO(cargoRepository.save(cargo)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al actualizar cargo: " + e.getMessage());
        }
    }

    @GetMapping("/permisos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PermisoDTO>> listarPermisos() {
        return ResponseEntity.ok(
                permisoRepository.findAll().stream().map(p -> {
                    PermisoDTO dto = new PermisoDTO();
                    dto.setId(p.getId());
                    dto.setNombre(p.getNombre());
                    dto.setDescripcion(p.getDescripcion());
                    return dto;
                }).collect(Collectors.toList())
        );
    }

    @PutMapping("/{cargoId}/permisos")
    @Transactional
    public ResponseEntity<?> actualizarPermisos(@PathVariable Long cargoId, @RequestBody List<Long> permisoIds) {
        try {
            Cargo cargo = cargoRepository.findById(cargoId).orElse(null);
            if (cargo == null) return ResponseEntity.notFound().build();
            List<Permiso> permisosNuevos = permisoRepository.findAllById(permisoIds);
            cargo.setPermisos(new HashSet<>(permisosNuevos));
            cargoRepository.save(cargo);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al actualizar permisos: " + e.getMessage());
        }
    }
}