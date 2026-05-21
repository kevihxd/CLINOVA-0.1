package com.clinova.controller;

import com.clinova.dto.DocumentoHistorialDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.entity.Documento;
import com.clinova.entity.Usuario;
import com.clinova.repository.DocumentoRepository;
import com.clinova.service.DocumentoHistorialService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoRepository repository;
    private final DocumentoHistorialService historialService;
    private final Path root = Paths.get("uploads").toAbsolutePath().normalize();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo crear la carpeta de uploads");
        }
    }

    @GetMapping
    public ResponseEntity<StructureResponses<List<Documento>>> obtenerTodos() {
        try {
            List<Documento> lista = repository.findAll();
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Listado obtenido", lista));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
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
            @AuthenticationPrincipal Usuario usuario) {
        try {
            if (archivo != null && !archivo.isEmpty()) {
                String nombreArchivo = UUID.randomUUID() + "_" + archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                Files.copy(archivo.getInputStream(), this.root.resolve(nombreArchivo));
                documento.setUbicacion(nombreArchivo);
            } else {
                if (documento.getUbicacion() == null || documento.getUbicacion().isEmpty()) {
                    documento.setUbicacion("SIN_ARCHIVO");
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

            documento.setEstado("EN REVISIÓN");
            Documento guardado = repository.save(documento);
            historialService.registrarHistorial(
                    guardado.getId(), "CREACION",
                    "Documento '" + guardado.getNombre() + "' enviado a revisión",
                    usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Documento enviado a revisión", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<StructureResponses<Documento>> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();
            doc.setEstado("VIGENTE");
            String fechaHoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            doc.setFechaAprobacion(fechaHoy);
            Documento guardado = repository.save(doc);
            historialService.registrarHistorial(
                    id, "APROBACION",
                    "Documento aprobado y publicado como VIGENTE",
                    usuario);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Aprobado", guardado));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<?> descargar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            Documento doc = repository.findById(id).orElseThrow();

            if (doc.getUbicacion() == null || doc.getUbicacion().equals("SIN_ARCHIVO")) {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El documento no tiene archivo físico", null));
            }

            Path file = root.resolve(doc.getUbicacion()).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                historialService.registrarHistorial(
                        id, "DESCARGA",
                        "Archivo descargado o visualizado",
                        usuario);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNombre() + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource);
            } else {
                return ResponseEntity.status(404).body(new StructureResponses<>("ERROR", "El archivo no se encuentra en el servidor", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            historialService.registrarHistorial(id, "ELIMINACION", "Documento eliminado del sistema", usuario);
            repository.deleteById(id);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Eliminado", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }
}