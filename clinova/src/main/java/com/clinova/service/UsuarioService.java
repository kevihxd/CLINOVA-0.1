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

        Persona persona = Persona.builder()
                .tipoDocumento(dto.getTipoDocumento())
                .numeroDocumento(dto.getNumeroDocumento())
                .primerNombre(dto.getPrimerNombre())
                .segundoNombre(dto.getSegundoNombre())
                .primerApellido(dto.getPrimerApellido())
                .segundoApellido(dto.getSegundoApellido())
                .fechaNacimiento(dto.getFechaNacimiento())
                .direccionResidencia(dto.getDireccionResidencia())
                .numeroTelefono(dto.getNumeroTelefono())
                .lugarNacimiento(dto.getLugarNacimiento())
                .correoElectronico(dto.getCorreoElectronico())
                .perfilVacunacion(dto.getPerfilVacunacion())
                .build();

        personaRepository.save(persona);

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
    public Usuario actualizarUsuario(Long id, UsuarioRequestDTO dto) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioExistente.setUsername(dto.getUsername());
        usuarioExistente.setRol(Role.valueOf(dto.getRol().toUpperCase()));

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            usuarioExistente.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getCargoId() != null) {
            Cargo cargo = cargoRepository.findById(dto.getCargoId())
                    .orElseThrow(() -> new RuntimeException("Cargo no encontrado"));
            usuarioExistente.setCargo(cargo);
        } else {
            usuarioExistente.setCargo(null);
        }

        Persona pExistente = usuarioExistente.getPersona();
        if (pExistente == null) {
            pExistente = new Persona();
            usuarioExistente.setPersona(pExistente);
        }

        pExistente.setTipoDocumento(dto.getTipoDocumento());
        pExistente.setNumeroDocumento(dto.getNumeroDocumento());
        pExistente.setPrimerNombre(dto.getPrimerNombre());
        pExistente.setSegundoNombre(dto.getSegundoNombre());
        pExistente.setPrimerApellido(dto.getPrimerApellido());
        pExistente.setSegundoApellido(dto.getSegundoApellido());
        pExistente.setFechaNacimiento(dto.getFechaNacimiento());
        pExistente.setDireccionResidencia(dto.getDireccionResidencia());
        pExistente.setNumeroTelefono(dto.getNumeroTelefono());
        pExistente.setLugarNacimiento(dto.getLugarNacimiento());
        pExistente.setCorreoElectronico(dto.getCorreoElectronico());
        pExistente.setPerfilVacunacion(dto.getPerfilVacunacion());

        personaRepository.save(pExistente);
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