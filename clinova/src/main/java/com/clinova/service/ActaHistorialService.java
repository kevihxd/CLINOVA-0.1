package com.clinova.service;

import com.clinova.dto.ActaHistorialDTO;
import com.clinova.entity.ActaHistorial;
import com.clinova.entity.Usuario;
import com.clinova.repository.ActaHistorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActaHistorialService {

    private final ActaHistorialRepository repository;

    @Transactional
    public void registrarHistorial(Long actaId, String accion, String descripcion, Usuario usuario) {
        String username = (usuario != null) ? usuario.getUsername() : "Sistema";
        ActaHistorial log = ActaHistorial.builder()
                .actaId(actaId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(username)
                .fecha(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActaHistorialDTO> obtenerHistorialPorActa(Long actaId) {
        return repository.findByActaIdOrderByFechaDesc(actaId).stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    private ActaHistorialDTO mapearADto(ActaHistorial log) {
        return new ActaHistorialDTO(
                log.getId(),
                log.getActaId(),
                log.getAccion(),
                log.getDescripcion(),
                log.getUsuario(),
                log.getFecha()
        );
    }
}
