package com.clinova.controller;

import com.clinova.dto.IncapacidadDTO;
import com.clinova.service.IncapacidadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/incapacidades")
@RequiredArgsConstructor
public class IncapacidadController {

    private final IncapacidadService incapacidadService;

    private static final Path UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath().normalize();

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncapacidadDTO> crearOActualizar(
            @RequestParam("usuarioId") Long usuarioId,
            @RequestPart("data") IncapacidadDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        return ResponseEntity.ok(incapacidadService.crearOActualizar(usuarioId, dto, archivo));
    }

    @GetMapping("/documento/{numeroDocumento}")
    public ResponseEntity<List<IncapacidadDTO>> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(incapacidadService.obtenerPorNumeroDocumento(numeroDocumento));
    }

    @GetMapping
    public ResponseEntity<List<IncapacidadDTO>> obtenerTodas() {
        return ResponseEntity.ok(incapacidadService.obtenerTodas());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        incapacidadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/descargar-archivo")
    public ResponseEntity<Resource> descargarArchivo(@RequestParam("ruta") String rutaArchivo) {
        try {
            // Seguridad: normalizar y verificar que la ruta esté dentro del directorio permitido
            Path filePath = UPLOAD_ROOT.resolve(rutaArchivo).normalize();

            if (!filePath.startsWith(UPLOAD_ROOT)) {
                log.warn("Intento de path traversal detectado: {}", rutaArchivo);
                throw new RuntimeException("Ruta de archivo no permitida");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("No se pudo leer el archivo");
            }
        } catch (Exception e) {
            log.error("Error al descargar incapacidad archivo={}: {}", rutaArchivo, e.getMessage());
            throw new RuntimeException("Error al descargar el archivo");
        }
    }
}
