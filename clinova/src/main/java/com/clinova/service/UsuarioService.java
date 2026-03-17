package com.clinova.service;

import com.clinova.dto.UsuarioRequestDTO;
import com.clinova.dto.UsuarioResponseDTO;
import com.clinova.entity.Persona;
import com.clinova.entity.TipoDocumento;
import com.clinova.entity.Usuario;
import com.clinova.repository.TipoDocumentoRepository;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    @Transactional
    public UsuarioResponseDTO createUsuario(UsuarioRequestDTO request) {
        TipoDocumento tipoDoc = tipoDocumentoRepository.findById(request.tipoDocumentoId())
                .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));

        Persona persona = Persona.builder()
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .numeroDocumento(request.numeroDocumento())
                .tipoDocumento(tipoDoc)
                .build();

        Usuario usuario = Usuario.builder()
                .username(request.username())
                .password(request.password())
                .persona(persona)
                .build();

        Usuario savedUsuario = usuarioRepository.save(usuario);
        return mapToResponseDTO(savedUsuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO getUsuarioById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return mapToResponseDTO(usuario);
    }

    private UsuarioResponseDTO mapToResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPersona().getNombres(),
                usuario.getPersona().getApellidos(),
                usuario.getPersona().getNumeroDocumento()
        );
    }
}