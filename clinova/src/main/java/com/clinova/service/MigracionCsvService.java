package com.clinova.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MigracionCsvService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final CargoRepository cargoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void migrarDesdeCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("El archivo CSV está vacío");
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                String[] datos = linea.split(",");

                if (datos.length < 12) continue;

                String username = datos[4].trim();

                if (!usuarioRepository.existsByUsername(username)) {

                    String[] nombres = datos[2].trim().split(" ", 2);
                    String primerNombre = nombres[0];
                    String segundoNombre = nombres.length > 1 ? nombres[1] : "";

                    String[] apellidos = datos[3].trim().split(" ", 2);
                    String primerApellido = apellidos[0];
                    String segundoApellido = apellidos.length > 1 ? apellidos[1] : "";

                    Persona persona = Persona.builder()
                            .tipoDocumento("CC")
                            .numeroDocumento(datos[1].trim())
                            .primerNombre(primerNombre)
                            .segundoNombre(segundoNombre)
                            .primerApellido(primerApellido)
                            .segundoApellido(segundoApellido)
                            .correoElectronico(datos[6].trim())
                            .numeroTelefono("")
                            .build();

                    personaRepository.save(persona);

                    String nombreCargoCsv = datos[5].trim();
                    Cargo cargoVinculado = cargoRepository.findByNombre(nombreCargoCsv).orElse(null);

                    Role rolAsignado = datos[11].trim().equalsIgnoreCase("Sí") ? Role.ADMIN : Role.USER;

                    Usuario usuario = Usuario.builder()
                            .username(username)
                            .password(passwordEncoder.encode("123456"))
                            .rol(rolAsignado)
                            .persona(persona)
                            .cargo(cargoVinculado)
                            .build();

                    usuarioRepository.save(usuario);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error procesando el archivo CSV: " + e.getMessage());
        }
    }
}