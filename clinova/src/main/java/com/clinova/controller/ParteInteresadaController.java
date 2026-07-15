package com.clinova.controller;

import com.clinova.entity.ParteInteresada;
import com.clinova.service.ParteInteresadaService;
import com.clinova.dto.StructureResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contexto/partes")
@RequiredArgsConstructor
public class ParteInteresadaController {
    private final ParteInteresadaService service;

    @GetMapping
    public ResponseEntity<StructureResponses<List<ParteInteresada>>> getAll() {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Lista de partes interesadas obtenida exitosamente", service.findAll()));
    }

    @PostMapping
    public ResponseEntity<StructureResponses<ParteInteresada>> create(@RequestBody ParteInteresada entity) {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Parte interesada creada exitosamente", service.save(entity)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StructureResponses<ParteInteresada>> update(@PathVariable Long id, @RequestBody ParteInteresada entity) {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Parte interesada actualizada exitosamente", service.update(id, entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Parte interesada eliminada exitosamente", null));
    }
}
