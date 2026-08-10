package com.clinova.controller;

import com.clinova.entity.Objeto;
import com.clinova.repository.ObjetoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/objetos")
@RequiredArgsConstructor
public class ObjetoController {

    private final ObjetoRepository objetoRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Objeto>> listar() {
        return ResponseEntity.ok(objetoRepository.findAll());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody Map<String, String> payload) {
        try {
            String nombre = payload.get("nombre");
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nombre del objeto es obligatorio");
            }
            if (objetoRepository.findByNombre(nombre.trim()).isPresent()) {
                return ResponseEntity.badRequest().body("Ya existe un objeto con ese nombre");
            }
            Objeto nuevo = Objeto.builder().nombre(nombre.trim()).build();
            return ResponseEntity.ok(objetoRepository.save(nuevo));
        } catch (Exception e) {
            log.error("Error al crear objeto: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error al crear objeto");
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Objeto obj = objetoRepository.findById(id).orElse(null);
            if (obj == null) return ResponseEntity.notFound().build();
            String nuevoNombre = payload.get("nombre");
            if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
                obj.setNombre(nuevoNombre.trim());
                return ResponseEntity.ok(objetoRepository.save(obj));
            }
            return ResponseEntity.badRequest().body("Nombre invalido");
        } catch (Exception e) {
            log.error("Error al actualizar objeto: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error al actualizar objeto");
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            objetoRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar objeto: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error al eliminar objeto");
        }
    }
}
