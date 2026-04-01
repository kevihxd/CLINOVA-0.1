package com.clinova.controller;

import com.clinova.entity.Cargo;
import com.clinova.entity.Permiso;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.PermisoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cargos")
@CrossOrigin(origins = "*")
public class CargoController {

    @Autowired
    private CargoRepository cargoRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @GetMapping
    public ResponseEntity<List<Cargo>> listarCargos() {
        return ResponseEntity.ok(cargoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> crearCargo(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre del cargo es obligatorio");
            }

            Cargo nuevoCargo = new Cargo();
            nuevoCargo.setNombre(nombre.trim());
            Cargo guardado = cargoRepository.save(nuevoCargo);

            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al crear cargo: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCargo(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            return cargoRepository.findById(id).map(cargo -> {
                String nuevoNombre = payload.get("nombre");
                if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("El nombre del cargo es obligatorio");
                }
                cargo.setNombre(nuevoNombre.trim());
                Cargo guardado = cargoRepository.save(cargo);
                return ResponseEntity.ok(guardado);
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al actualizar cargo: " + e.getMessage());
        }
    }

    @GetMapping("/permisos")
    public ResponseEntity<List<Permiso>> listarPermisos() {
        return ResponseEntity.ok(permisoRepository.findAll());
    }

    @PutMapping("/{cargoId}/permisos")
    public ResponseEntity<?> actualizarPermisos(@PathVariable Long cargoId, @RequestBody List<Long> permisoIds) {
        return cargoRepository.findById(cargoId).map(cargo -> {
            List<Permiso> permisosNuevos = permisoRepository.findAllById(permisoIds);
            cargo.setPermisos(new HashSet<>(permisosNuevos));
            cargoRepository.save(cargo);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}