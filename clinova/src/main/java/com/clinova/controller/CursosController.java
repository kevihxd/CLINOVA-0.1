package com.clinova.controller;

import com.clinova.dto.CursoAsignadoDTO;
import com.clinova.service.CursosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.clinova.dto.CursoMaestroDTO;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
public class CursosController {

    private final CursosService cursosService;

    @GetMapping("/mis-cursos/{usuarioId}")
    public ResponseEntity<List<CursoAsignadoDTO>> listarMisCursos(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(cursosService.obtenerCursosPorUsuario(usuarioId));
    }

    @PostMapping("/subir-certificado/{asignacionId}")
    public ResponseEntity<String> subirCertificado(
            @PathVariable Long asignacionId,
            @RequestParam("archivo") MultipartFile archivo) {
        cursosService.subirCertificado(asignacionId, archivo);
        return ResponseEntity.ok("Certificado cargado exitosamente");
    }

    @GetMapping("/maestros")
    public ResponseEntity<List<CursoMaestroDTO>> listarCursosMaestros() {
        return ResponseEntity.ok(cursosService.obtenerCursosMaestros());
    }

    @PostMapping("/maestros")
    public ResponseEntity<CursoMaestroDTO> crearCursoMaestro(@RequestBody CursoMaestroDTO request) {
        return ResponseEntity.ok(cursosService.crearCursoMaestro(request));
    }

    @PutMapping("/maestros/{id}")
    public ResponseEntity<CursoMaestroDTO> actualizarCursoMaestro(@PathVariable Long id, @RequestBody CursoMaestroDTO request) {
        return ResponseEntity.ok(cursosService.actualizarCursoMaestro(id, request));
    }

    @DeleteMapping("/maestros/{id}")
    public ResponseEntity<Void> eliminarCursoMaestro(@PathVariable Long id) {
        cursosService.eliminarCursoMaestro(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/asignacion-masiva")
    public ResponseEntity<Void> asignarMasivo(@RequestBody Map<String, Long> payload) {
        cursosService.asignarMasivo(payload.get("cursoId"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/asignar")
    public ResponseEntity<Void> asignarCurso(@RequestBody Map<String, Long> payload) {
        cursosService.asignarCurso(payload.get("usuarioId"), payload.get("cursoMaestroId"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/asignados")
    public ResponseEntity<List<CursoAsignadoDTO>> listarAsignados(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(cursosService.obtenerCursosPorUsuario(usuarioId));
    }

    @DeleteMapping("/asignados/{id}")
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable Long id) {
        cursosService.eliminarAsignacion(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/asignados/{id}/estado")
    public ResponseEntity<Void> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        cursosService.actualizarEstado(id, payload.get("estado"));
        return ResponseEntity.ok().build();
    }
}