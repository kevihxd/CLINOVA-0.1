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
        List<Documento> relacionados = new ArrayList<>();
        if (doc != null) {
            relacionados.add(doc);
            if (doc.getCodigo() != null && !doc.getCodigo().isBlank()) {
                List<Documento> matches = documentoRepository.findAllByCodigo(doc.getCodigo());
                for (Documento d : matches) {
                    if (!docIds.contains(d.getId())) {
                        docIds.add(d.getId());
                        relacionados.add(d);
                    }
                }
            }
        }

        List<DocumentoHistorial> existingLogs = repository.findByDocumentoIdInOrderByFechaDesc(docIds);
        List<DocumentoHistorialDTO> dtos = new ArrayList<>();

        for (DocumentoHistorial log : existingLogs) {
            String user = (log.getUsuario() != null && !log.getUsuario().isBlank()) ? log.getUsuario() : "Usuario del Sistema";
            dtos.add(new DocumentoHistorialDTO(
                    log.getId(),
                    log.getDocumentoId(),
                    log.getVersion(),
                    log.getAccion(),
                    log.getDescripcion(),
                    user,
                    log.getFecha()
            ));
        }

        for (Documento d : relacionados) {
            String verStr = d.getVersion() != null ? d.getVersion() : "1";
            boolean hasVer = dtos.stream().anyMatch(dto -> dto.getVersion() != null && verStr.equals(dto.getVersion().trim()));
            if (!hasVer) {
                String desc = (d.getControlCambios() != null && !d.getControlCambios().isBlank()) ? d.getControlCambios()
                        : (d.getDescripcion() != null && !d.getDescripcion().isBlank() && !d.getDescripcion().equals("Versión actual del documento")) 
                        ? d.getDescripcion() : ("Actualización del documento, versión " + verStr);
                String user = (d.getElabora() != null && !d.getElabora().isBlank()) ? d.getElabora() : "Usuario del Sistema";
                LocalDateTime fecha = LocalDateTime.now();
                if (d.getFechaAprobacion() != null && !d.getFechaAprobacion().isBlank()) {
                    try {
                        String[] parts = d.getFechaAprobacion().split("[/-]");
                        if (parts.length == 3) {
                            int day, month, year;
                            if (parts[0].length() == 4) {
                                year = Integer.parseInt(parts[0]);
                                month = Integer.parseInt(parts[1]);
                                day = Integer.parseInt(parts[2]);
                            } else {
                                day = Integer.parseInt(parts[0]);
                                month = Integer.parseInt(parts[1]);
                                year = Integer.parseInt(parts[2]);
                            }
                            fecha = LocalDateTime.of(year, month, day, 12, 0);
                        }
                    } catch(Exception ignored){}
                }
                dtos.add(new DocumentoHistorialDTO(
                        d.getId(),
                        d.getId(),
                        verStr,
                        "CREACION_VERSION",
                        desc,
                        user,
                        fecha
                ));
            }
        }

        dtos.sort((a, b) -> {
            try {
                int vA = Integer.parseInt(a.getVersion().replaceAll("[^0-9]", ""));
                int vB = Integer.parseInt(b.getVersion().replaceAll("[^0-9]", ""));
                return Integer.compare(vB, vA);
            } catch(Exception e) {
                return 0;
            }
        });

        return dtos;
    }
}
