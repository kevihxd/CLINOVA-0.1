package com.clinova.service;

import com.clinova.dto.ReporteTalentoHumanoDTO;
import com.clinova.dto.ReporteVacunacionDTO;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InformesService {

    private final HojaVidaRepository hojaVidaRepository;

    @Transactional(readOnly = true)
    public List<ReporteVacunacionDTO> generarReporteVacunacion() {
        return hojaVidaRepository.findAll().stream()
                .map(hv -> {
                    String detalle = hv.getDetalleVacunas() != null ? hv.getDetalleVacunas() : "Sin registro";
                    String perfil = hv.getPerfilVacunacion() != null ? hv.getPerfilVacunacion() : "No Asignado";
                    String semaforo = determinarSemaforo(detalle);

                    return new ReporteVacunacionDTO(
                            hv.getCedula(),
                            hv.getNombres(),
                            hv.getApellidos(),
                            perfil,
                            detalle,
                            semaforo
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteTalentoHumanoDTO> generarReporteTalentoHumano() {
        return hojaVidaRepository.findAll().stream()
                .map(hv -> new ReporteTalentoHumanoDTO(
                        hv.getCedula(),
                        hv.getNombres(),
                        hv.getApellidos(),
                        "Cargo en revisión", // Valor seguro (tu entidad HojaVida no tiene getCargo)
                        hv.getTipoContrato() != null ? hv.getTipoContrato() : "No Definido",
                        hv.getEstado() != null ? hv.getEstado() : "ACTIVO"
                ))
                .collect(Collectors.toList());
    }

    private String determinarSemaforo(String detalle) {
        String detLower = detalle.toLowerCase();
        if (detLower.isEmpty() || detLower.contains("sin registro") || detLower.contains("vencid")) {
            return "ROJO";
        }
        if (detLower.contains("pendiente") || detLower.contains("incompleto") || detLower.contains("dosis")) {
            return "AMARILLO";
        }
        return "VERDE";
    }
}