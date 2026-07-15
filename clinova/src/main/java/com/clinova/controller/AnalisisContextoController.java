package com.clinova.controller;

import com.clinova.entity.AnalisisContexto;
import com.clinova.service.AnalisisContextoService;
import com.clinova.service.FileLocatorService;
import com.clinova.dto.StructureResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/contexto/analisis")
@RequiredArgsConstructor
public class AnalisisContextoController {
    private final AnalisisContextoService service;
    private final FileLocatorService fileLocator;

    @GetMapping
    public ResponseEntity<StructureResponses<List<AnalisisContexto>>> getAll() {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Lista de análisis de contexto obtenida exitosamente", service.findAll()));
    }

    @PostMapping
    public ResponseEntity<StructureResponses<AnalisisContexto>> create(@RequestBody AnalisisContexto entity) {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Análisis de contexto creado exitosamente", service.save(entity)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StructureResponses<AnalisisContexto>> update(@PathVariable Long id, @RequestBody AnalisisContexto entity) {
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Análisis de contexto actualizado exitosamente", service.update(id, entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Análisis de contexto eliminado exitosamente", null));
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargar(@PathVariable Long id) {
        try {
            AnalisisContexto analisis = service.findById(id);
            if (analisis == null || analisis.getRutaArchivo() == null) {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "No hay archivo asociado", null));
            }

            Path archivoPath = fileLocator.buscarArchivo(analisis.getRutaArchivo());
            if (archivoPath != null && Files.exists(archivoPath)) {
                Resource resource = new UrlResource(archivoPath.toUri());
                String contentType = Files.probeContentType(archivoPath);
                if (contentType == null) contentType = "application/octet-stream";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            }
            return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El archivo no se encuentra en el servidor", null));
        } catch (Exception e) {
            log.error("Error al descargar archivo de analisis de contexto: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new StructureResponses<>("ERROR", "Error interno", null));
        }
    }
}
