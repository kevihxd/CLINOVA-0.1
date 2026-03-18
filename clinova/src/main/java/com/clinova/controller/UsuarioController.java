package com.clinova.controller;

import com.clinova.entity.Persona;
import com.clinova.entity.Usuario;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario datosActualizados) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioExistente.setUsername(datosActualizados.getUsername());
        usuarioExistente.setRol(datosActualizados.getRol());

        if (datosActualizados.getPassword() != null && !datosActualizados.getPassword().isEmpty()) {
            usuarioExistente.setPassword(datosActualizados.getPassword());
        }

        if (datosActualizados.getPersona() != null) {
            if (usuarioExistente.getPersona() == null) {
                usuarioExistente.setPersona(datosActualizados.getPersona());
            } else {
                Persona pExistente = usuarioExistente.getPersona();
                Persona pNueva = datosActualizados.getPersona();

                pExistente.setTipo_documento(pNueva.getTipo_documento());
                pExistente.setNumero_documento(pNueva.getNumero_documento());
                pExistente.setPrimer_nombre(pNueva.getPrimer_nombre());
                pExistente.setSegundo_nombre(pNueva.getSegundo_nombre());
                pExistente.setPrimer_apellido(pNueva.getPrimer_apellido());
                pExistente.setSegundo_apellido(pNueva.getSegundo_apellido());
                pExistente.setFecha_nacimiento(pNueva.getFecha_nacimiento());
                pExistente.setDireccion_residencia(pNueva.getDireccion_residencia());
                pExistente.setNumero_telefono(pNueva.getNumero_telefono());
                pExistente.setLugar_nacimiento(pNueva.getLugar_nacimiento());
                pExistente.setCorreo_electronico(pNueva.getCorreo_electronico());
            }
        }

        return ResponseEntity.ok(usuarioRepository.save(usuarioExistente));
    }
}