package com.clinova.controller;

import com.clinova.dto.StructureResponses;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Role;
import com.clinova.entity.Soporte;
import com.clinova.entity.Usuario;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SoporteRepository;
import com.clinova.repository.UsuarioRepository;
import com.clinova.service.FileLocatorService;
import com.clinova.service.HojaVidaHistorialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping({"/api/v1/soportes", "/api/soportes"})
@RequiredArgsConstructor
public class SoporteController {

    private final SoporteRepository soporteRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FileLocatorService fileLocator;
    private final ObjectMapper objectMapper;
    private final HojaVidaHistorialService hojaVidaHistorialService;

    @Value("${uploads.root-path:uploads}")
    private String uploadsRootPath;

    private Path rootUploads;

    @PostConstruct
    public void init() {
        rootUploads = Paths.get(uploadsRootPath).toAbsolutePath().normalize();
    }

    /** Lista todos los soportes de una hoja de vida */
    @GetMapping("/hoja-vida/{hojaVidaId}")
    public ResponseEntity<List<Soporte>> listarPorHojaVida(@PathVariable Long hojaVidaId) {
        return ResponseEntity.ok(soporteRepository.findByHojaVidaIdOrderByIdDesc(hojaVidaId));
    }

    /** Sube un nuevo documento soporte para una hoja de vida */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirSoporte(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestPart("datos") String datosJson,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            // Parsear metadatos enviados como JSON string
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = objectMapper.readValue(datosJson, Map.class);
            Long hojaVidaId = Long.valueOf(datos.getOrDefault("hojaVidaId", "0").toString());
            String tipoDocumento = datos.getOrDefault("tipoDocumento", "Otros").toString();

            HojaVida hojaVida = hojaVidaRepository.findById(hojaVidaId)
                    .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada: " + hojaVidaId));

            // Crear carpeta destino
            Path destDir = rootUploads.resolve("soportes").resolve(String.valueOf(hojaVidaId));
            Files.createDirectories(destDir);

            // Nombre único para evitar colisiones
            String originalName = archivo.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".pdf";
            String uniqueName = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + extension;
            Path destFile = destDir.resolve(uniqueName);

            Files.copy(archivo.getInputStream(), destFile, StandardCopyOption.REPLACE_EXISTING);

            String rutaRelativa = "soportes/" + hojaVidaId + "/" + uniqueName;

            Soporte soporte = Soporte.builder()
                    .tipoDocumento(tipoDocumento)
                    .nombreArchivo(originalName != null ? originalName : uniqueName)
                    .rutaArchivo(rutaRelativa)
                    .tamano(archivo.getSize())
                    .fechaCarga(LocalDateTime.now())
                    .estado("Vigente")
                    .hojaVida(hojaVida)
                    .build();

            Soporte guardado = soporteRepository.save(soporte);
            log.info("Soporte subido [hojaVidaId={}, tipo={}, archivo={}]", hojaVidaId, tipoDocumento, uniqueName);

            // Registrar en trazabilidad detallada
            try {
                hojaVidaHistorialService.registrarHistorial(
                        hojaVidaId,
                        "CARGA_SOPORTE",
                        "Se subió el soporte '" + (originalName != null ? originalName : uniqueName) + "' en la carpeta '" + tipoDocumento + "'",
                        usuario
                );
            } catch (Exception exHist) {
                log.warn("No se pudo registrar historial para carga de soporte: {}", exHist.getMessage());
            }

            return ResponseEntity.ok(guardado);

        } catch (IOException e) {
            log.error("Error de I/O al subir soporte: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "Error al guardar el archivo"));
        } catch (Exception e) {
            log.error("Error al subir soporte: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        try {
            Soporte soporte = soporteRepository.findById(id).orElseThrow();

            if (soporte.getRutaArchivo() == null || soporte.getRutaArchivo().isEmpty()) {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "No hay archivo físico asociado al soporte", null));
            }

            Path file = buscarArchivoFisico(soporte.getRutaArchivo());

            if (file != null && Files.exists(file) && Files.isReadable(file)) {
                Resource resource = new UrlResource(file.toUri());
                String contentType = "application/octet-stream";
                String filename = soporte.getRutaArchivo().toLowerCase();

                if (filename.endsWith(".pdf")) contentType = MediaType.APPLICATION_PDF_VALUE;
                else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = MediaType.IMAGE_JPEG_VALUE;
                else if (filename.endsWith(".png")) contentType = MediaType.IMAGE_PNG_VALUE;

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + soporte.getNombreArchivo() + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                log.warn("Archivo de soporte no encontrado en disco [id={}, ruta={}]", id, soporte.getRutaArchivo());
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "Archivo no encontrado en el servidor", null));
            }
        } catch (Exception e) {
            log.error("Error al descargar soporte [id={}]: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    /** Actualiza el tipoDocumento / categoría de un soporte (acepta vía query param o body) */
    @PutMapping({"/{id}/categoria", "/{id}/tipo"})
    @Transactional
    public ResponseEntity<?> cambiarCategoria(
            @PathVariable Long id,
            @RequestParam(name = "tipoDocumento", required = false) String tipoDocumentoParam,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Soporte soporte = soporteRepository.findByIdWithHojaVida(id)
                    .orElseGet(() -> soporteRepository.findById(id).orElse(null));
            if (soporte == null) {
                return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "Soporte no encontrado con ID: " + id));
            }

            String nuevaCategoria = tipoDocumentoParam;

            if ((nuevaCategoria == null || nuevaCategoria.trim().isEmpty()) && body != null) {
                nuevaCategoria = body.get("tipoDocumento");
                if (nuevaCategoria == null || nuevaCategoria.trim().isEmpty()) {
                    nuevaCategoria = body.get("categoria");
                }
            }

            if (nuevaCategoria == null || nuevaCategoria.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "Debe especificar la nueva categoría o tipoDocumento"));
            }

            String catAnterior = soporte.getTipoDocumento();
            soporte.setTipoDocumento(nuevaCategoria.trim());
            Soporte actualizado = soporteRepository.save(soporte);

            // Registrar en trazabilidad detallada
            try {
                if (soporte.getHojaVida() != null) {
                    hojaVidaHistorialService.registrarHistorial(
                            soporte.getHojaVida().getId(),
                            "CAMBIO_CARPETA_SOPORTE",
                            "Se movió el soporte '" + soporte.getNombreArchivo() + "' de la carpeta '" + (catAnterior != null ? catAnterior : "Sin categoría") + "' a la carpeta '" + nuevaCategoria.trim() + "'",
                            usuario
                    );
                }
            } catch (Exception exHist) {
                log.warn("No se pudo registrar historial para cambio de carpeta: {}", exHist.getMessage());
            }

            log.info("Tipo/Categoría de soporte actualizada [id={}, nuevaCategoria={}]", id, nuevaCategoria);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Categoría actualizada correctamente", "data", actualizado));
        } catch (Exception e) {
            log.error("Error al actualizar categoría de soporte [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /** Actualiza el nombreArchivo de un soporte (acepta vía query param o body) */
    @PutMapping("/{id}/nombre")
    @Transactional
    public ResponseEntity<?> cambiarNombre(
            @PathVariable Long id,
            @RequestParam(name = "nombreArchivo", required = false) String nombreParam,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Soporte soporte = soporteRepository.findByIdWithHojaVida(id)
                    .orElseGet(() -> soporteRepository.findById(id).orElse(null));
            if (soporte == null) {
                return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "Soporte no encontrado con ID: " + id));
            }

            String nuevoNombre = nombreParam;

            if ((nuevoNombre == null || nuevoNombre.trim().isEmpty()) && body != null) {
                nuevoNombre = body.get("nombreArchivo");
                if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                    nuevoNombre = body.get("nombre");
                }
            }

            if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "Debe especificar el nuevo nombre del archivo"));
            }

            String nombreAnterior = soporte.getNombreArchivo();
            soporte.setNombreArchivo(nuevoNombre.trim());
            Soporte actualizado = soporteRepository.save(soporte);

            // Registrar en trazabilidad detallada
            try {
                if (soporte.getHojaVida() != null) {
                    hojaVidaHistorialService.registrarHistorial(
                            soporte.getHojaVida().getId(),
                            "RENOMBRAR_SOPORTE",
                            "Se cambió el nombre del soporte de '" + nombreAnterior + "' a '" + nuevoNombre.trim() + "' (carpeta '" + (soporte.getTipoDocumento() != null ? soporte.getTipoDocumento() : "Sin categoría") + "')",
                            usuario
                    );
                }
            } catch (Exception exHist) {
                log.warn("No se pudo registrar historial para renombrar soporte: {}", exHist.getMessage());
            }

            log.info("Nombre de soporte actualizado [id={}, nuevoNombre={}]", id, nuevoNombre);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Nombre actualizado correctamente", "data", actualizado));
        } catch (Exception e) {
            log.error("Error al actualizar nombre de soporte [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /** Elimina un soporte por ID (y su archivo físico del disco si existe) */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(
            @PathVariable Long id,
            @RequestParam(name = "hojaVidaId", required = false) Long hojaVidaIdParam,
            @RequestParam(name = "tipoDocumento", required = false) String tipoDocumentoParam,
            @RequestParam(name = "nombreArchivo", required = false) String nombreArchivoParam,
            @RequestParam(name = "usuario", required = false) String usuarioParam,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            // Determinar el usuario que realiza la operación
            Usuario userEjecutor = usuario;
            String nombreUsuario = null;
            if (userEjecutor != null && userEjecutor.getUsername() != null && !userEjecutor.getUsername().trim().isEmpty()) {
                nombreUsuario = userEjecutor.getUsername();
            } else if (usuarioParam != null && !usuarioParam.trim().isEmpty()) {
                nombreUsuario = usuarioParam.trim();
                userEjecutor = usuarioRepository.findByUsername(nombreUsuario).orElse(null);
            }
            if (userEjecutor == null) {
                try {
                    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                        nombreUsuario = auth.getName();
                        userEjecutor = usuarioRepository.findByUsername(nombreUsuario).orElse(null);
                    }
                } catch (Exception ignored) {}
            }

            if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
                nombreUsuario = "Sistema";
            }

            // Verificar permisos: Los contratistas (USER / USER_PRACTICANTE) no pueden eliminar soportes
            if (userEjecutor != null && userEjecutor.getRol() != Role.ADMIN && userEjecutor.getRol() != Role.HR_MANAGER) {
                log.warn("Acceso denegado: Usuario {} con rol {} intentó eliminar el soporte con ID {}", 
                        userEjecutor.getUsername(), userEjecutor.getRol(), id);
                return ResponseEntity.status(403).body(Map.of(
                        "status", "ERROR", 
                        "message", "Los contratistas no tienen permisos para eliminar soportes documentales."
                ));
            }

            Soporte soporte = soporteRepository.findByIdWithHojaVida(id)
                    .orElseGet(() -> soporteRepository.findById(id).orElse(null));

            if (soporte == null) {
                return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "Soporte no encontrado con ID: " + id));
            }

            Long hojaVidaId = (soporte.getHojaVida() != null) ? soporte.getHojaVida().getId() : hojaVidaIdParam;
            String nombreArchivo = (soporte.getNombreArchivo() != null && !soporte.getNombreArchivo().trim().isEmpty())
                    ? soporte.getNombreArchivo().trim()
                    : (nombreArchivoParam != null ? nombreArchivoParam.trim() : "");
            String tipoDocumento = (soporte.getTipoDocumento() != null && !soporte.getTipoDocumento().trim().isEmpty())
                    ? soporte.getTipoDocumento().trim()
                    : (tipoDocumentoParam != null ? tipoDocumentoParam.trim() : "General");

            // Intentar eliminar el archivo físico del disco (no crítico si falla)
            if (soporte.getRutaArchivo() != null && !soporte.getRutaArchivo().isEmpty()) {
                try {
                    Path file = buscarArchivoFisico(soporte.getRutaArchivo());
                    if (file != null && Files.exists(file)) {
                        Files.delete(file);
                        log.info("Archivo físico eliminado: {}", file);
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo eliminar el archivo físico [id={}, ruta={}]: {}", id, soporte.getRutaArchivo(), ex.getMessage());
                }
            }

            soporteRepository.deleteById(id);

            // Registrar en trazabilidad detallada
            try {
                if (hojaVidaId != null) {
                    String descripcion = (nombreArchivo != null && !nombreArchivo.isEmpty())
                            ? "Se eliminó adjunto '" + nombreArchivo + "' en carpeta \"" + tipoDocumento + "\""
                            : "Se eliminó adjunto en carpeta \"" + tipoDocumento + "\"";

                    hojaVidaHistorialService.registrarHistorial(
                            hojaVidaId,
                            "ELIMINACION",
                            descripcion,
                            nombreUsuario
                    );
                    log.info("Historial registrado con éxito [hojaVidaId={}, usuario={}, desc={}]", hojaVidaId, nombreUsuario, descripcion);
                } else {
                    log.warn("No se pudo asociar hojaVidaId para el soporte eliminado con id={}", id);
                }
            } catch (Exception exHist) {
                log.error("Error al registrar historial para eliminación de soporte: {}", exHist.getMessage(), exHist);
            }

            log.info("Soporte eliminado correctamente [id={}, usuario={}]", id, nombreUsuario);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Documento eliminado correctamente"));
        } catch (Exception e) {
            log.error("Error al eliminar soporte [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /** Rechaza un soporte registrando motivo */
    @PutMapping("/{id}/rechazar")
    @Transactional
    public ResponseEntity<?> rechazarSoporte(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            if (usuario != null && usuario.getRol() != Role.ADMIN && usuario.getRol() != Role.HR_MANAGER) {
                return ResponseEntity.status(403).body(Map.of("status", "ERROR", "message", "No tiene permisos para rechazar soportes."));
            }

            Soporte soporte = soporteRepository.findByIdWithHojaVida(id)
                    .orElseGet(() -> soporteRepository.findById(id).orElse(null));
            if (soporte == null) {
                return ResponseEntity.status(404).body(Map.of("status", "ERROR", "message", "Soporte no encontrado con ID: " + id));
            }

            soporte.setEstado("Rechazado");
            Soporte guardado = soporteRepository.save(soporte);

            String motivo = body != null ? body.getOrDefault("motivo", "") : "";
            try {
                if (soporte.getHojaVida() != null) {
                    hojaVidaHistorialService.registrarHistorial(
                            soporte.getHojaVida().getId(),
                            "RECHAZO_SOPORTE",
                            "Se rechazó el soporte '" + soporte.getNombreArchivo() + "' de la carpeta '" + (soporte.getTipoDocumento() != null ? soporte.getTipoDocumento() : "Sin categoría") + "'" + (motivo.isEmpty() ? "" : ". Motivo: " + motivo),
                            usuario
                    );
                }
            } catch (Exception exHist) {
                log.warn("No se pudo registrar historial de rechazo: {}", exHist.getMessage());
            }

            return ResponseEntity.ok(Map.of("status", "OK", "message", "Soporte rechazado correctamente", "data", guardado));
        } catch (Exception e) {
            log.error("Error al rechazar soporte [id={}]: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    private Path buscarArchivoFisico(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) return null;
        
        // Limpiar "uploads/" del inicio si existe para evitar rutas duplicadas
        String cleanRuta = rutaArchivo;
        if (cleanRuta.startsWith("uploads/") || cleanRuta.startsWith("uploads\\")) {
            cleanRuta = cleanRuta.substring(8);
        }

        // 1. Buscar en el índice completo por nombre exacto
        Path fromIndex = fileLocator.buscarArchivo(cleanRuta);
        if (fromIndex != null) return fromIndex;

        // 2. Ruta relativa al root de uploads
        Path relativa = rootUploads.resolve(cleanRuta).normalize();
        if (Files.exists(relativa)) return relativa;

        // 3. Solo el nombre del archivo en carpetas conocidas
        String nombreSolo = Paths.get(rutaArchivo).getFileName().toString();
        String[] carpetas = {
            "files/thu_talento/hdv_documentos",
            "files/thu_talento/hdv_fotos",
            "DATA/files/thu_talento/hdv_documentos",
            "DATA/files/thu_talento/hdv_fotos",
            "soportes",
            "fotos"
        };
        for (String dir : carpetas) {
            Path p = rootUploads.resolve(dir).resolve(nombreSolo).normalize();
            if (Files.exists(p)) return p;
        }

        return null;
    }
}