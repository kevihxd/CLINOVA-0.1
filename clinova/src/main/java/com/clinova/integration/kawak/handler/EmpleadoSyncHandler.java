package com.clinova.integration.kawak.handler;

import com.clinova.entity.HojaVida;
import com.clinova.integration.kawak.dto.KawakEmpleadoDTO;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmpleadoSyncHandler {

    private final HojaVidaRepository hojaVidaRepository;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    @Transactional
    public int[] sincronizar(List<KawakEmpleadoDTO> dtos) {
        int creados = 0;
        int actualizados = 0;

        for (KawakEmpleadoDTO dto : dtos) {
            if (dto.id() == null && (dto.cedula() == null || dto.cedula().isBlank())) continue;
            try {
                // Busca por kawakId primero, luego por cédula como fallback
                HojaVida hv;
                boolean esNuevo;

                if (dto.id() != null) {
                    esNuevo = hojaVidaRepository.findByKawakId(dto.id()).isEmpty();
                    hv = hojaVidaRepository.findByKawakId(dto.id())
                            .orElseGet(() -> hojaVidaRepository.findByCedula(dto.cedula()).orElse(new HojaVida()));
                } else {
                    esNuevo = hojaVidaRepository.findByCedula(dto.cedula()).isEmpty();
                    hv = hojaVidaRepository.findByCedula(dto.cedula()).orElse(new HojaVida());
                }

                hv.setKawakId(dto.id());
                hv.setNombres(strOrDefault(dto.nombres(), "Sin nombre"));
                hv.setApellidos(strOrDefault(dto.apellidos(), "Sin apellido"));
                hv.setCedula(strOrDefault(dto.cedula(), "0"));
                hv.setFechaNacimiento(parseFecha(dto.fechaNacimiento()));
                hv.setFechaIngreso(parseFecha(dto.fechaIngreso()) != null ? parseFecha(dto.fechaIngreso()) : LocalDate.now());
                hv.setFechaRetiro(parseFecha(dto.fechaRetiro()));
                hv.setEstado(strOrDefault(dto.estado(), "ACTIVO"));
                hv.setTipoContrato(dto.tipoContrato());
                hv.setCorreoElectronico(dto.correoElectronico());
                hv.setTelefono(dto.telefono());
                hv.setDireccionResidencia(dto.direccionResidencia());
                hv.setArl(dto.arl());
                hv.setEps(dto.eps());
                hv.setAfp(dto.afp());
                hv.setCajaCompensacion(dto.cajaCompensacion());
                hv.setSalario(dto.salario());
                hv.setPerfilVacunacion(dto.perfilVacunacion());
                hv.setContactoEmergencia(dto.contactoEmergencia());
                hv.setTelefonoContactoEmergencia(dto.telefonoContactoEmergencia());
                hv.setFechaUltimaEdicion(LocalDateTime.now());
                hv.setUsuarioUltimaEdicion("KAWAK_SYNC");

                hojaVidaRepository.save(hv);

                if (esNuevo) creados++;
                else actualizados++;

            } catch (Exception e) {
                log.error("Error al sincronizar empleado kawakId={} cedula={}: {}", dto.id(), dto.cedula(), e.getMessage());
            }
        }

        log.info("Empleados sincronizados — creados: {}, actualizados: {}", creados, actualizados);
        return new int[]{creados, actualizados};
    }

    private LocalDate parseFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try { return LocalDate.parse(fecha, fmt); } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private String strOrDefault(String val, String def) {
        return (val != null && !val.isBlank()) ? val : def;
    }
}
