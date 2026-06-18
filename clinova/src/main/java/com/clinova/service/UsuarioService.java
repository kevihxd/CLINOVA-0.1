package com.clinova.service;

import com.clinova.dto.UsuarioRequestDTO;
import com.clinova.entity.Cargo;
import com.clinova.entity.Persona;
import com.clinova.entity.Role;
import com.clinova.entity.Usuario;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Sede;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.PersonaRepository;
import com.clinova.repository.UsuarioRepository;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final CargoRepository cargoRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CursosService cursosService;

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorDocumento(String numeroDocumento) {
        return usuarioRepository.findByPersona_NumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
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

        Usuario guardado = usuarioRepository.save(usuario);
        cursosService.asignarCursosGlobalesAUsuario(guardado);

        // Crear Hoja de Vida asociada
        String nombres = dto.getPrimerNombre();
        if (dto.getSegundoNombre() != null && !dto.getSegundoNombre().trim().isEmpty()) {
            nombres += " " + dto.getSegundoNombre().trim();
        }
        String apellidos = dto.getPrimerApellido();
        if (dto.getSegundoApellido() != null && !dto.getSegundoApellido().trim().isEmpty()) {
            apellidos += " " + dto.getSegundoApellido().trim();
        }

        List<Sede> sedes = new ArrayList<>();
        if (dto.getSedeId() != null) {
            sedeRepository.findById(dto.getSedeId()).ifPresent(sedes::add);
        }

        List<Cargo> cargosList = new ArrayList<>();
        if (cargo != null) {
            cargosList.add(cargo);
        }

        LocalDate fechaIngreso = parseLocalDate(dto.getFechaIngreso());
        if (fechaIngreso == null) {
            fechaIngreso = LocalDate.now();
        }

        String currentUser = null;
        try {
            currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            currentUser = "Sistema";
        }

        HojaVida hojaVida = HojaVida.builder()
                .nombres(nombres)
                .apellidos(apellidos)
                .cedula(dto.getNumeroDocumento())
                .fechaNacimiento(parseLocalDate(dto.getFechaNacimiento()))
                .direccionResidencia(dto.getDireccionResidencia())
                .telefono(dto.getNumeroTelefono())
                .arl(dto.getArl())
                .eps(dto.getEps())
                .afp(dto.getAfp())
                .cajaCompensacion(dto.getCajaCompensacion())
                .salario(dto.getSalario())
                .subsidioTransporte(dto.getSubsidioTransporte())
                .fechaIngreso(fechaIngreso)
                .estado(dto.getEstado())
                .tipoContrato(dto.getTipoContrato())
                .fechaRetiro(parseLocalDate(dto.getFechaRetiro()))
                .motivoRetiro(dto.getMotivoRetiro())
                .correoElectronico(dto.getCorreoElectronico())
                .pesv(dto.getPesvFecha())
                .perfilVacunacion(dto.getPerfilVacunacion())
                .responsableEvaluacionId(dto.getResponsableEvaluacionId())
                .usuario(guardado)
                .cargos(cargosList)
                .sedes(sedes)
                .fechaUltimaEdicion(LocalDateTime.now())
                .usuarioUltimaEdicion(currentUser)
                .build();

        hojaVidaRepository.save(hojaVida);

        return guardado;
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
        Usuario guardado = usuarioRepository.save(usuarioExistente);

        // Actualizar/Crear Hoja de Vida asociada
        String nombres = dto.getPrimerNombre();
        if (dto.getSegundoNombre() != null && !dto.getSegundoNombre().trim().isEmpty()) {
            nombres += " " + dto.getSegundoNombre().trim();
        }
        String apellidos = dto.getPrimerApellido();
        if (dto.getSegundoApellido() != null && !dto.getSegundoApellido().trim().isEmpty()) {
            apellidos += " " + dto.getSegundoApellido().trim();
        }

        List<Sede> sedes = new ArrayList<>();
        if (dto.getSedeId() != null) {
            sedeRepository.findById(dto.getSedeId()).ifPresent(sedes::add);
        }

        List<Cargo> cargosList = new ArrayList<>();
        if (guardado.getCargo() != null) {
            cargosList.add(guardado.getCargo());
        }

        LocalDate fechaIngreso = parseLocalDate(dto.getFechaIngreso());
        if (fechaIngreso == null) {
            fechaIngreso = LocalDate.now();
        }

        String currentUser = null;
        try {
            currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            currentUser = "Sistema";
        }

        HojaVida hojaVida = hojaVidaRepository.findByUsuario_Id(guardado.getId()).orElse(null);
        if (hojaVida == null) {
            hojaVida = HojaVida.builder()
                    .usuario(guardado)
                    .build();
        }

        hojaVida.setNombres(nombres);
        hojaVida.setApellidos(apellidos);
        hojaVida.setCedula(dto.getNumeroDocumento());
        hojaVida.setFechaNacimiento(parseLocalDate(dto.getFechaNacimiento()));
        hojaVida.setDireccionResidencia(dto.getDireccionResidencia());
        hojaVida.setTelefono(dto.getNumeroTelefono());
        hojaVida.setArl(dto.getArl());
        hojaVida.setEps(dto.getEps());
        hojaVida.setAfp(dto.getAfp());
        hojaVida.setCajaCompensacion(dto.getCajaCompensacion());
        hojaVida.setSalario(dto.getSalario());
        hojaVida.setSubsidioTransporte(dto.getSubsidioTransporte());
        hojaVida.setFechaIngreso(fechaIngreso);
        hojaVida.setEstado(dto.getEstado());
        hojaVida.setTipoContrato(dto.getTipoContrato());
        hojaVida.setFechaRetiro(parseLocalDate(dto.getFechaRetiro()));
        hojaVida.setMotivoRetiro(dto.getMotivoRetiro());
        hojaVida.setCorreoElectronico(dto.getCorreoElectronico());
        hojaVida.setPesv(dto.getPesvFecha());
        hojaVida.setPerfilVacunacion(dto.getPerfilVacunacion());
        hojaVida.setResponsableEvaluacionId(dto.getResponsableEvaluacionId());
        hojaVida.setCargos(cargosList);
        hojaVida.setSedes(sedes);
        hojaVida.setFechaUltimaEdicion(LocalDateTime.now());
        hojaVida.setUsuarioUltimaEdicion(currentUser);

        hojaVidaRepository.save(hojaVida);

        return guardado;
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}