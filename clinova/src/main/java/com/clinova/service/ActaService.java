package com.clinova.service;

import com.clinova.dto.ActaDTO;
import com.clinova.entity.Acta;
import com.clinova.entity.Role;
import com.clinova.entity.Usuario;
import com.clinova.repository.ActaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActaService {

    private final ActaRepository actaRepository;

    @Transactional
    public ActaDTO crearActa(ActaDTO actaDTO) {
        Acta acta = Acta.builder()
                .titulo(actaDTO.titulo())
                .contenidoHtml(actaDTO.contenidoHtml())
                .estado(actaDTO.estado())
                .tipo(actaDTO.tipo())
                .responsable(actaDTO.responsable())
                .fecha(LocalDate.now())
                .build();

        acta = actaRepository.save(acta);
        return mapearADto(acta);
    }

    @Transactional(readOnly = true)
    public List<ActaDTO> obtenerTodas() {
        return actaRepository.findAll().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActaDTO obtenerPorId(Long id) {
        return mapearADto(buscarActaPorId(id));
    }

    @Transactional
    public ActaDTO actualizarActa(Long id, ActaDTO actaDTO, Usuario usuarioAutenticado) {
        Acta acta = buscarActaPorId(id);

        validarPermisoEdicion(acta, usuarioAutenticado);

        acta.setTitulo(actaDTO.titulo());
        acta.setContenidoHtml(actaDTO.contenidoHtml());
        acta.setEstado(actaDTO.estado());
        acta.setTipo(actaDTO.tipo());

        acta = actaRepository.save(acta);
        return mapearADto(acta);
    }

    @Transactional
    public void eliminarActa(Long id) {
        Acta acta = buscarActaPorId(id);
        actaRepository.delete(acta);
    }

    private Acta buscarActaPorId(Long id) {
        return actaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Acta no encontrada con ID: " + id));
    }

    private ActaDTO mapearADto(Acta acta) {
        return new ActaDTO(
                acta.getId(),
                acta.getTitulo(),
                acta.getContenidoHtml(),
                acta.getEstado(),
                acta.getTipo(),
                acta.getResponsable(),
                acta.getFecha()
        );
    }

    private void validarPermisoEdicion(Acta acta, Usuario usuarioAutenticado) {
        if (usuarioAutenticado.getRol() == Role.ADMIN) {
            return;
        }

        if (usuarioAutenticado.getRol() == Role.LIDER_DE_PROCESO) {
            boolean esPropietario = acta.getResponsable().equals(usuarioAutenticado.getUsername());
            if (!esPropietario) {
                throw new AccessDeniedException("No tienes permiso para editar un acta creada por otro líder de proceso.");
            }
        }
    }
}