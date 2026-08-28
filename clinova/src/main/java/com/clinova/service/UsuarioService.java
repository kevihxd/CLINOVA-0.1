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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final CargoRepository cargoRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CursosService cursosService;

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAllOptimized();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarUsuariosDTO() {
        List<Usuario> usuarios = usuarioRepository.findAllOptimized();
        List<Map<String, Object>> list = new ArrayList<>();
        
        for (Usuario u : usuarios) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("rol", u.getRol() != null ? u.getRol().name() : "USER");
            map.put("requiereCambioPassword", u.getRequiereCambioPassword() != null ? u.getRequiereCambioPassword() : false);
            
            // Persona
            Map<String, Object> personaMap = null;
            if (u.getPersona() != null) {
                personaMap = new HashMap<>();
                personaMap.put("id", u.getPersona().getId());
                personaMap.put("tipoDocumento", u.getPersona().getTipoDocumento());
                personaMap.put("numeroDocumento", u.getPersona().getNumeroDocumento());
                personaMap.put("primerNombre", u.getPersona().getPrimerNombre());
                personaMap.put("segundoNombre", u.getPersona().getSegundoNombre());
                personaMap.put("primerApellido", u.getPersona().getPrimerApellido());
                personaMap.put("segundoApellido", u.getPersona().getSegundoApellido());
                personaMap.put("fechaNacimiento", u.getPersona().getFechaNacimiento());
                personaMap.put("direccionResidencia", u.getPersona().getDireccionResidencia());
                personaMap.put("numeroTelefono", u.getPersona().getNumeroTelefono());
                personaMap.put("lugarNacimiento", u.getPersona().getLugarNacimiento());
                String mailPersona = u.getPersona().getCorreoElectronico();
                String mailHv = u.getHojaVida() != null ? u.getHojaVida().getCorreoElectronico() : null;
                String realHv = sanitizarCorreo(mailHv, u.getUsername(), u.getPersona().getNumeroDocumento());
                String realP = sanitizarCorreo(mailPersona, u.getUsername(), u.getPersona().getNumeroDocumento());
                String finalCorreo = realHv != null ? realHv : (realP != null ? realP : (mailHv != null && !mailHv.isBlank() ? mailHv : mailPersona));
                personaMap.put("correoElectronico", finalCorreo);
                personaMap.put("perfilVacunacion", u.getPersona().getPerfilVacunacion());
            }
            map.put("persona", personaMap);
            
            // Cargo
            Map<String, Object> cargoMap = null;
            if (u.getCargo() != null) {
                cargoMap = new HashMap<>();
                cargoMap.put("id", u.getCargo().getId());
                cargoMap.put("nombre", u.getCargo().getNombre());
                cargoMap.put("areaSemaforizacion", u.getCargo().getAreaSemaforizacion());
            }
            map.put("cargo", cargoMap);
            
            // HojaVida fields
            map.put("arl", u.getArl());
            map.put("eps", u.getEps());
            map.put("afp", u.getAfp());
            map.put("cajaCompensacion", u.getCajaCompensacion());
            map.put("fechaIngreso", u.getFechaIngreso());
            map.put("tipoContrato", u.getTipoContrato());
            map.put("salario", u.getSalario());
            map.put("subsidioTransporte", u.getSubsidioTransporte());
            map.put("estado", u.getEstado());
            map.put("fechaRetiro", u.getFechaRetiro());
            map.put("pesvFecha", u.getPesvFecha());
            map.put("motivoRetiro", u.getMotivoRetiro());
            map.put("sede", u.getSede());
            map.put("sedeId", u.getSedeId());
            map.put("responsableEvaluacionId", u.getResponsableEvaluacionId());
            
            list.add(map);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerReporteUsuarios() {
        List<HojaVida> hojasVida = hojaVidaRepository.findAll();
        return hojasVida.stream().map(hv -> {
            String nombre = (hv.getNombres() + " " + hv.getApellidos()).trim();
            String estadoRaw = hv.getEstado() != null ? hv.getEstado().trim().toUpperCase() : "";
            String estado = (estadoRaw.equals("ACTIVO") || estadoRaw.equals("CONTRATADO") || estadoRaw.contains("ACTIVO")) ? "ACTIVO" : "INACTIVO";
            String sede = hv.getSedes() != null && !hv.getSedes().isEmpty() ? hv.getSedes().get(0).getNombre() : "N/A";
            
            String cargo = "N/A";
            if (hv.getCargos() != null && !hv.getCargos().isEmpty()) {
                cargo = hv.getCargos().get(0).getNombre();
            } else if (hv.getUsuario() != null && hv.getUsuario().getCargo() != null) {
                cargo = hv.getUsuario().getCargo().getNombre();
            }
            
            String documento = hv.getCedula();
            String tipoContrato = hv.getTipoContrato();
            if ((tipoContrato == null || tipoContrato.isEmpty()) && hv.getUsuario() != null) {
                tipoContrato = hv.getUsuario().getTipoContrato();
            }
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", hv.getId());
            map.put("documento", documento != null ? documento : "");
            map.put("nombre", nombre.replaceAll("\\s+", " "));
            map.put("cargo", cargo != null ? cargo : "N/A");
            map.put("sede", sede != null ? sede : "N/A");
            map.put("tipoContrato", tipoContrato != null ? tipoContrato : "");
            map.put("arl", hv.getArl() != null ? hv.getArl() : "");
            map.put("eps", hv.getEps() != null ? hv.getEps() : "");
            map.put("afp", hv.getAfp() != null ? hv.getAfp() : "");
            map.put("cajaCompensacion", hv.getCajaCompensacion() != null ? hv.getCajaCompensacion() : "");
            map.put("estado", estado);
            return map;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorDocumento(String numeroDocumento) {
        return usuarioRepository.findByPersona_NumeroDocumento(numeroDocumento)
                .orElseGet(() -> usuarioRepository.findByUsername(numeroDocumento).orElse(null));
    }

    @Transactional
    public Usuario crearUsuario(UsuarioRequestDTO dto) {
        String username = dto.getUsername() != null && !dto.getUsername().trim().isEmpty() 
                ? dto.getUsername().trim() 
                : (dto.getNumeroDocumento() != null ? dto.getNumeroDocumento().trim() : null);

        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario o documento es requerido");
        }

        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario o documento ya se encuentra registrado: " + username);
        }

        Cargo cargo = null;
        if (dto.getCargoId() != null) {
            cargo = cargoRepository.findById(dto.getCargoId()).orElse(null);
        }

        String docNum = dto.getNumeroDocumento() != null ? dto.getNumeroDocumento().trim() : username;

        // Reutilizar persona si ya existe por número de documento
        Persona persona = personaRepository.findByNumeroDocumento(docNum).orElse(null);
        if (persona == null) {
            persona = Persona.builder()
                    .tipoDocumento(dto.getTipoDocumento() != null ? dto.getTipoDocumento() : "CC")
                    .numeroDocumento(docNum)
                    .primerNombre(dto.getPrimerNombre())
                    .segundoNombre(dto.getSegundoNombre())
                    .primerApellido(dto.getPrimerApellido())
                    .segundoApellido(dto.getSegundoApellido())
                    .fechaNacimiento(dto.getFechaNacimiento())
                    .direccionResidencia(dto.getDireccionResidencia())
                    .numeroTelefono(dto.getNumeroTelefono())
                    .lugarNacimiento(dto.getLugarNacimiento())
                    .correoElectronico(sanitizarCorreo(dto.getCorreoElectronico(), username, docNum))
                    .perfilVacunacion(dto.getPerfilVacunacion())
                    .build();
            persona = personaRepository.save(persona);
        } else {
            // Actualizar datos básicos si vienen en el request
            if (dto.getPrimerNombre() != null) persona.setPrimerNombre(dto.getPrimerNombre());
            if (dto.getPrimerApellido() != null) persona.setPrimerApellido(dto.getPrimerApellido());
            if (dto.getCorreoElectronico() != null) persona.setCorreoElectronico(sanitizarCorreo(dto.getCorreoElectronico(), username, docNum));
            persona = personaRepository.save(persona);
        }

        String rawPassword = dto.getPassword() != null && !dto.getPassword().trim().isEmpty() 
                ? dto.getPassword().trim() 
                : username;

        Role roleEnum = Role.USER;
        if (dto.getRol() != null && !dto.getRol().trim().isEmpty()) {
            try {
                roleEnum = Role.valueOf(dto.getRol().trim().toUpperCase());
            } catch (Exception ignored) {}
        }

        Usuario usuario = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .rol(roleEnum)
                .cargo(cargo)
                .persona(persona)
                .requiereCambioPassword(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        try {
            cursosService.asignarCursosGlobalesAUsuario(guardado);
        } catch (Exception e) {
            log.warn("No se pudieron asignar cursos globales automáticamente: {}", e.getMessage());
        }

        // Vincular o crear Hoja de Vida asociada
        String nombres = dto.getPrimerNombre() != null ? dto.getPrimerNombre().trim() : "";
        if (dto.getSegundoNombre() != null && !dto.getSegundoNombre().trim().isEmpty()) {
            nombres += " " + dto.getSegundoNombre().trim();
        }
        String apellidos = dto.getPrimerApellido() != null ? dto.getPrimerApellido().trim() : "";
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

        String currentUser = "Sistema";
        try {
            if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                currentUser = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            }
        } catch (Exception ignored) {}

        HojaVida hojaVida = hojaVidaRepository.findByCedula(docNum).orElse(null);
        if (hojaVida == null) {
            hojaVida = HojaVida.builder()
                    .cedula(docNum)
                    .build();
        }

        hojaVida.setUsuario(guardado);
        hojaVida.setNombres(nombres);
        hojaVida.setApellidos(apellidos);
        if (dto.getFechaNacimiento() != null && !dto.getFechaNacimiento().isBlank()) hojaVida.setFechaNacimiento(parseLocalDate(dto.getFechaNacimiento()));
        if (dto.getDireccionResidencia() != null) hojaVida.setDireccionResidencia(dto.getDireccionResidencia());
        if (dto.getNumeroTelefono() != null) hojaVida.setTelefono(dto.getNumeroTelefono());
        if (dto.getArl() != null) hojaVida.setArl(dto.getArl());
        if (dto.getEps() != null) hojaVida.setEps(dto.getEps());
        if (dto.getAfp() != null) hojaVida.setAfp(dto.getAfp());
        if (dto.getCajaCompensacion() != null) hojaVida.setCajaCompensacion(dto.getCajaCompensacion());
        if (dto.getSalario() != null) hojaVida.setSalario(dto.getSalario());
        if (dto.getSubsidioTransporte() != null) hojaVida.setSubsidioTransporte(dto.getSubsidioTransporte());
        hojaVida.setFechaIngreso(fechaIngreso);
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) {
            hojaVida.setEstado(dto.getEstado());
        } else if (hojaVida.getEstado() == null || hojaVida.getEstado().isBlank()) {
            hojaVida.setEstado("ACTIVO");
        }
        if (dto.getTipoContrato() != null && !dto.getTipoContrato().isBlank()) {
            hojaVida.setTipoContrato(dto.getTipoContrato());
        }
        if (dto.getFechaRetiro() != null && !dto.getFechaRetiro().isBlank()) hojaVida.setFechaRetiro(parseLocalDate(dto.getFechaRetiro()));
        if (dto.getMotivoRetiro() != null) hojaVida.setMotivoRetiro(dto.getMotivoRetiro());
        if (dto.getCorreoElectronico() != null) hojaVida.setCorreoElectronico(dto.getCorreoElectronico());
        if (dto.getPesvFecha() != null) hojaVida.setPesv(dto.getPesvFecha());
        if (dto.getPerfilVacunacion() != null) hojaVida.setPerfilVacunacion(dto.getPerfilVacunacion());
        if (dto.getResponsableEvaluacionId() != null) hojaVida.setResponsableEvaluacionId(dto.getResponsableEvaluacionId());
        
        if (!cargosList.isEmpty()) hojaVida.setCargos(cargosList);
        if (!sedes.isEmpty()) hojaVida.setSedes(sedes);
        
        hojaVida.setFechaUltimaEdicion(LocalDateTime.now());
        hojaVida.setUsuarioUltimaEdicion(currentUser);

        hojaVidaRepository.save(hojaVida);

        return guardado;
    }

    @Transactional
    public void cambiarPasswordPrimerIngreso(Long usuarioId, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setRequiereCambioPassword(false);

        usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarUsuario(Long id, UsuarioRequestDTO dto) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (dto.getRol() != null && !dto.getRol().trim().isEmpty()) {
            usuarioExistente.setRol(parseRole(dto.getRol()));
        }

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            usuarioExistente.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getCargoId() != null) {
            Cargo cargo = cargoRepository.findById(dto.getCargoId()).orElse(null);
            if (cargo != null) {
                usuarioExistente.setCargo(cargo);
            }
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
        pExistente.setCorreoElectronico(sanitizarCorreo(dto.getCorreoElectronico(), usuarioExistente.getUsername(), dto.getNumeroDocumento()));
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
        if (hojaVida == null && dto.getNumeroDocumento() != null && !dto.getNumeroDocumento().trim().isEmpty()) {
            hojaVida = hojaVidaRepository.findByCedula(dto.getNumeroDocumento().trim()).orElse(null);
        }
        if (hojaVida == null) {
            hojaVida = HojaVida.builder()
                    .usuario(guardado)
                    .build();
        } else {
            hojaVida.setUsuario(guardado);
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
        hojaVida.setCorreoElectronico(sanitizarCorreo(dto.getCorreoElectronico(), usuarioExistente.getUsername(), dto.getNumeroDocumento()));
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
        try {
            Usuario usuario = usuarioRepository.findById(id).orElse(null);
            if (usuario == null) {
                log.warn("Usuario no encontrado id={}", id);
                return;
            }

            Long personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;

            // 1. Desvincular relaciones ORM
            try {
                usuario.setPersona(null);
                usuario.setCargo(null);
                usuarioRepository.saveAndFlush(usuario);
            } catch (Exception ignored) {}

            try {
                HojaVida hv = hojaVidaRepository.findByUsuario_Id(id).orElse(null);
                if (hv != null) {
                    hv.setUsuario(null);
                    try { hv.getCargos().clear(); } catch (Exception ignored) {}
                    try { hv.getSedes().clear(); } catch (Exception ignored) {}
                    hojaVidaRepository.saveAndFlush(hv);
                }
            } catch (Exception ignored) {}

            // 2. Ejecutar borrado SQL directo para evadir bloqueos de FK
            try { entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM hojas_vida_cargos WHERE hoja_vida_id IN (SELECT id FROM hojas_vida WHERE usuario_id = " + id + ")").executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM hojas_vida_sedes WHERE hoja_vida_id IN (SELECT id FROM hojas_vida WHERE usuario_id = " + id + ")").executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM soportes WHERE hoja_vida_id IN (SELECT id FROM hojas_vida WHERE usuario_id = " + id + ")").executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM educaciones WHERE hoja_vida_id IN (SELECT id FROM hojas_vida WHERE usuario_id = " + id + ")").executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM experiencias_laborales WHERE hoja_vida_id IN (SELECT id FROM hojas_vida WHERE usuario_id = " + id + ")").executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM cursos WHERE hoja_vida_id IN (SELECT id FROM hojas_vida WHERE usuario_id = " + id + ")").executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM curso_asignados WHERE usuario_id = " + id).executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM incapacidades WHERE usuario_id = " + id).executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("UPDATE comentarios_actas SET autor_id = NULL WHERE autor_id = " + id).executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM hojas_vida WHERE usuario_id = " + id).executeUpdate(); } catch (Exception ignored) {}
            try { entityManager.createNativeQuery("DELETE FROM usuarios WHERE id = " + id).executeUpdate(); } catch (Exception ignored) {}
            if (personaId != null) {
                try { entityManager.createNativeQuery("DELETE FROM personas WHERE id = " + personaId).executeUpdate(); } catch (Exception ignored) {}
            }
            try { entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate(); } catch (Exception ignored) {}

            try {
                if (usuarioRepository.existsById(id)) {
                    usuarioRepository.deleteById(id);
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            log.error("Error en eliminarUsuario id={}: {}", id, e.getMessage(), e);
        }
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

    private Role parseRole(String rolStr) {
        if (rolStr == null || rolStr.trim().isEmpty()) return Role.USER;
        try {
            return Role.valueOf(rolStr.trim().toUpperCase());
        } catch (Exception e) {
            String upper = rolStr.trim().toUpperCase();
            if (upper.contains("ADMIN")) return Role.ADMIN;
            if (upper.contains("HR") || upper.contains("TRABAJADOR") || upper.contains("MANAGER")) return Role.HR_MANAGER;
            if (upper.contains("LIDER")) return Role.LIDER_DE_PROCESO;
            return Role.USER;
        }
    }

    public String sanitizarCorreo(String email, String username, String docNum) {
        if (email == null || email.trim().isEmpty()) return null;
        String clean = email.trim().toLowerCase();
        if (!clean.contains("@")) return null;
        String localPart = clean.split("@")[0];

        String u = username != null ? username.trim().toLowerCase() : "";
        String d = docNum != null ? docNum.trim().toLowerCase() : "";

        if (!u.isEmpty() && localPart.equals(u)) return null;
        if (!d.isEmpty() && localPart.equals(d)) return null;
        if (localPart.matches("^\\d+$")) return null;

        return email.trim();
    }
}