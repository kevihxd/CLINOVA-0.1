package com.clinova.service;

import com.clinova.dto.CursoSemaforoDTO;
import com.clinova.dto.SemaforizacionReporteDTO;
import com.clinova.entity.CursoAsignado;
import com.clinova.entity.CursoMaestro;
import com.clinova.entity.HojaVida;
import com.clinova.repository.CursoAsignadoRepository;
import com.clinova.repository.CursoMaestroRepository;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemaforizacionService {

    private final HojaVidaRepository hojaVidaRepository;
    private final CursoMaestroRepository cursoMaestroRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public List<SemaforizacionReporteDTO> generarReporteSemaforizacion() {
        List<HojaVida> hojasVida = hojaVidaRepository.findAll();
        List<CursoMaestro> cursosGlobales = cursoMaestroRepository.findByEsGlobalTrue();
        LocalDate hoy = LocalDate.now();

        return hojasVida.stream()
                .filter(hv -> "ACTIVO".equalsIgnoreCase(hv.getEstado()) || hv.getEstado() == null)
                .map(hv -> mapToReporteDTO(hv, cursosGlobales, hoy))
                .collect(Collectors.toList());
    }

    private SemaforizacionReporteDTO mapToReporteDTO(HojaVida hv, List<CursoMaestro> cursosGlobales, LocalDate hoy) {
        SemaforizacionReporteDTO dto = SemaforizacionReporteDTO.builder()
                .hojaVidaId(hv.getId())
                .usuarioId(hv.getUsuario() != null ? hv.getUsuario().getId() : null)
                .nombreCompleto(hv.getNombres() + " " + hv.getApellidos())
                .identificacion(hv.getCedula())
                .cargo(hv.getCargos() != null && !hv.getCargos().isEmpty() ? hv.getCargos().get(0).getNombre() : "SIN CARGO")
                .sede(hv.getSedes() != null && !hv.getSedes().isEmpty() ? hv.getSedes().get(0).getNombre() : "SIN SEDE")
                .tipoContrato(hv.getTipoContrato())
                .valorContrato(hv.getValorContrato())
                .tiempoDuracionContrato(hv.getTiempoDuracionContrato())
                .fechaContratoInicial(hv.getFechaIngreso() != null ? hv.getFechaIngreso().format(formatter) : null)
                .build();

        // Calcular semaforizacion de contrato
        if (hv.getFechaRetiro() != null) {
            dto.setFechaFinalizacionContrato(hv.getFechaRetiro().format(formatter));
            long diasContrato = ChronoUnit.DAYS.between(hoy, hv.getFechaRetiro());
            dto.setDiasFinalizacionContrato(diasContrato);
            dto.setEstadoContrato(determinarEstado(diasContrato));
        } else {
            dto.setDiasFinalizacionContrato(null);
            dto.setEstadoContrato("NO DEFINIDO");
        }

        // Calcular semaforizacion de cursos
        List<CursoSemaforoDTO> cursosSemaforo = new ArrayList<>();
        List<CursoAsignado> asignados = cursoAsignadoRepository.findByHojaVidaId(hv.getId());
        Map<Long, CursoAsignado> asignadosMap = asignados.stream()
                .collect(Collectors.toMap(c -> c.getCursoMaestro().getId(), c -> c));

        for (CursoMaestro maestro : cursosGlobales) {
            CursoAsignado asignado = asignadosMap.get(maestro.getId());
            if (asignado != null && asignado.getFechaExpiracion() != null) {
                long diasCurso = ChronoUnit.DAYS.between(hoy, asignado.getFechaExpiracion());
                cursosSemaforo.add(CursoSemaforoDTO.builder()
                        .cursoMaestroId(maestro.getId())
                        .nombreCurso(maestro.getNombre())
                        .fechaRealizacion(asignado.getFechaRealizacion() != null ? asignado.getFechaRealizacion().format(formatter) : null)
                        .fechaExpiracion(asignado.getFechaExpiracion().format(formatter))
                        .diasRestantes(diasCurso)
                        .estado(determinarEstado(diasCurso))
                        .build());
            } else {
                // No tiene el curso o no tiene fecha expiracion
                cursosSemaforo.add(CursoSemaforoDTO.builder()
                        .cursoMaestroId(maestro.getId())
                        .nombreCurso(maestro.getNombre())
                        .estado("NO ASIGNADO")
                        .build());
            }
        }
        
        dto.setCursos(cursosSemaforo);
        return dto;
    }

    private String determinarEstado(long diasRestantes) {
        if (diasRestantes < 0) {
            return "VENCIDO";
        } else if (diasRestantes <= 30) {
            return "PROXIMO A VENCER";
        } else {
            return "VIGENTE";
        }
    }
}
