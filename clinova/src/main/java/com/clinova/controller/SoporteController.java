package com.clinova.controller;

import com.clinova.dto.StructureResponses;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Soporte;
import com.clinova.entity.Usuario;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SoporteRepository;
import com.clinova.service.FileLocatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/soportes")
@RequiredArgsConstructor
public class SoporteController {

    private final SoporteRepository soporteRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final FileLocatorService fileLocator;
    private final ObjectMapper objectMapper;

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

    /** Elimina un soporte por ID (y su archivo físico del disco si existe) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        try {
            Soporte soporte = soporteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Soporte no encontrado con ID: " + id));

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
            log.info("Soporte eliminado [id={}, usuario={}]", id, usuario != null ? usuario.getUsername() : "desconocido");
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Documento eliminado correctamente"));
        } catch (Exception e) {
            log.error("Error al eliminar soporte [id={}]: {}", id, e.getMessage());
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