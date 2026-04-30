package com.clinova.service;

import com.clinova.dto.ComentarioActaDTO;
import com.clinova.entity.Acta;
import com.clinova.entity.ComentarioActa;
import com.clinova.entity.Usuario;
import com.clinova.repository.ActaRepository;
import com.clinova.repository.ComentarioActaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComentarioActaService {

    private final ComentarioActaRepository comentarioActaRepository;
    private final ActaRepository actaRepository;

    @Transactional
    public ComentarioActaDTO agregarComentario(Long actaId, String contenido, Usuario autor) {
        Acta acta = actaRepository.findById(actaId)
                .orElseThrow(() -> new RuntimeException("Acta no encontrada con ID: " + actaId));

        ComentarioActa comentario = ComentarioActa.builder()
                .contenido(contenido)
                .acta(acta)
                .autor(autor)
                .build();

        comentario = comentarioActaRepository.save(comentario);
        return mapearADto(comentario);
    }

    @Transactional(readOnly = true)
    public List<ComentarioActaDTO> obtenerComentariosPorActa(Long actaId) {
        return comentarioActaRepository.findByActaIdOrderByFechaCreacionDesc(actaId).stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    private ComentarioActaDTO mapearADto(ComentarioActa comentario) {
        return new ComentarioActaDTO(
                comentario.getId(),
                comentario.getContenido(),
                comentario.getFechaCreacion(),
                comentario.getAutor().getUsername(), // Se usa getUsername() para evitar el error de método inexistente
                comentario.getAutor().getUsername()
        );
    }
}