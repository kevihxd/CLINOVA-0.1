package com.clinova.service;

import com.clinova.dto.HojaVidaHistorialDTO;
import com.clinova.entity.HojaVidaHistorial;
import com.clinova.entity.Usuario;
import com.clinova.repository.HojaVidaHistorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HojaVidaHistorialService {

    private final HojaVidaHistorialRepository repository;

    @Transactional
    public void registrarHistorial(Long hojaVidaId, String accion, String descripcion, Usuario usuario) {
        String username = (usuario != null) ? usuario.getUsername() : "Sistema";
        HojaVidaHistorial log = HojaVidaHistorial.builder()
                .hojaVidaId(hojaVidaId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(username)
                .fecha(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    @Transactional
    public void registrarHistorial(Long hojaVidaId, String accion, String descripcion, String usuario) {
        HojaVidaHistorial log = HojaVidaHistorial.builder()
                .hojaVidaId(hojaVidaId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(usuario != null ? usuario : "Sistema")
                .fecha(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<HojaVidaHistorialDTO> obtenerHistorialPorHojaVida(Long hojaVidaId) {
        return repository.findByHojaVidaIdOrderByFechaDesc(hojaVidaId).stream()
                .map(log -> new HojaVidaHistorialDTO(
                        log.getId(),
                        log.getHojaVidaId(),
                        log.getAccion(),
                        log.getDescripcion(),
                        log.getUsuario(),
                        log.getFecha()
                ))
                .collect(Collectors.toList());
    }
}
