package com.clinova.controller;

import com.clinova.dto.DocumentoHistorialDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.entity.Documento;
import com.clinova.entity.Usuario;
import com.clinova.repository.DocumentoRepository;
import com.clinova.service.DocumentoHistorialService;
import com.clinova.service.FileLocatorService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
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
    private final FileLocatorService fileLocator;

    @Value("${uploads.root-path:uploads}")
    private String uploadsRootPath;

    private Path root;

    @PostConstruct
    public void init() {
        root = Paths.get(uploadsRootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            log.warn("No se pudo crear la carpeta de uploads en: {}", root);
        }
        log.info("DocumentoController usando uploads en: {}", root);
    }

    @GetMapping
    public ResponseEntity<StructureResponses<List<com.clinova.dto.DocumentoListDTO>>> obtenerTodos() {
        try {
            List<com.clinova.dto.DocumentoListDTO> lista = repository.findAllLightweight();
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Listado obtenido", lista));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/codigo-preview")
    public ResponseEntity<Map<String, String>> previewCodigo(@RequestParam String proceso, @RequestParam String tipo) {
        return ResponseEntity.ok(Map.of("codigo", generarCodigo(proceso, tipo)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StructureResponses<Documento>> obtenerPorId(@PathVariable Long id) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "OK", doc));
        } catch (Exception e) {
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
            @RequestParam(value = "archivoPdf", required = false) MultipartFile archivoPdf,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            if (archivo != null && !archivo.isEmpty()) {
                String nombreArchivo = UUID.randomUUID() + "_" + archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Files.copy(archivo.getInputStream(), this.root.resolve("documentos").resolve(nombreArchivo));
                documento.setUbicacion(nombreArchivo);
            } else if (documento.getUbicacion() == null || documento.getUbicacion().isEmpty()) {
                documento.setUbicacion("SIN_ARCHIVO");
            }

            if (archivoPdf != null && !archivoPdf.isEmpty()) {
                String nombrePdf = UUID.randomUUID() + "_" + archivoPdf.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Files.copy(archivoPdf.getInputStream(), this.root.resolve("documentos").resolve(nombrePdf));
                documento.setUbicacionPdf(nombrePdf);
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
            if (tipoCodigo == null || tipoCodigo.isBlank() || tipoCodigo.equalsIgnoreCase("Automático") || tipoCodigo.equalsIgnoreCase("Semiautomático")) {
                documento.setCodigo(generarCodigo(documento.getProceso(), documento.getTipo()));
            }

            documento.setEstado("EN REVISIÓN");
            Documento guardado = repository.save(documento);
            historialService.registrarHistorial(guardado.getId(), "CREACION", "Documento enviado a revisión", usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento enviado a revisión", guardado));
        } catch (Exception e) {
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
            if (cambios.getNombre() != null) doc.setNombre(cambios.getNombre());
            if (cambios.getTipo() != null) doc.setTipo(cambios.getTipo());
            if (cambios.getProceso() != null) doc.setProceso(cambios.getProceso());
            if (cambios.getSede() != null) doc.setSede(cambios.getSede());
            if (cambios.getAlcance() != null) doc.setAlcance(cambios.getAlcance());
            if (cambios.getVersion() != null) doc.setVersion(cambios.getVersion());
            if (cambios.getConfidencialidad() != null) doc.setConfidencialidad(cambios.getConfidencialidad());
            if (cambios.getMesesRevision() != null) doc.setMesesRevision(cambios.getMesesRevision());
            if (cambios.getCodigo() != null) doc.setCodigo(cambios.getCodigo());
            if (cambios.getOtrosProcesos() != null) doc.setOtrosProcesos(cambios.getOtrosProcesos());
            if (cambios.getNormas() != null) doc.setNormas(cambios.getNormas());
            if (cambios.getElabora() != null) doc.setElabora(cambios.getElabora());
            if (cambios.getRevisa() != null) doc.setRevisa(cambios.getRevisa());
            if (cambios.getAprueba() != null) doc.setAprueba(cambios.getAprueba());
            if (cambios.getVisualizacion() != null) doc.setVisualizacion(cambios.getVisualizacion());
            if (cambios.getImpresion() != null) doc.setImpresion(cambios.getImpresion());
            if (cambios.getDescargaOriginal() != null) doc.setDescargaOriginal(cambios.getDescargaOriginal());
            if (cambios.getDescargaPdf() != null) doc.setDescargaPdf(cambios.getDescargaPdf());
            if (cambios.getFechaElaboracion() != null) doc.setFechaElaboracion(cambios.getFechaElaboracion());
            if (cambios.getFechaRevision() != null) doc.setFechaRevision(cambios.getFechaRevision());
            if (cambios.getFechaAprobacion() != null) doc.setFechaAprobacion(cambios.getFechaAprobacion());
            Documento guardado = repository.save(doc);
            historialService.registrarHistorial(id, "MODIFICACION", "Documento modificado", usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento actualizado", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<StructureResponses<Documento>> aprobar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            doc.setEstado("VIGENTE");
            doc.setFechaAprobacion(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            Documento guardado = repository.save(doc);
            historialService.registrarHistorial(id, "APROBACION", "Documento aprobado y publicado como VIGENTE", usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Aprobado", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargar(
            @PathVariable Long id, 
            @RequestParam(value = "tipo", required = false) String tipo,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();

            String archivoReal = null;
            
            // Si el cliente pide la vista previa en PDF, intentamos buscar primero el archivo PDF
            if ("pdf".equalsIgnoreCase(tipo)) {
                archivoReal = doc.getUbicacionPdf();
                if (archivoReal == null || archivoReal.trim().isEmpty() || archivoReal.equals("SIN_ARCHIVO")) {
                    String baseFile = doc.getRutaArchivoLocal();
                    if (baseFile == null || baseFile.trim().isEmpty() || baseFile.equals("SIN_ARCHIVO")) {
                        baseFile = doc.getUbicacion();
                    }
                    if (baseFile != null && !baseFile.equals("SIN_ARCHIVO")) {
                        int dotIndex = baseFile.lastIndexOf('.');
                        if (dotIndex > 0) {
                            String guessPdf = baseFile.substring(0, dotIndex) + ".pdf";
                            Path checkPath = buscarArchivoFisico(guessPdf, "documentos");
                            if (checkPath != null && Files.exists(checkPath)) {
                                archivoReal = guessPdf;
                            } else {
                                archivoReal = baseFile;
                            }
                        } else {
                            archivoReal = baseFile;
                        }
                    }
                }
            } else {
                archivoReal = doc.getRutaArchivoLocal();
            }

            if (archivoReal == null || archivoReal.trim().isEmpty() || archivoReal.equals("SIN_ARCHIVO")) {
                archivoReal = doc.getUbicacion();
            }

            if (archivoReal == null || archivoReal.trim().isEmpty() || archivoReal.equals("SIN_ARCHIVO")) {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El documento no tiene archivo físico", null));
            }

            Path file = buscarArchivoFisico(archivoReal, "documentos");

            if (file != null && Files.exists(file) && Files.isReadable(file)) {
                Resource resource = new UrlResource(file.toUri());
                String contentType = "application/octet-stream";
                String filename = archivoReal.toLowerCase();

                if (filename.endsWith(".pdf")) contentType = MediaType.APPLICATION_PDF_VALUE;
                else if (filename.endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                else if (filename.endsWith(".xlsx")) contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

                historialService.registrarHistorial(id, "DESCARGA", "Archivo descargado o visualizado", usuario);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + archivoReal + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El archivo no se encuentra en el servidor", null));
            }
        } catch (Exception e) {
            log.error("Error al descargar documento: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> eliminar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        try {
            historialService.registrarHistorial(id, "ELIMINACION", "Documento eliminado del sistema", usuario);
            repository.deleteById(id);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Eliminado", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    private Path buscarArchivoFisico(String nombreArchivo, String subcarpeta) {
        Path fromService = fileLocator.buscarArchivo(nombreArchivo);
        if (fromService != null) return fromService;

        Path path = root.resolve(subcarpeta).resolve(nombreArchivo).normalize();
        if (Files.exists(path)) return path;

        path = root.resolve(nombreArchivo).normalize();
        if (Files.exists(path)) return path;

        String[] carpetasExtendidas = {"soportes/otros_soportes", "soportes/sin_clasificar", "certificados", "fotos", "soportes"};
        for (String dir : carpetasExtendidas) {
            path = root.resolve(dir).resolve(nombreArchivo).normalize();
            if (Files.exists(path)) return path;
        }

        return null;
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
            default -> tipo.trim().toUpperCase().replaceAll("[AEIOUÁÉÍÓÚ ]", "").substring(0, Math.min(3, tipo.trim().replaceAll("[AEIOUÁÉÍÓÚ ]", "").length()));
        };
    }
}