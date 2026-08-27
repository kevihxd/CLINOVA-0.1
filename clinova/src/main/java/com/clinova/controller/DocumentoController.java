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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/documentos", "/api/documentos"})
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

    @GetMapping("/obsoletos")
    public ResponseEntity<StructureResponses<List<com.clinova.dto.DocumentoListDTO>>> obtenerObsoletos() {
        try {
            List<com.clinova.dto.DocumentoListDTO> lista = repository.findAllObsoletosLightweight();
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Obsoletos obtenidos", lista));
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
        try {
            return ResponseEntity.ok(historialService.obtenerHistorialPorDocumento(id));
        } catch (Exception e) {
            log.error("Error al obtener historial para id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @GetMapping("/debug-files")
    public ResponseEntity<?> debugFiles(@RequestParam(value = "q", required = false) String query) {
        Map<String, Object> debugInfo = new LinkedHashMap<>();
        debugInfo.put("fileIndexSize", fileLocator.getIndexSize());
        debugInfo.put("rootUploads", fileLocator.getRootUploads());
        if (query != null && !query.isBlank()) {
            debugInfo.put("queryResult", fileLocator.buscarArchivo(query));
            debugInfo.put("matchingKeys", fileLocator.findMatchingKeys(query));
        }
        return ResponseEntity.ok(debugInfo);
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
                if (documento.getRutaArchivoLocal() == null || documento.getRutaArchivoLocal().isEmpty() || "SIN_ARCHIVO".equals(documento.getRutaArchivoLocal())) {
                    documento.setRutaArchivoLocal(nombreArchivo);
                }
            } else if (documento.getUbicacion() == null || documento.getUbicacion().isEmpty()) {
                documento.setUbicacion("SIN_ARCHIVO");
            }

            if (archivoPdf != null && !archivoPdf.isEmpty()) {
                String nombrePdf = UUID.randomUUID() + "_" + archivoPdf.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Files.copy(archivoPdf.getInputStream(), this.root.resolve("documentos").resolve(nombrePdf));
                documento.setUbicacionPdf(nombrePdf);
                if (documento.getRutaArchivoLocal() == null || documento.getRutaArchivoLocal().isEmpty() || "SIN_ARCHIVO".equals(documento.getRutaArchivoLocal())) {
                    documento.setRutaArchivoLocal(nombrePdf);
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
            if (tipoCodigo == null || tipoCodigo.isBlank() || tipoCodigo.equalsIgnoreCase("Automático") || tipoCodigo.equalsIgnoreCase("Semiautomático")) {
                documento.setCodigo(generarCodigo(documento.getProceso(), documento.getTipo()));
            }

            if (documento.getControlCambios() != null && !documento.getControlCambios().isBlank()) {
                if (documento.getDescripcion() == null || documento.getDescripcion().isBlank()) {
                    documento.setDescripcion(documento.getControlCambios());
                }
            } else if (documento.getDescripcion() != null && !documento.getDescripcion().isBlank()) {
                documento.setControlCambios(documento.getDescripcion());
            }

            documento.setEstado("EN REVISIÓN");
            Documento guardado = repository.save(documento);
            String histDesc = (documento.getControlCambios() != null && !documento.getControlCambios().isBlank()) 
                    ? documento.getControlCambios() 
                    : "Documento creado y enviado a revisión";
            historialService.registrarHistorial(guardado.getId(), "CREACION", histDesc, usuario);
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
            String oldVersion = doc.getVersion();
            String oldClean = (oldVersion != null) ? oldVersion.replaceAll("[^0-9]", "").trim() : "";
            String newClean = (cambios.getVersion() != null) ? cambios.getVersion().replaceAll("[^0-9]", "").trim() : "";
            boolean isNewVersion = !newClean.isEmpty() && !oldClean.isEmpty() && !newClean.equals(oldClean);

            if (isNewVersion) {
                String prevDesc = (doc.getControlCambios() != null && !doc.getControlCambios().isBlank()) 
                        ? doc.getControlCambios() 
                        : (doc.getDescripcion() != null && !doc.getDescripcion().isBlank()) ? doc.getDescripcion() : ("Versión " + oldVersion + " del documento");

                // Clonar versión previa y enviarla a OBSOLETOS
                Documento obsoletoDoc = Documento.builder()
                        .codigo(doc.getCodigo())
                        .nombre(doc.getNombre())
                        .tipo(doc.getTipo())
                        .proceso(doc.getProceso())
                        .sede(doc.getSede())
                        .version(oldVersion != null ? oldVersion : "1")
                        .estado("OBSOLETO")
                        .metodoCreacion(doc.getMetodoCreacion())
                        .alcance(doc.getAlcance())
                        .confidencialidad(doc.getConfidencialidad())
                        .mesesRevision(doc.getMesesRevision())
                        .otrosProcesos(doc.getOtrosProcesos())
                        .normas(doc.getNormas())
                        .elabora(doc.getElabora())
                        .revisa(doc.getRevisa())
                        .aprueba(doc.getAprueba())
                        .visualizacion(doc.getVisualizacion())
                        .impresion(doc.getImpresion())
                        .descargaOriginal(doc.getDescargaOriginal())
                        .descargaPdf(doc.getDescargaPdf())
                        .fechaElaboracion(doc.getFechaElaboracion())
                        .fechaRevision(doc.getFechaRevision())
                        .fechaAprobacion(doc.getFechaAprobacion())
                        .rutaArchivoLocal(doc.getRutaArchivoLocal())
                        .ubicacion(doc.getUbicacion())
                        .ubicacionPdf(doc.getUbicacionPdf())
                        .controlCambios(prevDesc)
                        .descripcion(prevDesc)
                        .build();
                Documento obsoletoGuardado = repository.save(obsoletoDoc);
                historialService.registrarHistorial(obsoletoGuardado.getId(), "CREACION_VERSION", prevDesc, usuario, oldVersion);
                
                String fechaHoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                doc.setFechaRevision(fechaHoy);
                doc.setFechaAprobacion(fechaHoy);
            }

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
            if (cambios.getUbicacion() != null) doc.setUbicacion(cambios.getUbicacion());
            if (cambios.getUbicacionPdf() != null) doc.setUbicacionPdf(cambios.getUbicacionPdf());
            if (cambios.getRequiereAprobacion() != null) doc.setRequiereAprobacion(cambios.getRequiereAprobacion());
            if (cambios.getPermisoVisualizacionRegistros() != null) doc.setPermisoVisualizacionRegistros(cambios.getPermisoVisualizacionRegistros());
            if (cambios.getEdicionAprobadores() != null) doc.setEdicionAprobadores(cambios.getEdicionAprobadores());
            if (cambios.getEdicionOtros() != null) doc.setEdicionOtros(cambios.getEdicionOtros());
            if (cambios.getEdicionSolicitante() != null) doc.setEdicionSolicitante(cambios.getEdicionSolicitante());
            if (cambios.getReiniciarCicloAprobacion() != null) doc.setReiniciarCicloAprobacion(cambios.getReiniciarCicloAprobacion());
            if (cambios.getRequiereSeguimiento() != null) doc.setRequiereSeguimiento(cambios.getRequiereSeguimiento());
            if (cambios.getEvaluarGestion() != null) doc.setEvaluarGestion(cambios.getEvaluarGestion());
            if (cambios.getRetencionDonde() != null) doc.setRetencionDonde(cambios.getRetencionDonde());
            if (cambios.getRetencionComo() != null) doc.setRetencionComo(cambios.getRetencionComo());
            if (cambios.getRetencionRecuperacion() != null) doc.setRetencionRecuperacion(cambios.getRetencionRecuperacion());
            if (cambios.getRetencionTiempo() != null) doc.setRetencionTiempo(cambios.getRetencionTiempo());
            if (cambios.getDisposicionFinal() != null) doc.setDisposicionFinal(cambios.getDisposicionFinal());
            if (cambios.getQuienDiligencia() != null) doc.setQuienDiligencia(cambios.getQuienDiligencia());
            if (cambios.getQuienProtege() != null) doc.setQuienProtege(cambios.getQuienProtege());
            if (cambios.getQuienDisposicion() != null) doc.setQuienDisposicion(cambios.getQuienDisposicion());
            if (cambios.getLogo() != null) doc.setLogo(cambios.getLogo());
            if (cambios.getControlCambios() != null && !cambios.getControlCambios().isBlank()) {
                doc.setControlCambios(cambios.getControlCambios());
                doc.setDescripcion(cambios.getControlCambios());
            }

            Documento guardado = repository.save(doc);

            if (isNewVersion) {
                String versionComment = (guardado.getControlCambios() != null && !guardado.getControlCambios().isBlank()) 
                        ? guardado.getControlCambios() 
                        : ("Nueva versión " + cambios.getVersion() + " creada e implementada");
                historialService.registrarHistorial(guardado.getId(), "CREACION_VERSION", versionComment, usuario, cambios.getVersion());
            } else {
                historialService.registrarHistorial(guardado.getId(), "MODIFICACION", "Documento modificado", usuario);
            }
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento actualizado", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/nueva-version")
    public ResponseEntity<StructureResponses<Documento>> nuevaVersion(
            @PathVariable Long id,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "controlCambios", required = false) String controlCambios,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "archivoPdf", required = false) MultipartFile archivoPdf,
            @RequestParam(value = "elabora", required = false) String elabora,
            @RequestParam(value = "revisa", required = false) String revisa,
            @RequestParam(value = "aprueba", required = false) String aprueba,
            @RequestParam(value = "visualizacion", required = false) String visualizacion,
            @RequestParam(value = "impresion", required = false) String impresion,
            @RequestParam(value = "descargaOriginal", required = false) String descargaOriginal,
            @RequestParam(value = "descargaPdf", required = false) String descargaPdf,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow(() -> new RuntimeException("Documento no encontrado"));

            String oldVersion = doc.getVersion();

            String prevDesc = (doc.getControlCambios() != null && !doc.getControlCambios().isBlank()) 
                    ? doc.getControlCambios() 
                    : (doc.getDescripcion() != null && !doc.getDescripcion().isBlank()) ? doc.getDescripcion() : ("Versión " + oldVersion + " del documento");

            // 1. Archivar versión previa en OBSOLETOS
            Documento obsoletoDoc = Documento.builder()
                    .codigo(doc.getCodigo())
                    .nombre(doc.getNombre())
                    .tipo(doc.getTipo())
                    .proceso(doc.getProceso())
                    .sede(doc.getSede())
                    .version(oldVersion != null ? oldVersion : "1")
                    .estado("OBSOLETO")
                    .metodoCreacion(doc.getMetodoCreacion())
                    .alcance(doc.getAlcance())
                    .confidencialidad(doc.getConfidencialidad())
                    .mesesRevision(doc.getMesesRevision())
                    .otrosProcesos(doc.getOtrosProcesos())
                    .normas(doc.getNormas())
                    .elabora(doc.getElabora())
                    .revisa(doc.getRevisa())
                    .aprueba(doc.getAprueba())
                    .visualizacion(doc.getVisualizacion())
                    .impresion(doc.getImpresion())
                    .descargaOriginal(doc.getDescargaOriginal())
                    .descargaPdf(doc.getDescargaPdf())
                    .fechaElaboracion(doc.getFechaElaboracion())
                    .fechaRevision(doc.getFechaRevision())
                    .fechaAprobacion(doc.getFechaAprobacion())
                    .rutaArchivoLocal(doc.getRutaArchivoLocal())
                    .ubicacion(doc.getUbicacion())
                    .ubicacionPdf(doc.getUbicacionPdf())
                    .controlCambios(prevDesc)
                    .descripcion(prevDesc)
                    .build();
            Documento obsoletoGuardado = repository.save(obsoletoDoc);
            historialService.registrarHistorial(obsoletoGuardado.getId(), "CREACION_VERSION", prevDesc, usuario, oldVersion);

            // 2. Incrementar número de versión para el nuevo registro vigente
            String nuevaVerStr = version;
            if (nuevaVerStr == null || nuevaVerStr.trim().isEmpty()) {
                int vNum = 1;
                try {
                    if (oldVersion != null) {
                        vNum = Integer.parseInt(oldVersion.replaceAll("[^0-9]", ""));
                    }
                } catch(Exception ignored) {}
                vNum++;
                nuevaVerStr = String.valueOf(vNum);
            }
            doc.setVersion(nuevaVerStr.trim());

            if (archivo != null && !archivo.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String origName = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
                String nombreArchivo = UUID.randomUUID() + "_" + origName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Files.copy(archivo.getInputStream(), folder.resolve(nombreArchivo), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                doc.setUbicacion(nombreArchivo);
                doc.setRutaArchivoLocal(nombreArchivo);
                if (origName.toLowerCase().endsWith(".pdf")) {
                    doc.setUbicacionPdf(nombreArchivo);
                }
            }

            if (archivoPdf != null && !archivoPdf.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String nombrePdf = UUID.randomUUID() + "_" + archivoPdf.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Files.copy(archivoPdf.getInputStream(), folder.resolve(nombrePdf), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                doc.setUbicacionPdf(nombrePdf);
                if (doc.getRutaArchivoLocal() == null || doc.getRutaArchivoLocal().isEmpty() || "SIN_ARCHIVO".equals(doc.getRutaArchivoLocal())) {
                    doc.setRutaArchivoLocal(nombrePdf);
                }
            }

            if (elabora != null) doc.setElabora(elabora);
            if (revisa != null) doc.setRevisa(revisa);
            if (aprueba != null) doc.setAprueba(aprueba);
            if (visualizacion != null) doc.setVisualizacion(visualizacion);
            if (impresion != null) doc.setImpresion(impresion);
            if (descargaOriginal != null) doc.setDescargaOriginal(descargaOriginal);
            if (descargaPdf != null) doc.setDescargaPdf(descargaPdf);

            if (controlCambios != null && !controlCambios.trim().isEmpty()) {
                doc.setControlCambios(controlCambios.trim());
                doc.setDescripcion(controlCambios.trim());
            } else {
                doc.setControlCambios("Creación e implementación de la versión " + nuevaVerStr);
                doc.setDescripcion("Creación e implementación de la versión " + nuevaVerStr);
            }

            String fechaHoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            doc.setFechaRevision(fechaHoy);
            doc.setFechaAprobacion(fechaHoy);
            doc.setEstado("VIGENTE");

            Documento guardado = repository.save(doc);

            String descLog = (controlCambios != null && !controlCambios.trim().isEmpty()) 
                    ? controlCambios.trim() 
                    : "Creación e implementación de la nueva versión " + nuevaVerStr;
            historialService.registrarHistorial(guardado.getId(), "CREACION_VERSION", descLog, usuario, nuevaVerStr);

            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Nueva versión creada exitosamente", guardado));
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

    @PutMapping("/{id}/obsoleto")
    public ResponseEntity<StructureResponses<Documento>> marcarObsoleto(
            @PathVariable Long id, 
            @RequestParam(value = "motivo", required = false, defaultValue = "Documento marcado como obsoleto por el administrador") String motivo,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            doc.setEstado("OBSOLETO");
            Documento guardado = repository.save(doc);
            historialService.registrarHistorial(id, "OBSOLETO", "Documento pasado a OBSOLETO: " + motivo, usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento marcado como OBSOLETO", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/restaurar")
    public ResponseEntity<StructureResponses<Documento>> restaurarObsoleto(
            @PathVariable Long id, 
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            doc.setEstado("VIGENTE");
            Documento guardado = repository.save(doc);
            historialService.registrarHistorial(id, "RESTAURACION", "Documento restaurado a VIGENTE desde OBSOLETO", usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento restaurado a VIGENTE", guardado));
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
            Documento doc = repository.findById(id).orElse(null);
            if (doc == null) {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El documento con ID " + id + " no existe", null));
            }

            String archivoReal = null;
            
            // Probar candidatos de archivo en orden de prioridad
            List<String> candidatos = new ArrayList<>();
            Long kId = doc.getKawakId() != null ? doc.getKawakId() : doc.getId();

            // 1. Probar rutas directas de Kawak usando kawak_id
            candidatos.add("files/Formatos/" + kId + "/" + kId + ".pdf");
            candidatos.add("files/Formatos/" + kId + "/LMD" + kId + ".pdf");
            candidatos.add("files/Formatos/" + kId + "/LMR" + kId + ".pdf");
            candidatos.add("files/Documentos/" + kId + "/" + kId + ".pdf");
            candidatos.add("files/Documentos/" + kId + "/LMD" + kId + ".pdf");
            candidatos.add("files/Documentos/" + kId + "/LMR" + kId + ".pdf");
            candidatos.add("files/Externos/" + kId + "/" + kId + ".pdf");

            if ("pdf".equalsIgnoreCase(tipo)) {
                if (!isInvalidPath(doc.getUbicacionPdf()) && doc.getUbicacionPdf() != null && !doc.getUbicacionPdf().toLowerCase().endsWith(".swf")) {
                    candidatos.add(doc.getUbicacionPdf());
                }
                if (!isInvalidPath(doc.getUbicacion()) && doc.getUbicacion() != null && doc.getUbicacion().toLowerCase().endsWith(".pdf")) {
                    candidatos.add(doc.getUbicacion());
                }
                if (!isInvalidPath(doc.getRutaArchivoLocal()) && doc.getRutaArchivoLocal() != null && doc.getRutaArchivoLocal().toLowerCase().endsWith(".pdf")) {
                    candidatos.add(doc.getRutaArchivoLocal());
                }
            }

            // Candidatos con kawakId para archivos Office original
            candidatos.add("files/Formatos/" + kId + "/" + kId + ".docx");
            candidatos.add("files/Formatos/" + kId + "/" + kId + ".xlsx");
            candidatos.add("files/Formatos/" + kId + "/" + kId + ".xls");
            candidatos.add("files/Documentos/" + kId + "/" + kId + ".docx");
            candidatos.add("files/Documentos/" + kId + "/" + kId + ".xlsx");
            candidatos.add("files/Externos/" + kId + "/" + kId + ".docx");

            // Agregar ubicaciones generales como fallback
            if (!isInvalidPath(doc.getRutaArchivoLocal())) candidatos.add(doc.getRutaArchivoLocal());
            if (!isInvalidPath(doc.getUbicacion())) candidatos.add(doc.getUbicacion());
            if (!isInvalidPath(doc.getUbicacionPdf()) && doc.getUbicacionPdf() != null && !doc.getUbicacionPdf().toLowerCase().endsWith(".swf")) candidatos.add(doc.getUbicacionPdf());

            Path file = null;
            for (String cand : candidatos) {
                if (cand == null || cand.isBlank()) continue;
                file = buscarArchivoFisico(cand, "documentos");
                if (file != null && Files.exists(file) && Files.isReadable(file)) {
                    break;
                }
                // Probar reemplazando extensión a .pdf / .docx / .xlsx
                int dot = cand.lastIndexOf('.');
                if (dot > 0) {
                    String base = cand.substring(0, dot);
                    for (String ext : List.of(".pdf", ".docx", ".xlsx", ".xls", ".doc")) {
                        Path tryExt = buscarArchivoFisico(base + ext, "documentos");
                        if (tryExt != null && Files.exists(tryExt) && Files.isReadable(tryExt)) {
                            file = tryExt;
                            break;
                        }
                    }
                }
                if (file != null) break;
            }

            if (file == null || !Files.exists(file) || !Files.isReadable(file)) {
                log.warn("Archivo físico no encontrado para Documento id={}", id);
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El documento no tiene archivo físico registrado en el servidor", null));
            }

            Resource resource = new UrlResource(file.toUri());
            String contentType = "application/octet-stream";
            String filename = file.getFileName().toString().toLowerCase();

            if (filename.endsWith(".pdf")) contentType = MediaType.APPLICATION_PDF_VALUE;
            else if (filename.endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            else if (filename.endsWith(".xlsx")) contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            try {
                historialService.registrarHistorial(id, "DESCARGA", "Archivo descargado o visualizado", usuario);
            } catch (Exception ignored) {}

            String ext = "";
            String diskName = file.getFileName().toString();
            int lastDot = diskName.lastIndexOf('.');
            if (lastDot > 0) {
                ext = diskName.substring(lastDot);
            }

            String codigoPrefix = (doc.getCodigo() != null && !doc.getCodigo().isBlank() && !"--".equals(doc.getCodigo().trim())) 
                    ? doc.getCodigo().trim() + " - " : "";
            String docNombre = doc.getNombre() != null && !doc.getNombre().isBlank() ? doc.getNombre().trim() : "documento";
            String displayFilename = codigoPrefix + docNombre + ext;
            String safeFilename = displayFilename.replaceAll("[\\\\/:*?\"<>|]", "_");
            
            // Generate clean ASCII filename for RFC header compatibility with Tomcat
            String asciiFilename = java.text.Normalizer.normalize(safeFilename, java.text.Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("[^a-zA-Z0-9._ -]", "_")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (asciiFilename.isBlank()) asciiFilename = "documento" + ext;

            String encodedFilename = java.net.URLEncoder.encode(safeFilename, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");

            String disposition = ("pdf".equalsIgnoreCase(tipo) || filename.endsWith(".pdf")) ? "inline" : "attachment";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + asciiFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (Exception e) {
            log.error("Error al descargar documento id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "No se pudo acceder al archivo físico del documento: " + e.getMessage(), null));
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
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty() || "SIN_ARCHIVO".equalsIgnoreCase(nombreArchivo.trim())) return null;

        String cleanedName = nombreArchivo.trim().replace("\\", "/");
        if (cleanedName.startsWith("/")) cleanedName = cleanedName.substring(1);
        String strippedName = cleanedName.startsWith("data/") ? cleanedName.substring(5) : cleanedName;

        try {
            // 1. Probar la ruta directa unida a rootUploads
            Path directPath = root.resolve(cleanedName).normalize();
            if (Files.exists(directPath) && Files.isRegularFile(directPath)) return directPath;

            Path dataPath = root.resolve("data/" + strippedName).normalize();
            if (Files.exists(dataPath) && Files.isRegularFile(dataPath)) return dataPath;

            Path plainPath = root.resolve(strippedName).normalize();
            if (Files.exists(plainPath) && Files.isRegularFile(plainPath)) return plainPath;
        } catch (Exception ignored) {}

        try {
            // 2. Probar como ruta absoluta
            Path absPath = Paths.get(cleanedName);
            if (Files.exists(absPath) && Files.isRegularFile(absPath)) return absPath;

            Path absDataPath = Paths.get("/app/uploads/data/" + strippedName);
            if (Files.exists(absDataPath) && Files.isRegularFile(absDataPath)) return absDataPath;

            Path absPlainPath = Paths.get("/app/uploads/" + strippedName);
            if (Files.exists(absPlainPath) && Files.isRegularFile(absPlainPath)) return absPlainPath;
        } catch (Exception ignored) {}

        // 3. Probar via FileLocatorService (búsqueda en todo el índice en memoria)
        Path fromService = fileLocator.buscarArchivo(cleanedName);
        if (fromService != null && Files.exists(fromService)) return fromService;

        fromService = fileLocator.buscarArchivo(strippedName);
        if (fromService != null && Files.exists(fromService)) return fromService;

        // Extraer nombre de archivo simple para fallback
        String baseName = Paths.get(cleanedName).getFileName().toString();
        fromService = fileLocator.buscarArchivo(baseName);
        if (fromService != null && Files.exists(fromService)) return fromService;

        // 4. Probar subcarpetas conocidas
        String[] carpetasExtendidas = {
            "documentos", 
            "files/Formatos/1", 
            "files/Documentos/1", 
            "files/Externos", 
            "files/Internos", 
            "soportes/otros_soportes", 
            "soportes/sin_clasificar", 
            "certificados", 
            "fotos", 
            "soportes"
        };
        for (String dir : carpetasExtendidas) {
            Path path = root.resolve(dir).resolve(baseName).normalize();
            if (Files.exists(path) && Files.isRegularFile(path)) return path;

            Path dockerPath = Paths.get("/app/uploads").resolve(dir).resolve(baseName).normalize();
            if (Files.exists(dockerPath) && Files.isRegularFile(dockerPath)) return dockerPath;
        }

        return null;
    }

    private String generarCodigo(String proceso, String tipo) {
        String abrevProceso = abreviarProceso(proceso);
        String abrevTipo = abreviarTipo(tipo);
        String prefix = abrevProceso + "-" + abrevTipo + "-";
        
        List<String> codigosExistentes = repository.findCodigosByPrefix(prefix);
        long maxNum = 0;
        for (String c : codigosExistentes) {
            if (c != null) {
                int lastDash = c.lastIndexOf('-');
                if (lastDash != -1 && lastDash < c.length() - 1) {
                    try {
                        long num = Long.parseLong(c.substring(lastDash + 1).trim());
                        if (num > maxNum) maxNum = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return prefix + (maxNum + 1);
    }

    private String abreviarProceso(String proceso) {
        if (proceso == null || proceso.isBlank()) return "DOC";
        String pUpper = proceso.trim().toUpperCase();
        if (pUpper.contains("TALENTO HUMANO")) return "PTH";
        if (pUpper.contains("SEGURIDAD Y SALUD")) return "PSST";
        if (pUpper.contains("SEGURIDAD DEL PACIENTE")) return "PSP";
        if (pUpper.contains("SIAU")) return "PSIAU";
        if (pUpper.contains("CALIDAD")) return "PGC";
        if (pUpper.contains("FINANCIERA")) return "PGF";
        if (pUpper.contains("INFRAESTRUCTURA")) return "PGI";
        if (pUpper.contains("COMERCIAL")) return "PGCM";
        if (pUpper.contains("ESTRATÉGICA") || pUpper.contains("ESTRATEGI")) return "PGE";
        if (pUpper.contains("HUMANIZACIÓN") || pUpper.contains("HUMANIZACI")) return "PGH";
        if (pUpper.contains("SALUD PÚBLICA") || pUpper.contains("SALUD PUBLICA")) return "PSPU";
        if (pUpper.contains("CONSULTA EXTERNA")) return "PGCE";
        if (pUpper.contains("DOMICILIARIO") || pUpper.contains("INTERNACIÓN")) return "PGID";
        if (pUpper.contains("APOYO DIAGNOSTICO")) return "PGAD";
        if (pUpper.contains("TECNOLOGÍA") || pUpper.contains("TECNOLOGIA") || pUpper.contains("SISTEMAS")) return "PTSI";
        if (pUpper.contains("COMPRAS")) return "PGCO";
        if (pUpper.contains("ARCHIVO")) return "PGA";
        if (pUpper.contains("COMUNICACIONES")) return "PGCOM";

        String[] palabras = pUpper.split("[\\s]+");
        StringBuilder sb = new StringBuilder();
        if (!pUpper.startsWith("P")) sb.append("P");
        for (String p : palabras) {
            if (p.equalsIgnoreCase("DE") || p.equalsIgnoreCase("Y") || p.equalsIgnoreCase("E") || p.equalsIgnoreCase("EN") || p.equalsIgnoreCase("DEL")) continue;
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

    private boolean isInvalidPath(String path) {
        if (path == null || path.trim().isEmpty()) return true;
        String p = path.trim().toUpperCase();
        return p.equals("SIN_ARCHIVO") || p.equals("KAWAK") || p.equals("NONE") || p.equals("NULL");
    }
}