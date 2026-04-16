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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .rol(request.getRol() != null ? Role.valueOf(request.getRol().toUpperCase()) : Role.USER)
                .persona(persona)
                .build();

        usuarioRepository.save(usuario);

        List<String> permisos = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("permisos", permisos);
        extraClaims.put("role", usuario.getRol() != null ? usuario.getRol().name() : "");

        return AutenticacionResponseDTO.builder()
                .token(jwtService.generarToken(extraClaims, usuario))
                .mensaje("Registro exitoso")
                .username(usuario.getUsername())
                .cargo("Sin cargo")
                .permisos(permisos)
                .build();
    }

    public AutenticacionResponseDTO login(AutenticacionRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow();

        List<String> permisos = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("permisos", permisos);
        extraClaims.put("role", usuario.getRol() != null ? usuario.getRol().name() : "");

        if (usuario.getCargo() != null) {
            extraClaims.put("cargo", usuario.getCargo().getNombre());
        }

        return AutenticacionResponseDTO.builder()
                .token(jwtService.generarToken(extraClaims, usuario))
                .mensaje("Autenticación exitosa")
                .username(usuario.getUsername())
                .cargo(usuario.getCargo() != null ? usuario.getCargo().getNombre() : "Sin cargo")
                .permisos(permisos)
                .build();
    }
}