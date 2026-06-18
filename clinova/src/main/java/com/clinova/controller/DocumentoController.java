package com.clinova.controller;

import com.clinova.dto.DocumentoHistorialDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.entity.Documento;
import com.clinova.entity.Usuario;
import com.clinova.repository.DocumentoRepository;
import com.clinova.service.DocumentoHistorialService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoRepository repository;
    private final DocumentoHistorialService historialService;
    private final Path root = Paths.get("uploads").toAbsolutePath().normalize();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear la carpeta de uploads");
        }
    }
    @GetMapping
    public ResponseEntity<StructureResponses<List<com.clinova.dto.DocumentoListDTO>>> obtenerTodos() {
        try {
            List<com.clinova.dto.DocumentoListDTO> lista = repository.findAllLightweight();
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Listado obtenido", lista));
        } catch (Exception e) {
            log.error("Error en obtenerTodos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/codigo-preview")
    public ResponseEntity<Map<String, String>> previewCodigo(
            @RequestParam String proceso,
            @RequestParam String tipo) {
        String codigo = generarCodigo(proceso, tipo);
        return ResponseEntity.ok(Map.of("codigo", codigo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StructureResponses<Documento>> obtenerPorId(@PathVariable Long id) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "OK", doc));
        } catch (Exception e) {
            log.error("Error en obtenerPorId [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "No encontrado", null));
        }
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<DocumentoHistorialDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(historialService.obtenerHistorialPorDocumento(id));
    }

    @PostMapping
    public ResponseEntity<StructureResponses<Documento>> crear(
            @ModelAttribute Documento documento,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            if (archivo != null && !archivo.isEmpty()) {
                String nombreArchivo = UUID.randomUUID() + "_" + archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Files.copy(archivo.getInputStream(), this.root.resolve(nombreArchivo));
                documento.setUbicacion(nombreArchivo);
            } else {
                if (documento.getUbicacion() == null || documento.getUbicacion().isEmpty()) {
                    documento.setUbicacion("SIN_ARCHIVO");
                }
            }

            if (documento.getVersion() == null || documento.getVersion().trim().isEmpty()) {
                documento.setVersion("1");
            }
            String fechaHoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            if (documento.getFechaElaboracion() == null || documento.getFechaElaboracion().trim().isEmpty()) {
                documento.setFechaElaboracion(fechaHoy);
            }
            if (documento.getFechaRevision() == null || documento.getFechaRevision().trim().isEmpty()) {
                documento.setFechaRevision(fechaHoy);
            }

            String tipoCodigo = documento.getCodigo();
            if (tipoCodigo == null || tipoCodigo.isBlank() ||
                    tipoCodigo.equalsIgnoreCase("Automático") || tipoCodigo.equalsIgnoreCase("Automatico") ||
                    tipoCodigo.equalsIgnoreCase("Semiautomático") || tipoCodigo.equalsIgnoreCase("Semiautomatico")) {
                documento.setCodigo(generarCodigo(documento.getProceso(), documento.getTipo()));
            }

            documento.setEstado("EN REVISIÓN");
            Documento guardado = repository.save(documento);
            historialService.registrarHistorial(
                    guardado.getId(), "CREACION",
                    "Documento '" + guardado.getNombre() + "' [" + guardado.getCodigo() + "] enviado a revisión",
                    usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento enviado a revisión", guardado));
        } catch (Exception e) {
            log.error("Error en crear: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StructureResponses<Documento>> actualizar(
            @PathVariable Long id,
            @RequestBody Documento cambios,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            if (cambios.getNombre()           != null) doc.setNombre(cambios.getNombre());
            if (cambios.getTipo()             != null) doc.setTipo(cambios.getTipo());
            if (cambios.getProceso()          != null) doc.setProceso(cambios.getProceso());
            if (cambios.getSede()             != null) doc.setSede(cambios.getSede());
            if (cambios.getAlcance()          != null) doc.setAlcance(cambios.getAlcance());
            if (cambios.getVersion()          != null) doc.setVersion(cambios.getVersion());
            if (cambios.getConfidencialidad() != null) doc.setConfidencialidad(cambios.getConfidencialidad());
            if (cambios.getMesesRevision()    != null) doc.setMesesRevision(cambios.getMesesRevision());
            if (cambios.getCodigo()           != null) doc.setCodigo(cambios.getCodigo());
            if (cambios.getOtrosProcesos()    != null) doc.setOtrosProcesos(cambios.getOtrosProcesos());
            if (cambios.getNormas()           != null) doc.setNormas(cambios.getNormas());
            if (cambios.getElabora()          != null) doc.setElabora(cambios.getElabora());
            if (cambios.getRevisa()           != null) doc.setRevisa(cambios.getRevisa());
            if (cambios.getAprueba()          != null) doc.setAprueba(cambios.getAprueba());
            if (cambios.getVisualizacion()    != null) doc.setVisualizacion(cambios.getVisualizacion());
            if (cambios.getImpresion()        != null) doc.setImpresion(cambios.getImpresion());
            if (cambios.getDescargaOriginal() != null) doc.setDescargaOriginal(cambios.getDescargaOriginal());
            if (cambios.getDescargaPdf()      != null) doc.setDescargaPdf(cambios.getDescargaPdf());
            if (cambios.getFechaElaboracion() != null) doc.setFechaElaboracion(cambios.getFechaElaboracion());
            if (cambios.getFechaRevision()    != null) doc.setFechaRevision(cambios.getFechaRevision());
            if (cambios.getFechaAprobacion()  != null) doc.setFechaAprobacion(cambios.getFechaAprobacion());
            Documento guardado = repository.save(doc);
            historialService.registrarHistorial(id, "MODIFICACION",
                    "Documento '" + guardado.getNombre() + "' [" + guardado.getCodigo() + "] modificado", usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento actualizado", guardado));
        } catch (Exception e) {
            log.error("Error en actualizar [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<StructureResponses<Documento>> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            doc.setEstado("VIGENTE");
            String fechaHoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            doc.setFechaAprobacion(fechaHoy);
            Documento guardado = repository.save(doc);
            historialService.registrarHistorial(
                    id, "APROBACION",
                    "Documento aprobado y publicado como VIGENTE",
                    usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Aprobado", guardado));
        } catch (Exception e) {
            log.error("Error en aprobar [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();

            // 1. Validar si es un documento sincronizado de Kawak con archivo físico
            if (doc.getRutaArchivoLocal() != null && !doc.getRutaArchivoLocal().isBlank()) {
                Path file = Paths.get(doc.getRutaArchivoLocal());
                Resource resource = new UrlResource(file.toUri());

                if (resource.exists() && resource.isReadable()) {
                    String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    String ext = doc.getExtensionArchivo() != null ? doc.getExtensionArchivo().toLowerCase() : "";
                    if (ext.equals(".pdf")) contentType = MediaType.APPLICATION_PDF_VALUE;
                    else if (ext.equals(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    else if (ext.equals(".xlsx")) contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

                    historialService.registrarHistorial(
                            id, "DESCARGA",
                            "Archivo original de Kawak descargado (" + ext + ")",
                            usuario);

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Kawak_Doc_" + doc.getKawakId() + ext + "\"")
                            .header(HttpHeaders.CONTENT_TYPE, contentType)
                            .body(resource);
                }
            }

            // 2. Si no es de Kawak, usar la lógica normal
            if (doc.getUbicacion() == null || doc.getUbicacion().equals("SIN_ARCHIVO")) {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El documento no tiene archivo físico", null));
            }

            Path file = root.resolve(doc.getUbicacion()).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                historialService.registrarHistorial(
                        id, "DESCARGA",
                        "Archivo local descargado o visualizado",
                        usuario);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNombre() + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource);
            } else {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El archivo no se encuentra en el servidor", null));
            }
        } catch (Exception e) {
            log.error("Error en descargar [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            historialService.registrarHistorial(id, "ELIMINACION", "Documento eliminado del sistema", usuario);
            repository.deleteById(id);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Eliminado", null));
        } catch (Exception e) {
            log.error("Error en eliminar [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    private String generarCodigo(String proceso, String tipo) {
        String abrevProceso = abreviarProceso(proceso);
        String abrevTipo = abreviarTipo(tipo);
        long siguiente = repository.countByCodigoStartingWith(abrevProceso + "-" + abrevTipo + "-") + 1;
        return abrevProceso + "-" + abrevTipo + "-" + siguiente;
    }

    private String abreviarProceso(String proceso) {
        if (proceso == null || proceso.isBlank()) return "DOC";
        String[] palabras = proceso.trim().toUpperCase().split("[\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String p : palabras) {
            if (p.equalsIgnoreCase("DE") || p.equalsIgnoreCase("Y") || p.equalsIgnoreCase("E")) continue;
            if (!p.isEmpty()) sb.append(p.charAt(0));
        }
        String siglas = sb.toString();
        return siglas.length() > 6 ? siglas.substring(0, 6) : siglas;
    }

    private String abreviarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return "DOC";
        return switch (tipo.trim().toUpperCase()) {
            case "PROCEDIMIENTO"    -> "PR";
            case "PROTOCOLO"        -> "PT";
            case "FORMATO"          -> "FO";
            case "MANUAL"           -> "MA";
            case "GUÍA", "GUIA"    -> "GU";
            case "INSTRUCTIVO"      -> "IN";
            case "POLÍTICA", "POLITICA" -> "PO";
            case "PROGRAMA"         -> "PG";
            case "PLAN"             -> "PL";
            case "INFORME"          -> "IF";
            case "ACTA"             -> "AC";
            case "FOLLETO"          -> "FL";
            case "AFICHE"           -> "AF";
            case "RESOLUCIÓN", "RESOLUCION" -> "RS";
            case "CIRCULAR"         -> "CI";
            case "REGISTRO"         -> "RG";
            default -> tipo.trim().toUpperCase().replaceAll("[AEIOUÁÉÍÓÚ ]", "").substring(
                    0, Math.min(3, tipo.trim().replaceAll("[AEIOUÁÉÍÓÚ ]", "").length()));
        };
    }
}