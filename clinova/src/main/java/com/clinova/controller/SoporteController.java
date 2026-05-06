package com.clinova.controller;

import com.clinova.dto.RechazoSoporteRequestDTO;
import com.clinova.dto.SoporteRequestDTO;
import com.clinova.dto.SoporteResponseDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.entity.Soporte;
import com.clinova.repository.SoporteRepository;
import com.clinova.service.SoporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/soportes")
@RequiredArgsConstructor
public class SoporteController {

    private final SoporteService soporteService;
    private final SoporteRepository soporteRepository;

    @GetMapping("/{id}/ver")
    public ResponseEntity<?> verArchivo(@PathVariable Long id) {
        try {
            Soporte soporte = soporteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Soporte no encontrado"));

            Path rutaArchivo = Paths.get(soporte.getRutaArchivo()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(rutaArchivo.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(404)
                        .body(new StructureResponses<>("ERROR", "Archivo no encontrado en el servidor", null));
            }

            String contentType = "application/pdf";
            String extension = soporte.getNombreArchivo().toLowerCase();
            if (extension.endsWith(".png") || extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
                contentType = "image/" + (extension.endsWith(".png") ? "png" : "jpeg");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + soporte.getNombreArchivo() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/hoja-vida/{hojaVidaId}")
    public StructureResponses<List<SoporteResponseDTO>> obtenerSoportesPorHojaVida(@PathVariable Long hojaVidaId) {
        List<SoporteResponseDTO> response = soporteService.obtenerSoportesPorHojaVidaId(hojaVidaId);
        return new StructureResponses<>("SUCCESS", "Soportes obtenidos exitosamente", response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StructureResponses<SoporteResponseDTO> crearSoporte(
            @RequestPart("datos") SoporteRequestDTO request,
            @RequestPart("archivo") MultipartFile archivo) {
        SoporteResponseDTO response = soporteService.crearSoporte(request, archivo);
        return new StructureResponses<>("SUCCESS", "Documento subido exitosamente", response);
    }

    @DeleteMapping("/{id}")
    public StructureResponses<Void> eliminarSoporte(@PathVariable Long id) {
        soporteService.eliminarSoporte(id);
        return new StructureResponses<>("SUCCESS", "Documento eliminado exitosamente", null);
    }

    @PutMapping("/{id}/tipo")
    public StructureResponses<SoporteResponseDTO> actualizarTipoDocumento(
            @PathVariable Long id,
            @RequestParam String tipoDocumento) {
        SoporteResponseDTO response = soporteService.actualizarTipoDocumento(id, tipoDocumento);
        return new StructureResponses<>("SUCCESS", "Nombre del documento actualizado", response);
    }

    @PutMapping("/{id}/rechazar")
    public StructureResponses<SoporteResponseDTO> rechazarSoporte(
            @PathVariable Long id,
            @RequestBody RechazoSoporteRequestDTO request) {
        SoporteResponseDTO response = soporteService.rechazarSoporte(id, request);
        return new StructureResponses<>("SUCCESS", "Documento rechazado y notificación enviada", response);
    }
}