package com.clinova.service;

import com.clinova.dto.AutenticacionRequestDTO;
import com.clinova.dto.AutenticacionResponseDTO;
import com.clinova.entity.Persona;
import com.clinova.entity.Usuario;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AutenticacionResponseDTO registro(AutenticacionRequestDTO request) {
        Persona persona = Persona.builder()
                .tipoDocumento(request.getPersona().getTipoDocumento())
                .numeroDocumento(request.getPersona().getNumeroDocumento())
                .primerNombre(request.getPersona().getPrimerNombre())
                .segundoNombre(request.getPersona().getSegundoNombre())
                .primerApellido(request.getPersona().getPrimerApellido())
                .segundoApellido(request.getPersona().getSegundoApellido())
                .fechaNacimiento(request.getPersona().getFechaNacimiento())
                .direccionResidencia(request.getPersona().getDireccionResidencia())
                .numeroTelefono(request.getPersona().getNumeroTelefono())
                .lugarNacimiento(request.getPersona().getLugarNacimiento())
                .correoElectronico(request.getPersona().getCorreoElectronico())
                .build();

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .persona(persona)
                .build();

        usuarioRepository.save(usuario);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", String.valueOf(usuario.getRol()));

        var jwtToken = jwtService.generateToken(extraClaims, usuario);
        return AutenticacionResponseDTO.builder()
                .token(jwtToken)
                .username(usuario.getUsername())
                .role(String.valueOf(usuario.getRol()))
                .build();
    }

    public AutenticacionResponseDTO login(AutenticacionRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        var usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", String.valueOf(usuario.getRol()));

        var jwtToken = jwtService.generateToken(extraClaims, usuario);
        return AutenticacionResponseDTO.builder()
                .token(jwtToken)
                .username(usuario.getUsername())
                .role(String.valueOf(usuario.getRol()))
                .build();
    }
}