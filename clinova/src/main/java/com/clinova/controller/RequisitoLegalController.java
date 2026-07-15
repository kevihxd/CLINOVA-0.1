package com.clinova.controller;

import com.clinova.entity.RequisitoLegal;
import com.clinova.service.RequisitoLegalService;
import com.clinova.dto.StructureResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contexto/requisitos")
@RequiredArgsConstructor
public class RequisitoLegalController {
    private final RequisitoLegalService service;

    @GetMapping
    public ResponseEntity<StructureResponses<List<RequisitoLegal>>> getAll() {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Lista de requisitos legales obtenida exitosamente", service.findAll()));
    }

    @PostMapping
    public ResponseEntity<StructureResponses<RequisitoLegal>> create(@RequestBody RequisitoLegal entity) {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Requisito legal creado exitosamente", service.save(entity)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StructureResponses<RequisitoLegal>> update(@PathVariable Long id, @RequestBody RequisitoLegal entity) {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Requisito legal actualizado exitosamente", service.update(id, entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Requisito legal eliminado exitosamente", null));
    }
}
