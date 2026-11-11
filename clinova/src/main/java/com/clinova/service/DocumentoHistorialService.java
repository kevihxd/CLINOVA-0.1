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
import java.util.stream.Collectors;

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
        registrarHistorial(documentoId, accion, descripcion, usuario, version, LocalDateTime.now());
    }

    @Transactional
    public void registrarHistorial(Long documentoId, String accion, String descripcion, Usuario usuario, String version, LocalDateTime fecha) {
        try {
            String username = (usuario != null) ? usuario.getNombreCompleto() : "Carlos Humberto Barrera Rozo";
            DocumentoHistorial logEntry = DocumentoHistorial.builder()
                    .documentoId(documentoId)
                    .accion(accion)
                    .descripcion(descripcion)
                    .usuario(username)
                    .version(version)
                    .fecha(fecha != null ? fecha : LocalDateTime.now())
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
                    .usuario(usuario != null ? usuario : "Carlos Humberto Barrera Rozo")
                    .fecha(LocalDateTime.now())
                    .build();
            repository.save(logEntry);
        } catch (Exception e) {
            log.error("Error registrando historial para docId {}: {}", documentoId, e.getMessage());
        }
    }

    public String limpiarSaltosLiteral(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("(?:\\\\r\\\\n|\\\\n\\\\r|\\\\r|\\\\n)+", "\n").trim();
    }

    /**
     * Identifica registros automáticos del sistema en Kawak ("modificó los adjuntos...")
     * que no corresponden a comentarios humanos de Control de Cambios (rxv_tipo IS NULL).
     */
    public boolean esRuidoAdjuntosKawak(String texto) {
        if (texto == null) return false;
        String lower = texto.trim().toLowerCase();
        return lower.startsWith("el usuario ") && (lower.contains("modificó los adjuntos") || lower.contains("modifico los adjuntos"));
    }

    /**
     * Identifica textos genéricos o vacíos de Versión 1 que, según la regla de Kawak,
     * no deben generar filas sintéticas en el Control de Cambios.
     */
    public boolean esTextoGenericoV1(String texto) {
        if (texto == null || texto.isBlank()) return true;
        String t = texto.trim().toLowerCase();
        return esRuidoAdjuntosKawak(texto)
                || t.equals(".") || t.equals("-") || t.equals("--") || t.equals("—")
                || t.startsWith("creación e implementación inicial")
                || t.startsWith("creacion e implementacion inicial")
                || t.startsWith("creación y publicación inicial")
                || t.startsWith("creación del documento en el sistema")
                || t.startsWith("creacion del documento en el sistema")
                || t.equals("versión 1 del documento")
                || t.equals("version 1 del documento")
                || t.equals("versión actual del documento");
    }

    public LocalDateTime parsearFechaDocumento(Documento d) {
        if (d == null) return LocalDateTime.now();
        String raw = d.getFechaAprobacion();
        if (raw == null || raw.isBlank() || "—".equals(raw.trim()) || "--".equals(raw.trim())) {
            raw = d.getFechaRevision();
        }
        if (raw == null || raw.isBlank() || "—".equals(raw.trim()) || "--".equals(raw.trim())) {
            raw = d.getFechaElaboracion();
        }
        if (raw != null && !raw.isBlank()) {
            try {
                String cleanFecha = raw.trim();
                int spaceIdx = cleanFecha.indexOf(' ');
                if (spaceIdx > 0) cleanFecha = cleanFecha.substring(0, spaceIdx);
                int tIdx = cleanFecha.indexOf('T');
                if (tIdx > 0) cleanFecha = cleanFecha.substring(0, tIdx);
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
                    if (month >= 1 && month <= 12 && day >= 1 && day <= 31 && year >= 1990 && year <= 2100) {
                        return LocalDateTime.of(year, month, day, 12, 0);
                    }
                }
            } catch (Exception ignored) {}
        }
        return LocalDateTime.now();
    }

    @Transactional
    public void actualizarComentarioVersion(Long documentoId, String version, String nuevoComentario) {
        if (documentoId == null || nuevoComentario == null || nuevoComentario.isBlank()) return;
        try {
            String comentarioLimpio = limpiarSaltosLiteral(nuevoComentario);
            List<DocumentoHistorial> logs = repository.findByDocumentoIdOrderByFechaDesc(documentoId);
            for (DocumentoHistorial h : logs) {
                String acc = h.getAccion() != null ? h.getAccion().toUpperCase() : "";
                if ("CREACION_VERSION".equals(acc) || "CREACION".equals(acc) || "VERSION_ANTERIOR_OBSOLETA".equals(acc)) {
                    if (version == null || h.getVersion() == null || version.trim().equalsIgnoreCase(h.getVersion().trim())) {
                        h.setDescripcion(comentarioLimpio);
                        repository.save(h);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error actualizando comentario en historial para docId {}: {}", documentoId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentoHistorialDTO> obtenerHistorialPorDocumento(Long documentoId) {
        List<DocumentoHistorialDTO> dtos = new ArrayList<>();
        try {
            Documento doc = documentoRepository.findById(documentoId)
                    .or(() -> documentoRepository.findByKawakId(documentoId))
                    .orElse(null);
            if (doc == null) return dtos;

            List<Documento> relacionados = documentoRepository.findAllRelacionados(
                    doc.getId(),
                    doc.getCodigo(),
                    doc.getNombre(),
                    doc.getKawakId()
            );

            List<Long> docIds = relacionados.stream().map(Documento::getId).collect(Collectors.toList());

            List<DocumentoHistorial> existingLogs = repository.findByDocumentoIdInOrderByFechaDesc(docIds);

            for (DocumentoHistorial logEntry : existingLogs) {
                String accion = (logEntry.getAccion() != null) ? logEntry.getAccion().toUpperCase() : "";
                String desc = limpiarSaltosLiteral(logEntry.getDescripcion() != null ? logEntry.getDescripcion() : "");
                String user = (logEntry.getUsuario() != null && !logEntry.getUsuario().isBlank()) ? logEntry.getUsuario() : "Usuario del Sistema";

                boolean isSystemScheduler = "CAMBIO_ESTADO".equalsIgnoreCase(accion)
                        || "SEMAFORIZACION".equalsIgnoreCase(accion)
                        || "Sistema".equalsIgnoreCase(user)
                        || "System".equalsIgnoreCase(user)
                        || desc.toLowerCase().contains("cambió automáticamente")
                        || desc.toLowerCase().contains("días faltantes");

                String ver = logEntry.getVersion();
                Documento docAsociado = null;
                for (Documento d : relacionados) {
                    if (d.getId().equals(logEntry.getDocumentoId())) {
                        docAsociado = d;
                        break;
                    }
                }
                if (!isSystemScheduler && (ver == null || ver.isBlank() || ver.equals("0"))) {
                    if (docAsociado != null && docAsociado.getVersion() != null) {
                        ver = docAsociado.getVersion();
                    }
                }
                if (!isSystemScheduler && (ver == null || ver.isBlank())) {
                    ver = doc.getVersion() != null ? doc.getVersion() : "1";
                }
                if (docAsociado == null && ver != null) {
                    for (Documento d : relacionados) {
                        if (d.getVersion() != null && d.getVersion().trim().equalsIgnoreCase(ver.trim())) {
                            docAsociado = d;
                            break;
                        }
                    }
                }

                // Para eventos de versión (CREACION_VERSION, VERSION_ANTERIOR_OBSOLETA), usar la fecha oficial de aprobación
                // del documento de esa versión en lugar del timestamp del servidor cuando se archivó/subió.
                LocalDateTime fechaEvento = logEntry.getFecha() != null ? logEntry.getFecha() : LocalDateTime.now();
                if (docAsociado != null && ("VERSION_ANTERIOR_OBSOLETA".equals(accion) || "CREACION_VERSION".equals(accion) || "CREACION".equals(accion))) {
                    if (docAsociado.getFechaAprobacion() != null && !docAsociado.getFechaAprobacion().isBlank()) {
                        fechaEvento = parsearFechaDocumento(docAsociado);
                    }
                }

                dtos.add(new DocumentoHistorialDTO(
                        logEntry.getId(),
                        logEntry.getDocumentoId(),
                        ver,
                        logEntry.getAccion(),
                        desc,
                        user,
                        fechaEvento
                ));
            }

            for (Documento d : relacionados) {
                final String verStr = d.getVersion() != null ? d.getVersion().trim() : "1";
                boolean hasVer = dtos.stream().anyMatch(dto -> {
                    if (dto.version() == null || !verStr.equalsIgnoreCase(dto.version().trim())) return false;
                    String acc = (dto.accion() != null) ? dto.accion().toUpperCase() : "";
                    return !acc.contains("DESCARGA") && !acc.contains("VISUALIZ") && !acc.contains("ESTADO") && !acc.contains("SEMAFOR") && !acc.contains("ELIMINA");
                });
                if (!hasVer) {
                    int vInt = 1;
                    try { vInt = Integer.parseInt(verStr.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}

                    String dCC = d.getControlCambios();
                    String dDesc = d.getDescripcion();

                    // Regla oficial de Kawak: Versión 1 sin cambio explícito no genera fila sintética en Control de Cambios
                    if (vInt == 1 && esTextoGenericoV1(dCC) && esTextoGenericoV1(dDesc)) {
                        continue;
                    }

                    boolean isGenericDesc = dDesc != null && dDesc.toLowerCase().contains("creación e implementación inicial");
                    String rawChosen = (dCC != null && !dCC.isBlank() && !esRuidoAdjuntosKawak(dCC)) ? dCC
                            : (dDesc != null && !dDesc.isBlank() && !esRuidoAdjuntosKawak(dDesc) && !dDesc.equals("Versión actual del documento") && !(vInt > 1 && isGenericDesc))
                            ? dDesc : ("Actualización del documento, versión " + verStr);

                    String desc = limpiarSaltosLiteral(rawChosen);
                    String user = (d.getElabora() != null && !d.getElabora().isBlank()) ? d.getElabora() : "Usuario del Sistema";
                    LocalDateTime fecha = parsearFechaDocumento(d);
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
