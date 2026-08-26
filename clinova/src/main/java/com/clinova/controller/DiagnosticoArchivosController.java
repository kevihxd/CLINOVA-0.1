package com.clinova.controller;

import com.clinova.entity.HojaVida;
import com.clinova.entity.Soporte;
import com.clinova.entity.Usuario;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SoporteRepository;
import com.clinova.service.FileLocatorService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/diagnostico-soportes")
@RequiredArgsConstructor
public class DiagnosticoArchivosController {

    private final SoporteRepository soporteRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final FileLocatorService fileLocatorService;

    private static final String UPLOADS_DIR = System.getenv().getOrDefault("UPLOADS_ROOT_PATH", "uploads");

    @Data
    @Builder
    public static class DiagnosticoSoporteDTO {
        private Long id;
        private String tipoDiagnostico;
        private Long hojaVidaId;
        private String cedula;
        private String nombrePersona;
        private String tipoDocumento;
        private String nombreArchivo;
        private String rutaArchivo;
        private boolean existeFisico;
        private Integer httpStatus;
        private String errorConsola;
        private LocalDateTime fechaCarga;
    }

    @Data
    public static class FixRutaRequestDTO {
        private String nuevaRuta;
    }

    private boolean esAdmin(Usuario usuario) {
        if (usuario == null) return false;
        if (usuario.getUsername() != null && usuario.getUsername().toLowerCase().contains("admin")) return true;
        if (usuario.getRol() != null && "ADMIN".equalsIgnoreCase(usuario.getRol().name())) return true;
        return usuario.getAuthorities() != null && usuario.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> diagnosticarSoportes(@AuthenticationPrincipal Usuario usuario) {
        if (!esAdmin(usuario)) {
            log.warn("Intento no autorizado de acceder a diagnostico de soportes por usuario: {}", usuario != null ? usuario.getUsername() : "ANONYMOUS");
            return ResponseEntity.status(403).build();
        }

        try {
            List<Soporte> todosSoportes = soporteRepository.findAll();
            List<DiagnosticoSoporteDTO> diagnosticos = new ArrayList<>();

            for (Soporte sop : todosSoportes) {
                if (sop == null) continue;

                String ruta = sop.getRutaArchivo();
                boolean existe = false;

                if (ruta != null && !ruta.trim().isEmpty() && !"SIN_ARCHIVO".equalsIgnoreCase(ruta.trim())) {
                    try {
                        Path pService = fileLocatorService.buscarArchivo(ruta);
                        if (pService != null && Files.exists(pService) && Files.isRegularFile(pService)) {
                            existe = true;
                        } else {
                            String cleanedRuta = ruta.trim().replace("\\", "/");
                            if (cleanedRuta.startsWith("/")) cleanedRuta = cleanedRuta.substring(1);
                            Path pDirect = Paths.get(UPLOADS_DIR).resolve(cleanedRuta).normalize();
                            if (Files.exists(pDirect) && Files.isRegularFile(pDirect)) {
                                existe = true;
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Error al verificar ruta '{}' de soporte id={}: {}", ruta, sop.getId(), e.getMessage());
                    }
                }

                String cedula = "S/N";
                String nombrePersona = "Sin Colaborador Asignado";
                Long hvId = null;

                try {
                    HojaVida hv = sop.getHojaVida();
                    if (hv != null) {
                        hvId = hv.getId();
                        if (hv.getCedula() != null && !hv.getCedula().isBlank()) {
                            cedula = hv.getCedula().trim();
                        }
                        String nom = hv.getNombres() != null ? hv.getNombres().trim() : "";
                        String ape = hv.getApellidos() != null ? hv.getApellidos().trim() : "";
                        String full = (nom + " " + ape).trim();
                        if (!full.isEmpty()) nombrePersona = full;
                    }
                } catch (Exception e) {
                    log.debug("No se pudo obtener HojaVida para soporte id={}: {}", sop.getId(), e.getMessage());
                }

                String errorConsolaStr = null;
                Integer status = 200;

                if (!existe) {
                    status = 404;
                    errorConsolaStr = "GET https://apiclinovavps.clinicalhouse.co/api/v1/soportes/descargar/" + sop.getId() + " net::ERR_HTTP_RESPONSE_CODE_FAILURE 404 (Not Found)";
                }

                diagnosticos.add(DiagnosticoSoporteDTO.builder()
                        .id(sop.getId())
                        .tipoDiagnostico("SOPORTE_HOJA_VIDA")
                        .hojaVidaId(hvId)
                        .cedula(cedula)
                        .nombrePersona(nombrePersona)
                        .tipoDocumento(sop.getTipoDocumento() != null ? sop.getTipoDocumento() : "DESCONOCIDO")
                        .nombreArchivo(sop.getNombreArchivo() != null ? sop.getNombreArchivo() : "documento.pdf")
                        .rutaArchivo(sop.getRutaArchivo())
                        .existeFisico(existe)
                        .httpStatus(status)
                        .errorConsola(errorConsolaStr)
                        .fechaCarga(sop.getFechaCarga())
                        .build());
            }

            return ResponseEntity.ok(diagnosticos);
        } catch (Exception e) {
            log.error("Error catastrófico en diagnosticarSoportes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error al generar diagnóstico: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/fix-ruta")
    @Transactional
    public ResponseEntity<?> corregirRuta(
            @PathVariable Long id,
            @RequestBody FixRutaRequestDTO request,
            @AuthenticationPrincipal Usuario usuario
    ) {
        if (!esAdmin(usuario)) {
            return ResponseEntity.status(403).build();
        }

        Soporte sop = soporteRepository.findById(id).orElse(null);
        if (sop == null) return ResponseEntity.notFound().build();

        if (request.getNuevaRuta() != null && !request.getNuevaRuta().trim().isEmpty()) {
            sop.setRutaArchivo(request.getNuevaRuta().trim());
            soporteRepository.save(sop);
            log.info("Admin usuario '{}' corrigio ruta de soporte id={}: {}", usuario.getUsername(), id, request.getNuevaRuta());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reemplazar-archivo")
    @Transactional
    public ResponseEntity<?> reemplazarArchivo(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal Usuario usuario
    ) {
        if (!esAdmin(usuario)) {
            return ResponseEntity.status(403).build();
        }

        Soporte sop = soporteRepository.findById(id).orElse(null);
        if (sop == null) return ResponseEntity.notFound().build();

        try {
            String origName = archivo.getOriginalFilename();
            String cleanName = (origName != null ? origName : "soporte_" + id + ".pdf").replaceAll("[^a-zA-Z0-9._-]", "_");
            String nombreFinal = UUID.randomUUID().toString().substring(0, 8) + "_" + cleanName;

            Path dirDestino = Paths.get(UPLOADS_DIR, "soportes").toAbsolutePath().normalize();
            Files.createDirectories(dirDestino);

            Path targetPath = dirDestino.resolve(nombreFinal);
            Files.copy(archivo.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            sop.setNombreArchivo(origName);
            sop.setRutaArchivo("soportes/" + nombreFinal);
            sop.setTamano(archivo.getSize());
            sop.setFechaCarga(LocalDateTime.now());

            soporteRepository.save(sop);
            log.info("Admin usuario '{}' reemplazo archivo fisico de soporte id={}: {}", usuario.getUsername(), id, nombreFinal);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al reemplazar archivo de soporte id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
