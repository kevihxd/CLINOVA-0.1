package com.clinova.service;

import com.clinova.dto.UsuarioRequestDTO;
import com.clinova.entity.Cargo;
import com.clinova.entity.Persona;
import com.clinova.entity.Role;
import com.clinova.entity.Usuario;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.PersonaRepository;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final CargoRepository cargoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional
    public Usuario crearUsuario(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("El nombre de usuario ya existe");
        }

        Cargo cargo = null;
        if (dto.getCargoId() != null) {
            cargo = cargoRepository.findById(dto.getCargoId())
                    .orElseThrow(() -> new RuntimeException("Cargo no encontrado"));
        }

        // Creamos y guardamos la persona primero (asegúrate de que en DTO mapeen a primerNombre, etc.)
        Persona persona = Persona.builder()
                .primerNombre(dto.getNombres())
                .primerApellido(dto.getApellidos())
                .correoElectronico(dto.getCorreo())
                .build();
        personaRepository.save(persona);

        // Creamos el usuario
        Usuario usuario = Usuario.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .rol(Role.valueOf(dto.getRol().toUpperCase()))
                .cargo(cargo)
                .persona(persona)
                .build();

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarUsuario(Long id, Usuario datosActualizados) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioExistente.setUsername(datosActualizados.getUsername());
        usuarioExistente.setRol(datosActualizados.getRol());

        // Encriptar contraseña solo si se envía una nueva
        if (datosActualizados.getPassword() != null && !datosActualizados.getPassword().isEmpty()) {
            usuarioExistente.setPassword(passwordEncoder.encode(datosActualizados.getPassword()));
        }

        // Actualización limpia de la Persona asociada
        if (datosActualizados.getPersona() != null) {
            if (usuarioExistente.getPersona() == null) {
                usuarioExistente.setPersona(datosActualizados.getPersona());
            } else {
                Persona pExistente = usuarioExistente.getPersona();
                Persona pNueva = datosActualizados.getPersona();

                pExistente.setTipoDocumento(pNueva.getTipoDocumento());
                pExistente.setNumeroDocumento(pNueva.getNumeroDocumento());
                pExistente.setPrimerNombre(pNueva.getPrimerNombre());
                pExistente.setSegundoNombre(pNueva.getSegundoNombre());
                pExistente.setPrimerApellido(pNueva.getPrimerApellido());
                pExistente.setSegundoApellido(pNueva.getSegundoApellido());
                pExistente.setFechaNacimiento(pNueva.getFechaNacimiento());
                pExistente.setDireccionResidencia(pNueva.getDireccionResidencia());
                pExistente.setNumeroTelefono(pNueva.getNumeroTelefono());
                pExistente.setLugarNacimiento(pNueva.getLugarNacimiento());
                pExistente.setCorreoElectronico(pNueva.getCorreoElectronico());
            }
        }

        return usuarioRepository.save(usuarioExistente);
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }
}