package com.clinova.controller;

import com.clinova.dto.ComentarioActaDTO;
import com.clinova.entity.Usuario;
import com.clinova.service.ComentarioActaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/actas/{actaId}/comentarios")
@RequiredArgsConstructor
public class ComentarioActaController {

    private final ComentarioActaService comentarioActaService;

    @PostMapping
    public ResponseEntity<ComentarioActaDTO> agregarComentario(
            @PathVariable Long actaId,
            @RequestBody String contenido,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        return ResponseEntity.ok(comentarioActaService.agregarComentario(actaId, contenido, usuarioAutenticado));
    }

    @GetMapping
    public ResponseEntity<List<ComentarioActaDTO>> obtenerComentarios(@PathVariable Long actaId) {
        return ResponseEntity.ok(comentarioActaService.obtenerComentariosPorActa(actaId));
    }
}