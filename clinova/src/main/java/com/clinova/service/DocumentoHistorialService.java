package com.clinova.service;

import com.clinova.dto.DocumentoHistorialDTO;
import com.clinova.entity.Documento;
import com.clinova.entity.DocumentoHistorial;
import com.clinova.entity.Usuario;
import com.clinova.repository.DocumentoHistorialRepository;
import com.clinova.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
        try {
            String username = (usuario != null) ? usuario.getUsername() : "Sistema";
            DocumentoHistorial logEntry = DocumentoHistorial.builder()
                    .documentoId(documentoId)
                    .accion(accion)
                    .descripcion(descripcion)
                    .usuario(username)
                    .version(version)
                    .fecha(LocalDateTime.now())
                    .build();
            repository.save(logEntry);
        } catch (Exception e) {
            log.error("Error registrando historial para docId {}: {}", documentoId, e.getMessage());
        }
    }

    @Transactional
    public void registrarHistorial(Long documentoId, String accion, String descripcion, String usuario) {
        try {
            DocumentoHistorial logEntry = DocumentoHistorial.builder()
                    .documentoId(documentoId)
                    .accion(accion)
                    .descripcion(descripcion)
                    .usuario(usuario != null ? usuario : "Sistema")
                    .fecha(LocalDateTime.now())
                    .build();
            repository.save(logEntry);
        } catch (Exception e) {
            log.error("Error registrando historial para docId {}: {}", documentoId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoHistorialDTO> obtenerHistorialPorDocumento(Long documentoId) {
        List<DocumentoHistorialDTO> dtos = new ArrayList<>();
        try {
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

            for (DocumentoHistorial logEntry : existingLogs) {
                String user = (logEntry.getUsuario() != null && !logEntry.getUsuario().isBlank()) ? logEntry.getUsuario() : "Usuario del Sistema";
                String ver = logEntry.getVersion();
                if (ver == null || ver.isBlank() || ver.equals("0")) {
                    for (Documento d : relacionados) {
                        if (d.getId().equals(logEntry.getDocumentoId()) && d.getVersion() != null) {
                            ver = d.getVersion();
                            break;
                        }
                    }
                }
                if (ver == null || ver.isBlank()) ver = doc != null && doc.getVersion() != null ? doc.getVersion() : "1";
                dtos.add(new DocumentoHistorialDTO(
                        logEntry.getId(),
                        logEntry.getDocumentoId(),
                        ver,
                        logEntry.getAccion(),
                        logEntry.getDescripcion(),
                        user,
                        logEntry.getFecha() != null ? logEntry.getFecha() : LocalDateTime.now()
                ));
            }

            for (Documento d : relacionados) {
                String verStr = d.getVersion() != null ? d.getVersion().trim() : "1";
                boolean hasVer = dtos.stream().anyMatch(dto -> dto.version() != null && verStr.equalsIgnoreCase(dto.version().trim()));
                if (!hasVer) {
                    String desc = (d.getControlCambios() != null && !d.getControlCambios().isBlank()) ? d.getControlCambios()
                            : (d.getDescripcion() != null && !d.getDescripcion().isBlank() && !d.getDescripcion().equals("Versión actual del documento")) 
                            ? d.getDescripcion() : ("Actualización del documento, versión " + verStr);
                    String user = (d.getElabora() != null && !d.getElabora().isBlank()) ? d.getElabora() : "Usuario del Sistema";
                    LocalDateTime fecha = LocalDateTime.now();
                    if (d.getFechaAprobacion() != null && !d.getFechaAprobacion().isBlank()) {
                        try {
                            String cleanFecha = d.getFechaAprobacion().trim();
                            int spaceIdx = cleanFecha.indexOf(' ');
                            if (spaceIdx > 0) cleanFecha = cleanFecha.substring(0, spaceIdx);
                            String[] parts = cleanFecha.split("[/-]");
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
                                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                                    fecha = LocalDateTime.of(year, month, day, 12, 0);
                                }
                            }
                        } catch (Exception ignored) {}
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

            // Safe sorting without contract violations or NullPointerException
            dtos.sort((a, b) -> {
                String vA = (a != null && a.version() != null) ? a.version().replaceAll("[^0-9]", "") : "";
                String vB = (b != null && b.version() != null) ? b.version().replaceAll("[^0-9]", "") : "";
                int numA = 0;
                int numB = 0;
                try { if (!vA.isEmpty()) numA = Integer.parseInt(vA); } catch (Exception ignored) {}
                try { if (!vB.isEmpty()) numB = Integer.parseInt(vB); } catch (Exception ignored) {}

                if (numA != numB) {
                    return Integer.compare(numB, numA); // Higher version numbers first
                }
                LocalDateTime fA = (a != null && a.fecha() != null) ? a.fecha() : LocalDateTime.MIN;
                LocalDateTime fB = (b != null && b.fecha() != null) ? b.fecha() : LocalDateTime.MIN;
                return fB.compareTo(fA);
            });

        } catch (Exception e) {
            log.error("Error al obtener historial del documento {}: {}", documentoId, e.getMessage(), e);
        }

        return dtos;
    }
}
