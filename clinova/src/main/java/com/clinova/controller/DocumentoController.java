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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

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

    @PersistenceContext
    private EntityManager entityManager;

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
        debugInfo.put("jarVersion", "v9-eb82554-FileLocatorPriority");
        debugInfo.put("fileIndexSize", fileLocator.getIndexSize());
        debugInfo.put("rootUploads", fileLocator.getRootUploads());
        if (query != null && !query.isBlank()) {
            debugInfo.put("queryResult", fileLocator.buscarArchivo(query));
            debugInfo.put("matchingKeys", fileLocator.findMatchingKeys(query));
        }
        return ResponseEntity.ok(debugInfo);
    }

    @GetMapping("/debug-doc/{id}")
    public ResponseEntity<?> debugDoc(@PathVariable Long id) {
        try {
            Documento doc = repository.findById(id).orElse(null);
            if (doc == null) return ResponseEntity.status(404).body("Documento no encontrado");
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("id", doc.getId());
            info.put("kawakId", doc.getKawakId());
            info.put("nombre", doc.getNombre());
            info.put("ubicacion", doc.getUbicacion());
            info.put("ubicacionPdf", doc.getUbicacionPdf());
            info.put("rutaArchivoLocal", doc.getRutaArchivoLocal());
            Long kId = doc.getKawakId() != null ? doc.getKawakId() : doc.getId();
            Long docId = doc.getId();
            // Probar los candidatos clave
            List<String> testCandidatos = List.of(
                "files/Formatos/1/" + kId + ".pdf",
                "files/Formatos/1/" + docId + ".pdf",
                "files/formatos/1/" + kId + ".pdf",
                "files/formatos/1/" + docId + ".pdf",
                kId + ".pdf",
                docId + ".pdf"
            );
            Map<String, Object> resoluciones = new LinkedHashMap<>();
            for (String cand : testCandidatos) {
                Path p = fileLocator.buscarArchivo(cand);
                resoluciones.put(cand, p != null ? p.toString() + " [exists=" + Files.exists(p) + "]" : "null");
            }
            info.put("resoluciones", resoluciones);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<StructureResponses<Documento>> crear(
            @ModelAttribute Documento documento,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "archivoPdf", required = false) MultipartFile archivoPdf,
            @AuthenticationPrincipal Usuario usuario) {
        try {
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

            // Guardar archivos usando el ID como nombre físico (esquema Kawak)
            Long fileId = guardado.getKawakId() != null ? guardado.getKawakId() : guardado.getId();
            boolean updatedFiles = false;

            if (archivo != null && !archivo.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String origName = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
                String ext = "";
                int lastDot = origName.lastIndexOf('.');
                if (lastDot >= 0) ext = origName.substring(lastDot);

                String nombreArchivo = fileId + ext;
                Path targetPath = folder.resolve(nombreArchivo);
                Files.copy(archivo.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fileLocator.registrarNuevoArchivo(targetPath);
                guardado.setUbicacion(nombreArchivo);
                guardado.setRutaArchivoLocal(nombreArchivo);
                if (origName.toLowerCase().endsWith(".pdf")) {
                    guardado.setUbicacionPdf(nombreArchivo);
                }
                updatedFiles = true;
            } else if (guardado.getUbicacion() == null || guardado.getUbicacion().isEmpty()) {
                guardado.setUbicacion("SIN_ARCHIVO");
            }

            if (archivoPdf != null && !archivoPdf.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String nombrePdf = fileId + ".pdf";
                Path targetPdfPath = folder.resolve(nombrePdf);
                Files.copy(archivoPdf.getInputStream(), targetPdfPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fileLocator.registrarNuevoArchivo(targetPdfPath);
                guardado.setUbicacionPdf(nombrePdf);
                if (guardado.getRutaArchivoLocal() == null || guardado.getRutaArchivoLocal().isEmpty() || "SIN_ARCHIVO".equals(guardado.getRutaArchivoLocal())) {
                    guardado.setRutaArchivoLocal(nombrePdf);
                }
                updatedFiles = true;
            }

            if (updatedFiles) {
                guardado = repository.save(guardado);
            }

            String histDesc = (guardado.getControlCambios() != null && !guardado.getControlCambios().isBlank()) 
                    ? guardado.getControlCambios() 
                    : "Documento creado y enviado a revisión";
            historialService.registrarHistorial(guardado.getId(), "CREACION", histDesc, usuario, guardado.getVersion());
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
            }
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento actualizado", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @Transactional
    @PutMapping("/{id}/cambiar-id")
    public ResponseEntity<StructureResponses<Documento>> cambiarIdDocumento(
            @PathVariable Long id,
            @RequestParam("nuevoId") Long nuevoId,
            @RequestParam("password") String password) {
        try {
            if (!"admin123".equals(password)) {
                return ResponseEntity.status(401).body(new StructureResponses<>("ERROR", "Contraseña de administrador incorrecta", null));
            }
            if (repository.existsById(nuevoId)) {
                return ResponseEntity.badRequest().body(new StructureResponses<>("ERROR", "El ID " + nuevoId + " ya existe en la base de datos", null));
            }
            entityManager.createNativeQuery("UPDATE documentos SET id = :nuevoId, kawak_id = :nuevoId WHERE id = :id OR kawak_id = :id")
                    .setParameter("nuevoId", nuevoId)
                    .setParameter("id", id)
                    .executeUpdate();
            
            try {
                entityManager.createNativeQuery("UPDATE documentos_historial SET documento_id = :nuevoId WHERE documento_id = :id")
                        .setParameter("nuevoId", nuevoId)
                        .setParameter("id", id)
                        .executeUpdate();
            } catch (Exception ignored) {}

            Documento actualizado = repository.findById(nuevoId).orElse(null);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "ID actualizado exitosamente a " + nuevoId, actualizado));
        } catch (Exception e) {
            log.error("Error al cambiar ID de documento {} a {}: {}", id, nuevoId, e.getMessage());
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/actualizar")
    public ResponseEntity<StructureResponses<Documento>> actualizarContenido(
            @PathVariable Long id,
            @ModelAttribute Documento cambios,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "archivoPdf", required = false) MultipartFile archivoPdf,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow(() -> new RuntimeException("Documento no encontrado"));
            Long fileId = doc.getKawakId() != null ? doc.getKawakId() : doc.getId();

            if (archivo != null && !archivo.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String origName = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
                String ext = "";
                int lastDot = origName.lastIndexOf('.');
                if (lastDot >= 0) ext = origName.substring(lastDot);

                String nombreArchivo = fileId + ext;
                Path targetPath = folder.resolve(nombreArchivo);
                Files.copy(archivo.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fileLocator.registrarNuevoArchivo(targetPath);
                doc.setUbicacion(nombreArchivo);
                doc.setRutaArchivoLocal(nombreArchivo);
                if (origName.toLowerCase().endsWith(".pdf")) {
                    doc.setUbicacionPdf(nombreArchivo);
                }
            }

            if (archivoPdf != null && !archivoPdf.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String nombrePdf = fileId + ".pdf";
                Path targetPdfPath = folder.resolve(nombrePdf);
                Files.copy(archivoPdf.getInputStream(), targetPdfPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fileLocator.registrarNuevoArchivo(targetPdfPath);
                doc.setUbicacionPdf(nombrePdf);
                if (doc.getRutaArchivoLocal() == null || doc.getRutaArchivoLocal().isEmpty() || "SIN_ARCHIVO".equals(doc.getRutaArchivoLocal())) {
                    doc.setRutaArchivoLocal(nombrePdf);
                }
            }

            if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) doc.setNombre(cambios.getNombre());
            if (cambios.getTipo() != null && !cambios.getTipo().isBlank()) doc.setTipo(cambios.getTipo());
            if (cambios.getProceso() != null && !cambios.getProceso().isBlank()) doc.setProceso(cambios.getProceso());
            if (cambios.getSede() != null && !cambios.getSede().isBlank()) doc.setSede(cambios.getSede());
            if (cambios.getAlcance() != null && !cambios.getAlcance().isBlank()) doc.setAlcance(cambios.getAlcance());
            if (cambios.getCodigo() != null && !cambios.getCodigo().isBlank()) doc.setCodigo(cambios.getCodigo());
            if (cambios.getElabora() != null) doc.setElabora(cambios.getElabora());
            if (cambios.getRevisa() != null) doc.setRevisa(cambios.getRevisa());
            if (cambios.getAprueba() != null) doc.setAprueba(cambios.getAprueba());
            if (cambios.getVisualizacion() != null) doc.setVisualizacion(cambios.getVisualizacion());
            if (cambios.getImpresion() != null) doc.setImpresion(cambios.getImpresion());
            if (cambios.getDescargaOriginal() != null) doc.setDescargaOriginal(cambios.getDescargaOriginal());
            if (cambios.getDescargaPdf() != null) doc.setDescargaPdf(cambios.getDescargaPdf());

            Documento guardado = repository.save(doc);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Contenido del documento actualizado sin alterar la trazabilidad", guardado));
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
            @RequestParam(value = "codigo", required = false) String codigo,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "proceso", required = false) String proceso,
            @RequestParam(value = "sede", required = false) String sede,
            @RequestParam(value = "alcance", required = false) String alcance,
            @RequestParam(value = "elabora", required = false) String elabora,
            @RequestParam(value = "revisa", required = false) String revisa,
            @RequestParam(value = "aprueba", required = false) String aprueba,
            @RequestParam(value = "visualizacion", required = false) String visualizacion,
            @RequestParam(value = "impresion", required = false) String impresion,
            @RequestParam(value = "descargaOriginal", required = false) String descargaOriginal,
            @RequestParam(value = "descargaPdf", required = false) String descargaPdf,
            @RequestParam(value = "fechaElaboracion", required = false) String fechaElaboracion,
            @RequestParam(value = "fechaRevision", required = false) String fechaRevision,
            @RequestParam(value = "fechaAprobacion", required = false) String fechaAprobacion,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow(() -> new RuntimeException("Documento no encontrado"));

            String oldVersion = doc.getVersion();
            String prevDesc = (doc.getControlCambios() != null && !doc.getControlCambios().isBlank()) 
                    ? doc.getControlCambios() 
                    : (doc.getDescripcion() != null && !doc.getDescripcion().isBlank()) ? doc.getDescripcion() : ("Versión " + oldVersion + " del documento");

            // 1. Archivar versión previa en OBSOLETOS (mantiene su ID histórico)
            doc.setEstado("OBSOLETO");
            if (doc.getControlCambios() == null || doc.getControlCambios().isBlank()) {
                doc.setControlCambios(prevDesc);
            }
            repository.save(doc);
            historialService.registrarHistorial(doc.getId(), "VERSION_ANTERIOR_OBSOLETA", prevDesc, usuario, oldVersion);

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

            String descLog = (controlCambios != null && !controlCambios.trim().isEmpty()) 
                    ? controlCambios.trim() 
                    : "Creación e implementación de la versión " + nuevaVerStr;
            String fechaHoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            // 3. Crear el nuevo registro VIGENTE (obtiene un nuevo ID autoincremental)
            Documento nuevoVigente = Documento.builder()
                    .kawakId(null)
                    .codigo(codigo != null && !codigo.isBlank() ? codigo.trim() : doc.getCodigo())
                    .nombre(nombre != null && !nombre.isBlank() ? nombre.trim() : doc.getNombre())
                    .tipo(tipo != null && !tipo.isBlank() ? tipo.trim() : doc.getTipo())
                    .proceso(proceso != null && !proceso.isBlank() ? proceso.trim() : doc.getProceso())
                    .sede(sede != null && !sede.isBlank() ? sede.trim() : doc.getSede())
                    .version(nuevaVerStr.trim())
                    .estado("VIGENTE")
                    .metodoCreacion(doc.getMetodoCreacion())
                    .alcance(alcance != null && !alcance.isBlank() ? alcance.trim() : doc.getAlcance())
                    .confidencialidad(doc.getConfidencialidad())
                    .mesesRevision(doc.getMesesRevision())
                    .otrosProcesos(doc.getOtrosProcesos())
                    .normas(doc.getNormas())
                    .elabora(elabora != null ? elabora : doc.getElabora())
                    .revisa(revisa != null ? revisa : doc.getRevisa())
                    .aprueba(aprueba != null ? aprueba : doc.getAprueba())
                    .visualizacion(visualizacion != null ? visualizacion : doc.getVisualizacion())
                    .impresion(impresion != null ? impresion : doc.getImpresion())
                    .descargaOriginal(descargaOriginal != null ? descargaOriginal : doc.getDescargaOriginal())
                    .descargaPdf(descargaPdf != null ? descargaPdf : doc.getDescargaPdf())
                    .fechaElaboracion(fechaElaboracion != null && !fechaElaboracion.isBlank() ? fechaElaboracion.trim() : (doc.getFechaElaboracion() != null ? doc.getFechaElaboracion() : "30/06/2026"))
                    .fechaRevision(fechaRevision != null && !fechaRevision.isBlank() ? fechaRevision.trim() : fechaHoy)
                    .fechaAprobacion(fechaAprobacion != null && !fechaAprobacion.isBlank() ? fechaAprobacion.trim() : fechaHoy)
                    .controlCambios(descLog)
                    .descripcion(descLog)
                    .ubicacion(doc.getUbicacion())
                    .ubicacionPdf(doc.getUbicacionPdf())
                    .rutaArchivoLocal(doc.getRutaArchivoLocal())
                    .build();

            Documento guardado = repository.save(nuevoVigente);

            Long fileId = guardado.getKawakId() != null ? guardado.getKawakId() : guardado.getId();

            if (archivo != null && !archivo.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String origName = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
                String ext = "";
                int lastDot = origName.lastIndexOf('.');
                if (lastDot >= 0) ext = origName.substring(lastDot).toLowerCase();
                if (ext.isEmpty()) ext = ".docx";

                String nombreArchivo = fileId + ext;
                Path targetPath = folder.resolve(nombreArchivo);
                Files.copy(archivo.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fileLocator.registrarNuevoArchivo(targetPath);
                guardado.setUbicacion(nombreArchivo);
                guardado.setRutaArchivoLocal(nombreArchivo);
                if (ext.endsWith(".pdf") && (archivoPdf == null || archivoPdf.isEmpty())) {
                    guardado.setUbicacionPdf(nombreArchivo);
                }
            }

            if (archivoPdf != null && !archivoPdf.isEmpty()) {
                Path folder = this.root.resolve("documentos");
                Files.createDirectories(folder);
                String nombrePdf = fileId + ".pdf";
                Path targetPdfPath = folder.resolve(nombrePdf);
                Files.copy(archivoPdf.getInputStream(), targetPdfPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                fileLocator.registrarNuevoArchivo(targetPdfPath);
                guardado.setUbicacionPdf(nombrePdf);
                if (guardado.getRutaArchivoLocal() == null || guardado.getRutaArchivoLocal().isEmpty() || "SIN_ARCHIVO".equals(guardado.getRutaArchivoLocal())) {
                    guardado.setRutaArchivoLocal(nombrePdf);
                }
            }

            guardado = repository.save(guardado);
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
            Long docId = doc.getId();

            List<String> prefixes = List.of("", "LMD", "LMR", "LMC", "LMP");
            List<String> subdirs = List.of("files/Formatos/1", "files/Documentos/1", "files/Externos/1", "files/Formatos/" + kId, "files/Documentos/" + kId, "files/Externos/" + kId, "files/Formatos", "files/Documentos", "files/Externos");

            if ("original".equalsIgnoreCase(tipo)) {
                // 1. Probar primero ubicaciones registradas en BD que NO sean PDF (Word, Excel, etc.)
                if (!isInvalidPath(doc.getRutaArchivoLocal()) && !doc.getRutaArchivoLocal().toLowerCase().endsWith(".pdf")) candidatos.add(doc.getRutaArchivoLocal());
                if (!isInvalidPath(doc.getUbicacion()) && !doc.getUbicacion().toLowerCase().endsWith(".pdf")) candidatos.add(doc.getUbicacion());

                // 2. Probar archivos de Office en disco (.docx, .xlsx, .doc, .xls, .zip)
                for (String dir : subdirs) {
                    for (String pfx : prefixes) {
                        for (String ext : List.of(".docx", ".xlsx", ".doc", ".xls", ".pub", ".zip")) {
                            candidatos.add(dir + "/" + pfx + kId + ext);
                            candidatos.add(dir + "/" + pfx + docId + ext);
                        }
                    }
                }

                // 3. Fallback a ubicaciones registradas en BD que sean PDF si no existe archivo de Office
                if (!isInvalidPath(doc.getRutaArchivoLocal())) candidatos.add(doc.getRutaArchivoLocal());
                if (!isInvalidPath(doc.getUbicacion())) candidatos.add(doc.getUbicacion());
                if (!isInvalidPath(doc.getUbicacionPdf()) && !doc.getUbicacionPdf().toLowerCase().endsWith(".swf")) candidatos.add(doc.getUbicacionPdf());
            } else if ("pdf".equalsIgnoreCase(tipo)) {
                // Para PDF: probar primero las rutas PDF específicas guardadas en BD y archivos .pdf en disco
                if (!isInvalidPath(doc.getUbicacionPdf()) && !doc.getUbicacionPdf().toLowerCase().endsWith(".swf")) {
                    candidatos.add(doc.getUbicacionPdf());
                }
                if (!isInvalidPath(doc.getUbicacion()) && doc.getUbicacion().toLowerCase().endsWith(".pdf")) {
                    candidatos.add(doc.getUbicacion());
                }
                if (!isInvalidPath(doc.getRutaArchivoLocal()) && doc.getRutaArchivoLocal().toLowerCase().endsWith(".pdf")) {
                    candidatos.add(doc.getRutaArchivoLocal());
                }
                for (String dir : subdirs) {
                    for (String pfx : prefixes) {
                        candidatos.add(dir + "/" + pfx + kId + ".pdf");
                        candidatos.add(dir + "/" + pfx + docId + ".pdf");
                    }
                }
            }

            // Candidatos para archivos Office y otros formatos originales (xlsx, docx, xls, doc, pub, zip)
            for (String dir : subdirs) {
                for (String pfx : prefixes) {
                    for (String ext : List.of(".xlsx", ".docx", ".xls", ".doc", ".pdf", ".pub", ".zip")) {
                        candidatos.add(dir + "/" + pfx + kId + ext);
                        candidatos.add(dir + "/" + pfx + docId + ext);
                    }
                }
            }

            // Agregar ubicaciones generales registradas en BD como fallback final
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

            byte[] header = new byte[8];
            try (java.io.InputStream is = Files.newInputStream(file)) {
                int read = is.read(header);
            } catch (Exception ignored) {}

            boolean isPdf = header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46; // %PDF
            boolean isZip = header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04; // PK.. (docx/xlsx)
            boolean isOle = (header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF && (header[2] & 0xFF) == 0x11 && (header[3] & 0xFF) == 0xE0; // OLE Binary (doc/xls)

            String diskName = file.getFileName().toString().toLowerCase();
            String ext = "";
            int lastDot = diskName.lastIndexOf('.');
            if (lastDot > 0) {
                ext = diskName.substring(lastDot);
            }

            String contentType = "application/octet-stream";

            if (isPdf || ext.endsWith(".pdf")) {
                ext = ".pdf";
                contentType = MediaType.APPLICATION_PDF_VALUE;
            } else if (isOle) {
                if (ext.contains("xls")) {
                    ext = ".xls";
                    contentType = "application/vnd.ms-excel";
                } else {
                    ext = ".doc";
                    contentType = "application/msword";
                }
            } else if (isZip || ext.endsWith(".docx") || ext.endsWith(".xlsx")) {
                if (ext.contains("xls")) {
                    ext = ".xlsx";
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                } else {
                    ext = ".docx";
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                }
            }

            try {
                historialService.registrarHistorial(id, "DESCARGA", "Archivo descargado o visualizado", usuario);
            } catch (Exception ignored) {}

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

            String disposition = ("pdf".equalsIgnoreCase(tipo) || ext.endsWith(".pdf")) ? "inline" : "attachment";

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
        String baseName = Paths.get(cleanedName).getFileName().toString();

        // 1. Probar rutas directas exactas PRIMERO (garantiza servir el archivo exacto sin colisiones)
        try {
            Path directPath = root.resolve(cleanedName).normalize();
            if (Files.exists(directPath) && Files.isRegularFile(directPath)) return directPath;

            Path plainPath = root.resolve(strippedName).normalize();
            if (Files.exists(plainPath) && Files.isRegularFile(plainPath)) return plainPath;

            Path dockerPath = Paths.get("/app/uploads").resolve(cleanedName).normalize();
            if (Files.exists(dockerPath) && Files.isRegularFile(dockerPath)) return dockerPath;

            Path dockerPlain = Paths.get("/app/uploads").resolve(strippedName).normalize();
            if (Files.exists(dockerPlain) && Files.isRegularFile(dockerPlain)) return dockerPlain;

            Path dockerDataPath = Paths.get("/app/uploads/data").resolve(strippedName).normalize();
            if (Files.exists(dockerDataPath) && Files.isRegularFile(dockerDataPath)) return dockerDataPath;

            Path absPath = Paths.get(cleanedName);
            if (Files.exists(absPath) && Files.isRegularFile(absPath)) return absPath;
        } catch (Exception ignored) {}

        // 2. Probar via FileLocatorService (rutas reales indexadas del sistema Linux)
        Path fromService = fileLocator.buscarArchivo(cleanedName);
        if (fromService != null && Files.exists(fromService) && Files.isReadable(fromService)) return fromService;

        fromService = fileLocator.buscarArchivo(strippedName);
        if (fromService != null && Files.exists(fromService) && Files.isReadable(fromService)) return fromService;

        fromService = fileLocator.buscarArchivo(baseName);
        if (fromService != null && Files.exists(fromService) && Files.isReadable(fromService)) return fromService;

        try {
            // 3. Probar subcarpetas directas (incluyendo subcarpeta /documentos/)
            Path docSubfolderPath = root.resolve("documentos").resolve(baseName).normalize();
            if (Files.exists(docSubfolderPath) && Files.isRegularFile(docSubfolderPath)) return docSubfolderPath;

            Path dataPath = root.resolve("data/" + strippedName).normalize();
            if (Files.exists(dataPath) && Files.isRegularFile(dataPath)) return dataPath;
        } catch (Exception ignored) {}

        try {
            // 3. Probar como rutas absolutas (incluyendo /app/uploads/documentos/)
            Path absPath = Paths.get(cleanedName);
            if (Files.exists(absPath) && Files.isRegularFile(absPath)) return absPath;

            Path absDocPath = Paths.get("/app/uploads/documentos/" + baseName);
            if (Files.exists(absDocPath) && Files.isRegularFile(absDocPath)) return absDocPath;

            Path absDataPath = Paths.get("/app/uploads/data/" + strippedName);
            if (Files.exists(absDataPath) && Files.isRegularFile(absDataPath)) return absDataPath;

            Path absPlainPath = Paths.get("/app/uploads/" + strippedName);
            if (Files.exists(absPlainPath) && Files.isRegularFile(absPlainPath)) return absPlainPath;
        } catch (Exception ignored) {}

        // 4. Probar subcarpetas conocidas (Restaurado para resolver 100% de archivos Kawak)
        String[] carpetasExtendidas = {
            "files/Formatos/1", 
            "files/Documentos/1", 
            "files/Externos/1", 
            "documentos/1",
            "documentos", 
            "files/Formatos", 
            "files/Documentos", 
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

            Path dockerDataPath = Paths.get("/app/uploads/data").resolve(dir).resolve(baseName).normalize();
            if (Files.exists(dockerDataPath) && Files.isRegularFile(dockerDataPath)) return dockerDataPath;
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