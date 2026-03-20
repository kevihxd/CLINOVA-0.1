package com.clinova.service;

import com.clinova.dto.AutenticacionRequestDTO;
import com.clinova.dto.AutenticacionResponseDTO;
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

        // 1. Verificamos qué rol nos mandaron. Si no mandan nada, será USER por defecto.
        Role rolAsignado = Role.USER;
        if (request.getRol() != null && !request.getRol().isEmpty()) {
            try {
                rolAsignado = Role.valueOf(request.getRol().toUpperCase());
            } catch (Exception e) {
                rolAsignado = Role.USER;
            }
        }

        // 2. Creamos el usuario leyendo todos los datos
        var usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(rolAsignado) // 🔥 Ahora sí usa el rol que mandes ("ADMIN")
                .persona(request.getPersona()) // 🔥 Y guardamos la persona de una vez
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