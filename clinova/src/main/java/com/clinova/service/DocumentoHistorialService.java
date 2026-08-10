package com.clinova.service;

import com.clinova.dto.DocumentoHistorialDTO;
import com.clinova.entity.DocumentoHistorial;
import com.clinova.entity.Usuario;
import com.clinova.repository.DocumentoHistorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentoHistorialService {

    private final DocumentoHistorialRepository repository;

    @Transactional
    public void registrarHistorial(Long documentoId, String accion, String descripcion, Usuario usuario) {
        String username = (usuario != null) ? usuario.getUsername() : "Sistema";
        DocumentoHistorial log = DocumentoHistorial.builder()
                .documentoId(documentoId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(username)
                .fecha(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    @Transactional
    public void registrarHistorial(Long documentoId, String accion, String descripcion, String usuario) {
        DocumentoHistorial log = DocumentoHistorial.builder()
                .documentoId(documentoId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(usuario != null ? usuario : "Sistema")
                .fecha(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<DocumentoHistorialDTO> obtenerHistorialPorDocumento(Long documentoId) {
        return repository.findByDocumentoIdOrderByFechaDesc(documentoId).stream()
                .map(log -> new DocumentoHistorialDTO(
                        log.getId(),
                        log.getDocumentoId(),
                        log.getVersion(),
                        log.getAccion(),
                        log.getDescripcion(),
                        log.getUsuario(),
                        log.getFecha()
                ))
                .collect(Collectors.toList());
    }
}
