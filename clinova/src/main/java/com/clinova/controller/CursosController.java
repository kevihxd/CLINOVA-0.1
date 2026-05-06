package com.clinova.controller;

import com.clinova.entity.CursoAsignado;
import com.clinova.entity.CursoMaestro;
import com.clinova.entity.HojaVida;
import com.clinova.repository.CursoAsignadoRepository;
import com.clinova.repository.CursoMaestroRepository;
import com.clinova.repository.HojaVidaRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
public class CursosController {

    private final CursoMaestroRepository cursoMaestroRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final HojaVidaRepository hojaVidaRepository;

    @GetMapping("/catalogo")
    public ResponseEntity<List<CursoMaestro>> listarCatalogo() {
        return ResponseEntity.ok(cursoMaestroRepository.findAll());
    }

    @PostMapping("/catalogo")
    public ResponseEntity<CursoMaestro> crearCursoCatalogo(@RequestBody CursoMaestro payload) {
        // Aseguramos que no venga un ID (evita confusión de merge vs persist)
        payload.setId(null);
        CursoMaestro guardado = cursoMaestroRepository.save(payload);
        return ResponseEntity.ok(guardado);
    }

    @GetMapping("/asignados/{hojaVidaId}")
    @Transactional(readOnly = true)  // <-- FIX: sesión activa para evitar LazyInitializationException
    public ResponseEntity<List<CursoAsignado>> listarAsignados(@PathVariable Long hojaVidaId) {
        return ResponseEntity.ok(cursoAsignadoRepository.findByHojaVidaId(hojaVidaId));
    }

    @PostMapping("/asignar")
    @Transactional  // <-- FIX: sesión activa
    public ResponseEntity<?> asignarCurso(@RequestBody AsignarCursoRequest request) {
        HojaVida hojaVida = hojaVidaRepository.findById(request.getHojaVidaId()).orElse(null);
        CursoMaestro cursoMaestro = cursoMaestroRepository.findById(request.getCursoMaestroId()).orElse(null);

        if (hojaVida == null || cursoMaestro == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Hoja de vida o curso no encontrado")
            );
        }

        CursoAsignado nuevo = CursoAsignado.builder()
                .hojaVida(hojaVida)
                .cursoMaestro(cursoMaestro)
                .fechaLimite(request.getFechaLimite())
                .estado("PENDIENTE")
                .build();

        return ResponseEntity.ok(cursoAsignadoRepository.save(nuevo));
    }

    @DeleteMapping("/asignados/{id}")
    public ResponseEntity<?> eliminarAsignacion(@PathVariable Long id) {
        cursoAsignadoRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class AsignarCursoRequest {
        private Long hojaVidaId;
        private Long cursoMaestroId;
        private LocalDate fechaLimite;
    }
}