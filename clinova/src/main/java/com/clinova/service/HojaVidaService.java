package com.clinova.service;

import com.clinova.dto.CargoDTO;
import com.clinova.dto.SedeDTO;
import com.clinova.dto.HojaVidaRequestDTO;
import com.clinova.dto.HojaVidaResponseDTO;
import com.clinova.entity.Cargo;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Sede;
import com.clinova.entity.Usuario;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SedeRepository;
import com.clinova.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HojaVidaService {

    private final HojaVidaRepository hojaVidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CargoRepository cargoRepository;
    private final SedeRepository sedeRepository;

    private final Path rootLocation = Paths.get("uploads/fotos");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el directorio para las fotos", e);
        }
    }

    @Transactional
    public HojaVidaResponseDTO crearHojaVida(HojaVidaRequestDTO request) {

        Usuario usuario = null;
        if (request.usuarioId() != null) {
            usuario = usuarioRepository.findById(request.usuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        }

        List<Cargo> cargos = (request.cargosIds() != null && !request.cargosIds().isEmpty())
                ? cargoRepository.findAllById(request.cargosIds())
                : new ArrayList<>();

        List<Sede> sedes = (request.sedesIds() != null && !request.sedesIds().isEmpty())
                ? sedeRepository.findAllById(request.sedesIds())
                : new ArrayList<>();

        HojaVida hojaVida = HojaVida.builder()
                .nombres(request.nombres())
                .apellidos(request.apellidos())
                .cedula(request.cedula())
                .fechaNacimiento(request.fechaNacimiento())
                .direccionResidencia(request.direccionResidencia())
                .telefono(request.telefono())
                .contactoEmergencia(request.contactoEmergencia())
                .telefonoContactoEmergencia(request.telefonoContactoEmergencia())
                .arl(request.arl())
                .eps(request.eps())
                .afp(request.afp())
                .fechaIngreso(request.fechaIngreso())
                .estado(request.estado())
                .tipoContrato(request.tipoContrato())
                .fechaRetiro(request.fechaRetiro())
                .correoElectronico(request.correoElectronico())
                .pesv(request.pesv())
                .responsableEvaluacionId(request.responsableEvaluacionId())
                .fechaUltimaEdicion(LocalDateTime.now())
                .usuarioUltimaEdicion(request.usuarioUltimaEdicion())
                .usuario(usuario)
                .cargos(cargos)
                .sedes(sedes)
                .build();

        HojaVida hojaVidaGuardada = hojaVidaRepository.save(hojaVida);
        return mapearAResponseDTO(hojaVidaGuardada);
    }

    @Transactional
    public HojaVidaResponseDTO actualizarHojaVida(Long id, HojaVidaRequestDTO request) {
        HojaVida hojaVida = hojaVidaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));

        Usuario usuario = null;
        if (request.usuarioId() != null) {
            usuario = usuarioRepository.findById(request.usuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        }

        List<Cargo> cargos = (request.cargosIds() != null && !request.cargosIds().isEmpty())
                ? cargoRepository.findAllById(request.cargosIds())
                : new ArrayList<>();

        List<Sede> sedes = (request.sedesIds() != null && !request.sedesIds().isEmpty())
                ? sedeRepository.findAllById(request.sedesIds())
                : new ArrayList<>();

        hojaVida.setNombres(request.nombres());
        hojaVida.setApellidos(request.apellidos());
        hojaVida.setCedula(request.cedula());
        hojaVida.setFechaNacimiento(request.fechaNacimiento());
        hojaVida.setDireccionResidencia(request.direccionResidencia());
        hojaVida.setTelefono(request.telefono());
        hojaVida.setContactoEmergencia(request.contactoEmergencia());
        hojaVida.setTelefonoContactoEmergencia(request.telefonoContactoEmergencia());
        hojaVida.setArl(request.arl());
        hojaVida.setEps(request.eps());
        hojaVida.setAfp(request.afp());
        hojaVida.setFechaIngreso(request.fechaIngreso());
        hojaVida.setEstado(request.estado());
        hojaVida.setTipoContrato(request.tipoContrato());
        hojaVida.setFechaRetiro(request.fechaRetiro());
        hojaVida.setCorreoElectronico(request.correoElectronico());
        hojaVida.setPesv(request.pesv());
        hojaVida.setResponsableEvaluacionId(request.responsableEvaluacionId());
        hojaVida.setFechaUltimaEdicion(LocalDateTime.now());
        hojaVida.setUsuarioUltimaEdicion(request.usuarioUltimaEdicion());
        hojaVida.setUsuario(usuario);
        hojaVida.setCargos(cargos);
        hojaVida.setSedes(sedes);

        HojaVida hojaVidaActualizada = hojaVidaRepository.save(hojaVida);
        return mapearAResponseDTO(hojaVidaActualizada);
    }

    @Transactional
    public HojaVidaResponseDTO subirFoto(Long id, MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new RuntimeException("El archivo de la foto está vacío");
        }

        HojaVida hojaVida = hojaVidaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));

        try {
            String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
            Path destino = this.rootLocation.resolve(nombreArchivo).normalize();

            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            hojaVida.setFotoUrl("/uploads/fotos/" + nombreArchivo);
            hojaVida.setFechaUltimaEdicion(LocalDateTime.now());
            HojaVida hojaVidaActualizada = hojaVidaRepository.save(hojaVida);

            return mapearAResponseDTO(hojaVidaActualizada);

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la foto", e);
        }
    }

    @Transactional(readOnly = true)
    public HojaVidaResponseDTO obtenerHojaVidaPorId(Long id) {
        HojaVida hojaVida = hojaVidaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));
        return mapearAResponseDTO(hojaVida);
    }

    @Transactional(readOnly = true)
    public List<HojaVidaResponseDTO> obtenerTodasLasHojasDeVida() {
        return hojaVidaRepository.findAll().stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HojaVidaResponseDTO obtenerHojaVidaPorCedula(String cedula) {
        HojaVida hojaVida = hojaVidaRepository.findByCedula(cedula)
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));
        return mapearAResponseDTO(hojaVida);
    }

    private HojaVidaResponseDTO mapearAResponseDTO(HojaVida hojaVida) {
        Long usuarioId = hojaVida.getUsuario() != null ? hojaVida.getUsuario().getId() : null;

        List<CargoDTO> cargosDTO = hojaVida.getCargos() != null ? hojaVida.getCargos().stream()
                .map(c -> new CargoDTO(c.getId(), c.getNombre()))
                .collect(Collectors.toList()) : new ArrayList<>();

        List<SedeDTO> sedesDTO = hojaVida.getSedes() != null ? hojaVida.getSedes().stream()
                .map(s -> new SedeDTO(s.getId(), s.getNombre()))
                .collect(Collectors.toList()) : new ArrayList<>();

        return new HojaVidaResponseDTO(
                hojaVida.getId(),
                hojaVida.getNombres(),
                hojaVida.getApellidos(),
                hojaVida.getCedula(),
                hojaVida.getFechaNacimiento(),
                hojaVida.getDireccionResidencia(),
                hojaVida.getTelefono(),
                hojaVida.getContactoEmergencia(),
                hojaVida.getTelefonoContactoEmergencia(),
                hojaVida.getArl(),
                hojaVida.getEps(),
                hojaVida.getAfp(),
                hojaVida.getFotoUrl(),
                hojaVida.getFechaIngreso(),
                hojaVida.getEstado(),
                hojaVida.getTipoContrato(),
                hojaVida.getFechaRetiro(),
                hojaVida.getCorreoElectronico(),
                hojaVida.getPesv(),
                usuarioId,
                hojaVida.getResponsableEvaluacionId(),
                hojaVida.getFechaUltimaEdicion(),
                hojaVida.getUsuarioUltimaEdicion(),
                cargosDTO,
                sedesDTO
        );
    }
}