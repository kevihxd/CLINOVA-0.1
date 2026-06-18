package com.clinova.controller;

import com.clinova.dto.PerfilUsuarioDTO;
import com.clinova.dto.UsuarioRequestDTO;
import com.clinova.entity.*;
import com.clinova.repository.UsuarioRepository;
import com.clinova.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<Usuario>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
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
    public ResponseEntity<Usuario> actualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(usuario.getId(), dto));
    }

    @GetMapping("/documento/{numeroDocumento}")
    public ResponseEntity<Usuario> obtenerPorDocumento(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(usuarioService.obtenerPorDocumento(numeroDocumento));
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody UsuarioRequestDTO dto) {
        log.info("Creando nuevo usuario: {}", dto.getUsername() != null ? dto.getUsername() : "N/A");
        return ResponseEntity.ok(usuarioService.crearUsuario(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizar(@PathVariable Long id, @RequestBody UsuarioRequestDTO dto) {
        log.info("Actualizando usuario id={}", id);
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("Eliminando usuario id={}", id);
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}