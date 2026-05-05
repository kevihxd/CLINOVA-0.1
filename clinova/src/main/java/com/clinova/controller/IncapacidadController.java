package com.clinova.controller;

import com.clinova.dto.IncapacidadDTO;
import com.clinova.service.IncapacidadService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/incapacidades")
@RequiredArgsConstructor
public class IncapacidadController {

    private final IncapacidadService incapacidadService;

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
            Path file = Paths.get(rutaArchivo);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("No se pudo leer el archivo");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar el archivo", e);
        }
    }
}
