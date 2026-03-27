package com.clinova.service;

import com.clinova.dto.ActaDTO;
import com.clinova.entity.Acta;
import com.clinova.repository.ActaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActaService {

    private final ActaRepository actaRepository;

    @Transactional
    public ActaDTO crearActa(ActaDTO request) {
        Acta acta = Acta.builder()
                .titulo(request.titulo())
                .fecha(request.fecha())
                .tipo(request.tipo())
                .estado(request.estado())
                .responsable(request.responsable())
                .contenidoHtml(request.contenidoHtml())
                .build();

        Acta guardada = actaRepository.save(acta);
        return mapearADTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<ActaDTO> obtenerTodas() {
        return actaRepository.findAll().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActaDTO obtenerPorId(Long id) {
        Acta acta = actaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Acta no encontrada"));
        return mapearADTO(acta);
    }

    @Transactional
    public void eliminarActa(Long id) {
        if (!actaRepository.existsById(id)) {
            throw new RuntimeException("Acta no encontrada");
        }
        actaRepository.deleteById(id);
    }

    private ActaDTO mapearADTO(Acta acta) {
        return new ActaDTO(
                acta.getId(),
                acta.getTitulo(),
                acta.getFecha(),
                acta.getTipo(),
                acta.getEstado(),
                acta.getResponsable(),
                acta.getContenidoHtml(),
                acta.getFechaCreacion()
        );
    }
}