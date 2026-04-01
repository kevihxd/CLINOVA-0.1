package com.clinova.service;

import com.clinova.dto.AutenticacionRequestDTO;
import com.clinova.dto.AutenticacionResponseDTO;
import com.clinova.entity.Role;
import com.clinova.entity.Usuario;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AutenticacionResponseDTO registrar(AutenticacionRequestDTO request) {

        Role rolAsignado = Role.USER;
        if (request.getRol() != null && !request.getRol().isEmpty()) {
            try {
                rolAsignado = Role.valueOf(request.getRol().toUpperCase());
            } catch (Exception e) {
                rolAsignado = Role.USER;
            }
        }

        var usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(rolAsignado)
                .persona(request.getPersona())
                .build();

        usuarioRepository.save(usuario);
        var jwtToken = jwtService.generarToken(usuario);

        return construirRespuesta(usuario, jwtToken, "Usuario registrado exitosamente");
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

        return construirRespuesta(usuario, jwtToken, "Autenticación exitosa");
    }

    // Método auxiliar para armar la respuesta con los nuevos campos
    private AutenticacionResponseDTO construirRespuesta(Usuario usuario, String token, String mensaje) {
        List<String> permisos = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String cargoNombre = (usuario.getCargo() != null) ? usuario.getCargo().getNombre() : "Sin Cargo Asignado";

        return AutenticacionResponseDTO.builder()
                .token(token)
                .mensaje(mensaje)
                .username(usuario.getUsername())
                .cargo(cargoNombre)
                .permisos(permisos)
                .build();
    }
}