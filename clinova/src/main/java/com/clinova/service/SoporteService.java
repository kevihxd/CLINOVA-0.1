package com.clinova.service;

import com.clinova.dto.RechazoSoporteRequestDTO;
import com.clinova.dto.SoporteRequestDTO;
import com.clinova.dto.SoporteResponseDTO;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Soporte;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SoporteRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SoporteService {

    private final SoporteRepository soporteRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final EmailService emailService;
    private final String RUTA_DIRECTORIO = "uploads/soportes/";

    @Transactional
    public SoporteResponseDTO crearSoporte(SoporteRequestDTO request, MultipartFile archivo) {
        if (archivo.isEmpty() || !archivo.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("El archivo debe ser un PDF válido y no estar vacío");
        }

        HojaVida hojaVida = hojaVidaRepository.findById(request.hojaVidaId())
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));

        String nombreArchivoOriginal = archivo.getOriginalFilename();
        String nombreArchivoUnico = UUID.randomUUID().toString() + "_" + nombreArchivoOriginal;
        Path rutaDestino = Paths.get(RUTA_DIRECTORIO + nombreArchivoUnico);

        try {
            Files.createDirectories(rutaDestino.getParent());
            Files.copy(archivo.getInputStream(), rutaDestino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo PDF", e);
        }

        Soporte soporte = Soporte.builder()
                .tipoDocumento(request.tipoDocumento())
                .nombreArchivo(nombreArchivoOriginal)
                .rutaArchivo(rutaDestino.toString())
                .tamano(archivo.getSize())
                .fechaCarga(LocalDateTime.now())
                .estado("Aprobado")
                .hojaVida(hojaVida)
                .build();

        Soporte soporteGuardado = soporteRepository.save(soporte);
        return mapearAResponseDTO(soporteGuardado);
    }

    @Transactional(readOnly = true)
    public List<SoporteResponseDTO> obtenerSoportesPorHojaVidaId(Long hojaVidaId) {
        return soporteRepository.findByHojaVidaId(hojaVidaId).stream()
                .map(this::mapearAResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminarSoporte(Long id) {
        Soporte soporte = soporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Soporte no encontrado"));

        try {
            Files.deleteIfExists(Paths.get(soporte.getRutaArchivo()));
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar el archivo físico", e);
        }

        soporteRepository.delete(soporte);
    }

    @Transactional
    public SoporteResponseDTO actualizarTipoDocumento(Long id, String nuevoTipo) {
        Soporte soporte = soporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Soporte no encontrado"));

        soporte.setTipoDocumento(nuevoTipo);
        Soporte actualizado = soporteRepository.save(soporte);
        return mapearAResponseDTO(actualizado);
    }

    @Transactional
    public SoporteResponseDTO rechazarSoporte(Long id, RechazoSoporteRequestDTO request) {
        Soporte soporte = soporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Soporte no encontrado"));

        soporte.setEstado("Rechazado");
        Soporte actualizado = soporteRepository.save(soporte);

        String correoEmpleado = soporte.getHojaVida().getCorreoElectronico();
        if (correoEmpleado != null && !correoEmpleado.trim().isEmpty()) {
            emailService.enviarNotificacionRechazo(
                    correoEmpleado,
                    soporte.getTipoDocumento(),
                    request.getMotivo(),
                    request.getFechaLimite()
            );
        }

        return mapearAResponseDTO(actualizado);
    }

    private SoporteResponseDTO mapearAResponseDTO(Soporte soporte) {
        return new SoporteResponseDTO(
                soporte.getId(),
                soporte.getTipoDocumento(),
                soporte.getNombreArchivo(),
                soporte.getRutaArchivo(),
                soporte.getTamano(),
                soporte.getFechaCarga(),
                soporte.getEstado(),
                soporte.getHojaVida().getId()
        );
    }
}