package com.clinova.controller;

import com.clinova.entity.Documento;
import com.clinova.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "*")
public class DocumentoController {
    @Autowired
    private DocumentoService documentoService;

    @GetMapping
    public List<Documento> listar() {
        return documentoService.listarTodos();
    }

    @PostMapping
    public ResponseEntity<Documento> crear(@RequestBody Documento documento) {
        return ResponseEntity.ok(documentoService.guardar(documento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
        return ResponseEntity.ok().build();
    }
}