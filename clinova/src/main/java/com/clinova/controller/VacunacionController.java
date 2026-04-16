package com.clinova.controller;

import com.clinova.dto.PersonaStatusDTO;
import com.clinova.dto.RegistroVacunaDTO;
import com.clinova.entity.Vacuna;
import com.clinova.service.VacunacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vacunacion")
@RequiredArgsConstructor
public class VacunacionController {

    private final VacunacionService vacunacionService;

    @GetMapping("/catalogo")
    public ResponseEntity<List<Vacuna>> getCatalogo() {
        return ResponseEntity.ok(vacunacionService.listarCatalogoVacunas());
    }

    @PostMapping("/catalogo")
    public ResponseEntity<Vacuna> addVacuna(@RequestParam String nombre) {
        return ResponseEntity.ok(vacunacionService.crearVacuna(nombre));
    }

    @PutMapping("/catalogo/{id}")
    public ResponseEntity<Vacuna> editarVacuna(@PathVariable Long id, @RequestParam String nombre) {
        return ResponseEntity.ok(vacunacionService.editarVacuna(id, nombre));
    }

    @DeleteMapping("/catalogo/{id}")
    public ResponseEntity<?> eliminarVacuna(@PathVariable Long id) {
        try {
            vacunacionService.eliminarVacuna(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/buscar/{documento}")
    public ResponseEntity<PersonaStatusDTO> buscar(@PathVariable String documento) {
        return ResponseEntity.ok(vacunacionService.buscarPorDocumento(documento));
    }

    @PostMapping("/registrar")
    public ResponseEntity<Void> registrar(@RequestBody RegistroVacunaDTO dto) {
        vacunacionService.registrarDosis(dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/registrar/{id}")
    public ResponseEntity<Void> editarDosis(@PathVariable Long id, @RequestBody RegistroVacunaDTO dto) {
        vacunacionService.editarDosis(id, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/registrar/{id}")
    public ResponseEntity<Void> eliminarDosis(@PathVariable Long id) {
        vacunacionService.eliminarDosis(id);
        return ResponseEntity.noContent().build();
    }
}