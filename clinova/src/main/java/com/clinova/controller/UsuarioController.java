package com.clinova.controller;

import com.clinova.dto.PerfilUsuarioDTO;
import com.clinova.dto.UsuarioRequestDTO;
import com.clinova.entity.*;
import com.clinova.repository.PermisoRepository;
import com.clinova.repository.UsuarioRepository;
import com.clinova.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/v1/usuarios", "/api/usuarios"})
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PermisoRepository permisoRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> listarTodos() {
        try {
            return ResponseEntity.ok(usuarioService.listarUsuariosDTO());
        } catch (Exception e) {
            log.error("Error al listar usuarios: {}", e.getMessage(), e);
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @GetMapping("/reportes")
    public ResponseEntity<List<Map<String, Object>>> reporteUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerReporteUsuarios());
    }

    @GetMapping("/me")
    public ResponseEntity<PerfilUsuarioDTO> obtenerPerfil(@AuthenticationPrincipal Usuario principal) {
        Usuario u = usuarioRepository.findById(principal.getId()).orElse(principal);
        Persona p  = u.getPersona();
        HojaVida hv = u.getHojaVida();

        String primerNombre    = val(p != null ? p.getPrimerNombre()    : null, hv != null ? splitFirst(hv.getNombres())   : null);
        String segundoNombre   = val(p != null ? p.getSegundoNombre()   : null, hv != null ? splitSecond(hv.getNombres())  : null);
        String primerApellido  = val(p != null ? p.getPrimerApellido()  : null, hv != null ? splitFirst(hv.getApellidos()) : null);
        String segundoApellido = val(p != null ? p.getSegundoApellido() : null, hv != null ? splitSecond(hv.getApellidos()): null);
        String numeroDoc       = val(p != null ? p.getNumeroDocumento() : null, hv != null ? hv.getCedula() : u.getUsername());
        String telefono        = val(p != null ? p.getNumeroTelefono()  : null, hv != null ? hv.getTelefono() : null);
        String correo          = val(p != null ? p.getCorreoElectronico(): null, hv != null ? hv.getCorreoElectronico() : null);
        String direccion       = val(p != null ? p.getDireccionResidencia(): null, hv != null ? hv.getDireccionResidencia(): null);
        String fechaNac        = val(p != null ? p.getFechaNacimiento() : null, hv != null && hv.getFechaNacimiento() != null ? hv.getFechaNacimiento().toString() : null);

        String sedeNombre = null;
        String fechaIngreso = null;
        String fechaMod = null;
        String estado = null;

        if (hv != null) {
            try { sedeNombre = hv.getSedes() != null && !hv.getSedes().isEmpty() ? hv.getSedes().get(0).getNombre() : null; } catch (Exception ignored) {}
            try { fechaIngreso = hv.getFechaIngreso() != null ? hv.getFechaIngreso().toString() : null; } catch (Exception ignored) {}
            try { fechaMod = hv.getFechaUltimaEdicion() != null ? hv.getFechaUltimaEdicion().toLocalDate().toString() : null; } catch (Exception ignored) {}
            try { estado = hv.getEstado(); } catch (Exception ignored) {}
        }

        PerfilUsuarioDTO dto = PerfilUsuarioDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .rol(u.getRol() != null ? u.getRol().name() : null)
                .cargoNombre(u.getCargo() != null ? u.getCargo().getNombre() : null)
                .primerNombre(primerNombre)
                .segundoNombre(segundoNombre)
                .primerApellido(primerApellido)
                .segundoApellido(segundoApellido)
                .tipoDocumento(p != null ? p.getTipoDocumento() : null)
                .numeroDocumento(numeroDoc)
                .fechaNacimiento(fechaNac)
                .lugarNacimiento(p != null ? p.getLugarNacimiento() : null)
                .direccionResidencia(direccion)
                .numeroTelefono(telefono)
                .correoElectronico(correo)
                .sedeNombre(sedeNombre)
                .fechaIngreso(fechaIngreso)
                .fechaUltimaEdicion(fechaMod)
                .estado(estado)
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@AuthenticationPrincipal Usuario principal, @RequestBody Map<String, String> payload) {
        String nuevaPassword = payload.get("nuevaPassword");
        if (nuevaPassword == null || nuevaPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "La nueva contraseña es requerida"));
        }
        usuarioService.cambiarPasswordPrimerIngreso(principal.getId(), nuevaPassword);
        return ResponseEntity.ok(Map.of("message", "Contraseña cambiada exitosamente"));
    }

    private String val(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    private String splitFirst(String full) {
        if (full == null || full.isBlank()) return null;
        return full.trim().split("\\s+")[0];
    }

    private String splitSecond(String full) {
        if (full == null || full.isBlank()) return null;
        String[] parts = full.trim().split("\\s+");
        return parts.length > 1 ? parts[1] : null;
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> actualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody UsuarioRequestDTO dto) {
        Usuario u = usuarioService.actualizarUsuario(usuario.getId(), dto);
        return ResponseEntity.ok(usuarioService.mapearUsuarioADTO(u));
    }

    @GetMapping("/documento/{numeroDocumento}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        try {
            Map<String, Object> dto = usuarioService.obtenerUsuarioDTOPorDocumento(numeroDocumento);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.warn("Error al buscar usuario por documento: {}", numeroDocumento);
            return ResponseEntity.ok(null);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@RequestBody UsuarioRequestDTO dto) {
        log.info("Creando nuevo usuario: {}", dto.getUsername() != null ? dto.getUsername() : "N/A");
        Usuario u = usuarioService.crearUsuario(dto);
        return ResponseEntity.ok(usuarioService.mapearUsuarioADTO(u));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable Long id, @RequestBody UsuarioRequestDTO dto) {
        log.info("Actualizando usuario id={}", id);
        Usuario u = usuarioService.actualizarUsuario(id, dto);
        return ResponseEntity.ok(usuarioService.mapearUsuarioADTO(u));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        log.info("Eliminando usuario id={}", id);
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar usuario id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "No se pudo eliminar el usuario: " + e.getMessage()));
        }
    }

    private boolean puedeAdministrarPermisos(Usuario principal) {
        if (principal == null) return false;
        if (principal.getRol() == Role.ADMIN) return true;
        return principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "CARGOS_MODIFICAR".equals(a.getAuthority()) || "USUARIOS_MODIFICAR".equals(a.getAuthority()));
    }

    @GetMapping("/me/permisos")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> obtenerMisPermisos(@AuthenticationPrincipal Usuario principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario u = usuarioRepository.findByIdWithPermisos(principal.getId()).orElse(principal);
        List<String> efectivos = u.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        List<String> delCargo = (u.getCargo() != null && u.getCargo().getPermisos() != null)
                ? u.getCargo().getPermisos().stream().map(Permiso::getNombre).collect(Collectors.toList())
                : List.of();
        List<String> directos = (u.getPermisosDirectos() != null)
                ? u.getPermisosDirectos().stream().map(Permiso::getNombre).collect(Collectors.toList())
                : List.of();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("usuarioId", u.getId());
        body.put("username", u.getUsername());
        body.put("rol", u.getRol() != null ? u.getRol().name() : null);
        body.put("cargo", u.getCargo() != null ? u.getCargo().getNombre() : null);
        body.put("permisos", efectivos);
        body.put("permisosCargo", delCargo);
        body.put("permisosDirectos", directos);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/permisos")
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerPermisosUsuario(@PathVariable Long id, @AuthenticationPrincipal Usuario principal) {
        if (!puedeAdministrarPermisos(principal) && (principal == null || !principal.getId().equals(id))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No tiene permisos para consultar los permisos de este usuario"));
        }
        Usuario u = usuarioRepository.findByIdWithPermisos(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();

        List<Long> directosIds = (u.getPermisosDirectos() != null)
                ? u.getPermisosDirectos().stream().map(Permiso::getId).collect(Collectors.toList())
                : List.of();
        List<Long> cargoIds = (u.getCargo() != null && u.getCargo().getPermisos() != null)
                ? u.getCargo().getPermisos().stream().map(Permiso::getId).collect(Collectors.toList())
                : List.of();
        List<String> efectivos = u.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("usuarioId", u.getId());
        body.put("username", u.getUsername());
        body.put("nombreCompleto", u.getNombreCompleto());
        body.put("cargoNombre", u.getCargo() != null ? u.getCargo().getNombre() : "SIN CARGO");
        body.put("permisosDirectosIds", directosIds);
        body.put("permisosCargoIds", cargoIds);
        body.put("permisosEfectivos", efectivos);
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}/permisos")
    @Transactional
    public ResponseEntity<?> actualizarPermisosUsuario(
            @PathVariable Long id,
            @RequestBody List<Long> permisoIds,
            @AuthenticationPrincipal Usuario principal) {
        if (!puedeAdministrarPermisos(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "No tiene permisos para modificar permisos de usuarios"));
        }
        Usuario u = usuarioRepository.findByIdWithPermisos(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();

        List<Permiso> nuevos = (permisoIds != null && !permisoIds.isEmpty())
                ? permisoRepository.findAllById(permisoIds)
                : List.of();
        u.setPermisosDirectos(new HashSet<>(nuevos));
        usuarioRepository.save(u);
        log.info("Permisos directos actualizados para usuario id={} ({}) por {}: {} permisos asignados",
                u.getId(), u.getUsername(), principal != null ? principal.getUsername() : "SYSTEM", nuevos.size());
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Permisos directos del usuario actualizados correctamente",
                "totalDirectos", nuevos.size()
        ));
    }
}