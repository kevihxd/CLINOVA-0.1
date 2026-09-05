package com.clinova.controller;

import com.clinova.entity.ReporteSistema;
import com.clinova.repository.ReporteSistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/reportes-sistema", "/api/reportes-sistema"})
@RequiredArgsConstructor
public class ReporteSistemaController {

    private final ReporteSistemaRepository repository;

    @GetMapping
    public ResponseEntity<List<ReporteSistema>> listar() {
        return ResponseEntity.ok(repository.findAllByOrderByFechaCreacionDesc());
    }

    @PostMapping
    public ResponseEntity<ReporteSistema> crear(@RequestBody ReporteSistema reporte) {
        return ResponseEntity.ok(repository.save(reporte));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ReporteSistema> actualizarEstado(@PathVariable Long id, @RequestParam String nuevoEstado) {
        return repository.findById(id).map(r -> {
            r.setEstado(nuevoEstado);
            return ResponseEntity.ok(repository.save(r));
        }).orElse(ResponseEntity.notFound().build());
    }
}
