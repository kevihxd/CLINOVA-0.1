package com.clinova.controller;

import com.clinova.entity.ApoyoTerapeuticoAdjunto;
import com.clinova.entity.Role;
import com.clinova.entity.Usuario;
import com.clinova.service.ApoyoTerapeuticoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/v1/apoyo-terapeutico", "/api/apoyo-terapeutico"})
@RequiredArgsConstructor
public class ApoyoTerapeuticoController {

    private final ApoyoTerapeuticoService service;

    private boolean tienePermiso(Usuario usuario, String... permisosRequeridos) {
        if (usuario == null) return false;
        if (usuario.getRol() == Role.ADMIN) return true;
        if (usuario.getAuthorities() == null) return false;
        for (var auth : usuario.getAuthorities()) {
            String a = auth.getAuthority();
            if ("ROLE_ADMIN".equals(a)) return true;
            for (String req : permisosRequeridos) {
                if (req.equalsIgnoreCase(a)) return true;
            }
        }
        return false;
    }

    private ResponseEntity<Map<String, Object>> forbidden(String accion) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "FORBIDDEN",
                "message", "No tiene permisos habilitados para " + accion + " en el módulo de Apoyo Terapéutico."
        ));
    }

    @GetMapping("/ingresos")
    public ResponseEntity<?> listarIngresos(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "alerta", required = false) String alerta,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_VER", "APOYO_TERAPEUTICO_CREAR", "APOYO_TERAPEUTICO_MODIFICAR")) {
            return forbidden("consultar ingresos");
        }
        try {
            return ResponseEntity.ok(service.listarIngresos(q, estado, alerta));
        } catch (Exception e) {
            log.error("Error listando ingresos de Apoyo Terapéutico: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/ingresos/{id}")
    public ResponseEntity<?> obtenerDetalle(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_VER", "APOYO_TERAPEUTICO_CREAR", "APOYO_TERAPEUTICO_MODIFICAR")) {
            return forbidden("ver el detalle del ingreso");
        }
        try {
            return ResponseEntity.ok(service.obtenerDetalle(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/ingresos")
    public ResponseEntity<?> crearIngreso(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_CREAR")) {
            return forbidden("registrar ingresos");
        }
        try {
            return ResponseEntity.ok(service.crearIngreso(payload, usuario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando ingreso: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/ingresos/{id}")
    public ResponseEntity<?> actualizarIngreso(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_MODIFICAR")) {
            return forbidden("modificar ingresos");
        }
        try {
            return ResponseEntity.ok(service.actualizarIngreso(id, payload, usuario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/ingresos/{id}")
    public ResponseEntity<?> eliminarIngreso(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_ELIMINAR")) {
            return forbidden("eliminar ingresos");
        }
        try {
            service.eliminarIngreso(id);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Ingreso eliminado correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/ingresos/importar")
    public ResponseEntity<?> importarIngresos(
            @RequestBody List<Map<String, Object>> filas,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_CREAR")) {
            return forbidden("importar ingresos desde Kubapp/Excel");
        }
        try {
            return ResponseEntity.ok(service.importarIngresos(filas, usuario));
        } catch (Exception e) {
            log.error("Error importando ingresos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping(value = "/ingresos/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> subirAdjunto(
            @PathVariable Long id,
            @RequestParam("tipoAdjunto") String tipoAdjunto,
            @RequestParam(value = "fechaDocumento", required = false) String fechaDocumento,
            @RequestParam(value = "observacion", required = false) String observacion,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_CREAR", "APOYO_TERAPEUTICO_MODIFICAR")) {
            return forbidden("cargar adjuntos");
        }
        try {
            return ResponseEntity.ok(service.subirAdjunto(id, tipoAdjunto, fechaDocumento, observacion, archivo, usuario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error subiendo adjunto en ingreso {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/adjuntos/{adjuntoId}/archivo")
    public ResponseEntity<?> descargarOVerAdjunto(
            @PathVariable Long adjuntoId,
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_VER", "APOYO_TERAPEUTICO_CREAR", "APOYO_TERAPEUTICO_MODIFICAR")) {
            return forbidden("visualizar o descargar adjuntos");
        }
        try {
            ApoyoTerapeuticoAdjunto adj = service.obtenerAdjuntoEntidad(adjuntoId);
            Path filePath = service.resolverRutaArchivo(adj);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Archivo físico no encontrado en el servidor."));
            }
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = adj.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = Files.probeContentType(filePath);
            }
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            String disposition = (download ? "attachment" : "inline")
                    + "; filename=\"" + (adj.getNombreArchivoOriginal() != null ? adj.getNombreArchivoOriginal().replace("\"", "") : "archivo") + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/adjuntos/{adjuntoId}")
    public ResponseEntity<?> eliminarAdjunto(
            @PathVariable Long adjuntoId,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_ELIMINAR")) {
            return forbidden("eliminar adjuntos");
        }
        try {
            return ResponseEntity.ok(service.eliminarAdjunto(adjuntoId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/ingresos/{id}/descargar-zip")
    public ResponseEntity<?> descargarZip(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        if (!tienePermiso(usuario, "APOYO_TERAPEUTICO_VER", "APOYO_TERAPEUTICO_CREAR", "APOYO_TERAPEUTICO_MODIFICAR")) {
            return forbidden("descargar carpeta de adjuntos");
        }
        try {
            byte[] zipBytes = service.generarZipAdjuntos(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ingreso_" + id + "_adjuntos.zip\"")
                    .body(zipBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }
}
