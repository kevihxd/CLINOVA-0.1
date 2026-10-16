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

    private String cleanMojibake(String text) {
        if (text == null) return null;
        return text
                .replace("Ã¡", "á").replace("Ã©", "é").replace("Ã­", "í").replace("Ã³", "ó").replace("Ãº", "ú").replace("Ã±", "ñ")
                .replace("Ã", "Á").replace("Ã‰", "É").replace("Ã", "Í").replace("Ã“", "Ó").replace("Ãš", "Ú").replace("Ã‘", "Ñ")
                .replace("Ã", "í")
                .replace("Â·", "·").replace("Â", "");
    }

    @Transactional
    public void registrarHistorial(Long hojaVidaId, String accion, String descripcion, Usuario usuario) {
        String username = (usuario != null && usuario.getUsername() != null && !usuario.getUsername().trim().isEmpty()) 
                ? usuario.getUsername() 
                : "Sistema";
        HojaVidaHistorial log = HojaVidaHistorial.builder()
                .hojaVidaId(hojaVidaId)
                .accion(accion)
                .descripcion(cleanMojibake(descripcion))
                .usuario(cleanMojibake(username))
                .fecha(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    @Transactional
    public void registrarHistorial(Long hojaVidaId, String accion, String descripcion, String usuario) {
        String username = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "Sistema";
        HojaVidaHistorial log = HojaVidaHistorial.builder()
                .hojaVidaId(hojaVidaId)
                .accion(accion)
                .descripcion(cleanMojibake(descripcion))
                .usuario(cleanMojibake(username))
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
                        cleanMojibake(log.getDescripcion()),
                        cleanMojibake(log.getUsuario()),
                        log.getFecha()
                ))
                .collect(Collectors.toList());
    }
}
