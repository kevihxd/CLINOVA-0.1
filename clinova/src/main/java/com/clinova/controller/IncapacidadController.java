package com.clinova.controller;

import com.clinova.dto.StructureResponses;
import com.clinova.entity.Incapacidad;
import com.clinova.entity.Usuario;
import com.clinova.repository.IncapacidadRepository;
import com.clinova.service.FileLocatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/v1/incapacidades")
@RequiredArgsConstructor
public class IncapacidadController {

    private final IncapacidadRepository incapacidadRepository;
    private final FileLocatorService fileLocator;
    private final Path rootUploads = Paths.get("uploads").toAbsolutePath().normalize();

    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargarAdjunto(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        try {
            Incapacidad incapacidad = incapacidadRepository.findById(id).orElseThrow();

            if (incapacidad.getRutaArchivo() == null || incapacidad.getRutaArchivo().isEmpty()) {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El registro no tiene documento adjunto", null));
            }

            Path file = buscarArchivoFisico(incapacidad.getRutaArchivo(), "ausentismos");

            if (file != null && Files.exists(file) && Files.isReadable(file)) {
                Resource resource = new UrlResource(file.toUri());
                String contentType = "application/octet-stream";
                String filename = incapacidad.getRutaArchivo().toLowerCase();

                if (filename.endsWith(".pdf")) contentType = MediaType.APPLICATION_PDF_VALUE;
                else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = MediaType.IMAGE_JPEG_VALUE;
                else if (filename.endsWith(".png")) contentType = MediaType.IMAGE_PNG_VALUE;

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + incapacidad.getNombreArchivo() + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "Archivo no encontrado en el disco del servidor", null));
            }
        } catch (Exception e) {
            log.error("Error al descargar incapacidad [id={}]: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    private Path buscarArchivoFisico(String nombreArchivo, String subcarpeta) {
        Path fromService = fileLocator.buscarArchivo(nombreArchivo);
        if (fromService != null) return fromService;

        Path path = rootUploads.resolve(subcarpeta).resolve(nombreArchivo).normalize();
        if (Files.exists(path)) return path;

        path = rootUploads.resolve(nombreArchivo).normalize();
        if (Files.exists(path)) return path;

        String[] carpetasExtendidas = {"soportes", "soportes/otros_soportes", "soportes/sin_clasificar", "documentos"};
        for (String dir : carpetasExtendidas) {
            path = rootUploads.resolve(dir).resolve(nombreArchivo).normalize();
            if (Files.exists(path)) return path;
        }

        return null;
    }
}