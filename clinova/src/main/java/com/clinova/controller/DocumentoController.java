package com.clinova.controller;

import com.clinova.dto.StructureResponses;
import com.clinova.entity.Documento;
import com.clinova.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @GetMapping
    public ResponseEntity<StructureResponses<List<Documento>>> obtenerTodos() {
        List<Documento> documentos = documentoService.obtenerTodos();
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documentos obtenidos", documentos));
    }

    @PostMapping
    public ResponseEntity<StructureResponses<Documento>> crear(@RequestBody Documento documento) {
        try {
            Documento guardado = documentoService.crear(documento);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento creado exitosamente", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> eliminar(@PathVariable Long id) {
        try {
            documentoService.eliminar(id);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento eliminado", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }
}