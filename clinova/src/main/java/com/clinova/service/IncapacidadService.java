package com.clinova.service;

import com.clinova.dto.IncapacidadDTO;
import com.clinova.entity.Incapacidad;
import com.clinova.entity.Usuario;
import com.clinova.repository.IncapacidadRepository;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncapacidadService {

    private final IncapacidadRepository incapacidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final String RUTA_DIRECTORIO = "uploads/incapacidades/";

    @Transactional
    public IncapacidadDTO crearOActualizar(Long usuarioId, IncapacidadDTO dto, MultipartFile archivo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Incapacidad incapacidad;
        if (dto.id() != null) {
            incapacidad = incapacidadRepository.findById(dto.id())
                    .orElseThrow(() -> new RuntimeException("Incapacidad no encontrada"));
        } else {
            incapacidad = new Incapacidad();
            incapacidad.setUsuario(usuario);
        }

        // Si se sube archivo
        if (archivo != null && !archivo.isEmpty()) {
            String nombreArchivoOriginal = archivo.getOriginalFilename();
            String nombreArchivoUnico = UUID.randomUUID().toString() + "_" + nombreArchivoOriginal;
            Path rutaDestino = Paths.get(RUTA_DIRECTORIO + nombreArchivoUnico);

            try {
                Files.createDirectories(rutaDestino.getParent());
                Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);
                incapacidad.setNombreArchivo(nombreArchivoOriginal);
                incapacidad.setRutaArchivo(rutaDestino.toString());
            } catch (IOException e) {
                throw new RuntimeException("Error al guardar el archivo adjunto", e);
            }
        }

        // Mapear campos
        incapacidad.setEpsArl(dto.epsArl());
        incapacidad.setTipoIncapacidad(dto.tipoIncapacidad());
        incapacidad.setCodigo(dto.codigo());
        incapacidad.setDx(dto.dx());
        incapacidad.setFechaInicio(dto.fechaInicio());
        incapacidad.setFechaFin(dto.fechaFin());
        
        // Cálculo automático de días otorgados
        if (dto.fechaInicio() != null && dto.fechaFin() != null) {
            long dias = ChronoUnit.DAYS.between(dto.fechaInicio(), dto.fechaFin()) + 1;
            incapacidad.setDiasOtorgados((int) dias);
        } else {
            incapacidad.setDiasOtorgados(dto.diasOtorgados());
        }

        incapacidad.setDiasAprobados(dto.diasAprobados());
        incapacidad.setFechaReporteTH(dto.fechaReporteTH());
        incapacidad.setFechaRadicado(dto.fechaRadicado());
        incapacidad.setEstado(dto.estado());
        incapacidad.setNumeroRadicacion(dto.numeroRadicacion());
        incapacidad.setIbc(dto.ibc());
        incapacidad.setDiasPagadosIps(dto.diasPagadosIps());
        incapacidad.setValorLiquidadoIps(dto.valorLiquidadoIps());
        incapacidad.setDiasPagadosEps(dto.diasPagadosEps());
        incapacidad.setValorLiquidadoEps(dto.valorLiquidadoEps());
        incapacidad.setDiasPagadosArl(dto.diasPagadosArl());
        incapacidad.setCampo30(dto.campo30());
        incapacidad.setCampo60(dto.campo60());
        incapacidad.setCampo90(dto.campo90());
        incapacidad.setCampo180(dto.campo180());
        incapacidad.setObservaciones(dto.observaciones());
        incapacidad.setValorPago(dto.valorPago());
        incapacidad.setFechaPago(dto.fechaPago());
        incapacidad.setNumeroComprobantePago(dto.numeroComprobantePago());

        Incapacidad guardada = incapacidadRepository.save(incapacidad);
        return mapearDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<IncapacidadDTO> obtenerPorNumeroDocumento(String numeroDocumento) {
        return incapacidadRepository.findByUsuario_Persona_NumeroDocumento(numeroDocumento).stream()
                .map(this::mapearDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<IncapacidadDTO> obtenerTodas() {
        return incapacidadRepository.findAll().stream()
                .map(this::mapearDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminar(Long id) {
        Incapacidad incapacidad = incapacidadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Incapacidad no encontrada"));

        if (incapacidad.getRutaArchivo() != null) {
            try {
                Files.deleteIfExists(Paths.get(incapacidad.getRutaArchivo()));
            } catch (IOException ignored) {}
        }
        incapacidadRepository.delete(incapacidad);
    }

    private IncapacidadDTO mapearDTO(Incapacidad i) {
        String[] cartera = i.calcularCartera();
        return new IncapacidadDTO(
                i.getId(),
                i.getUsuario().getId(),
                i.getEpsArl(),
                i.getTipoIncapacidad(),
                i.getCodigo(),
                i.getDx(),
                i.getFechaInicio(),
                i.getFechaFin(),
                i.getDiasOtorgados(),
                i.getDiasAprobados(),
                i.getFechaReporteTH(),
                i.getFechaRadicado(),
                i.getEstado(),
                i.getNumeroRadicacion(),
                i.getIbc(),
                i.getDiasPagadosIps(),
                i.getValorLiquidadoIps(),
                i.getDiasPagadosEps(),
                i.getValorLiquidadoEps(),
                i.getDiasPagadosArl(),
                cartera[0],
                cartera[1],
                cartera[2],
                cartera[3],
                i.getObservaciones(),
                i.getNombreArchivo(),
                i.getRutaArchivo(),
                i.getValorPago(),
                i.getFechaPago(),
                i.getNumeroComprobantePago()
        );
    }
}
