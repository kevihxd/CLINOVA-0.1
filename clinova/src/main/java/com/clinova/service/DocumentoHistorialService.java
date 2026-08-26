package com.clinova.service;

import com.clinova.dto.DocumentoHistorialDTO;
import com.clinova.entity.Documento;
import com.clinova.repository.DocumentoHistorialRepository;
import com.clinova.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentoHistorialService {

    private final DocumentoHistorialRepository repository;
    private final DocumentoRepository documentoRepository;

    @Transactional
    public void registrarHistorial(Long documentoId, String accion, String descripcion, Usuario usuario) {
        registrarHistorial(documentoId, accion, descripcion, usuario, null);
    }

    @Transactional
    public void registrarHistorial(Long documentoId, String accion, String descripcion, Usuario usuario, String version) {
        String username = (usuario != null) ? usuario.getUsername() : "Sistema";
        DocumentoHistorial log = DocumentoHistorial.builder()
                .documentoId(documentoId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(username)
                .version(version)
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
        List<Long> docIds = new ArrayList<>();
        docIds.add(documentoId);

        Documento doc = documentoRepository.findById(documentoId).orElse(null);
        if (doc != null && doc.getCodigo() != null && !doc.getCodigo().isBlank()) {
            List<Documento> relacionados = documentoRepository.findAllByCodigo(doc.getCodigo());
            for (Documento d : relacionados) {
                if (!docIds.contains(d.getId())) {
                    docIds.add(d.getId());
                }
            }
        }

        return repository.findByDocumentoIdInOrderByFechaDesc(docIds).stream()
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
