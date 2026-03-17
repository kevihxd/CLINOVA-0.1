package com.clinova.service;

import com.clinova.dto.AutenticacionRequestDTO;
import com.clinova.dto.AutenticacionResponseDTO;
import com.clinova.entity.Persona;
import com.clinova.entity.Role;
import com.clinova.entity.Usuario;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AutenticacionResponseDTO registrar(AutenticacionRequestDTO request) {

        var persona = Persona.builder()
                .nombre("Pendiente")
                .apellido("Pendiente")
                .build();

        var usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Role.USER)
                .persona(persona)
                .build();

        usuarioRepository.save(usuario);

        var jwtToken = jwtService.generarToken(usuario);

        return AutenticacionResponseDTO.builder()
                .token(jwtToken)
                .build();
    }

    public AutenticacionResponseDTO autenticar(AutenticacionRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow();

        var jwtToken = jwtService.generarToken(usuario);

        return AutenticacionResponseDTO.builder()
                .token(jwtToken)
                .build();
    }
}