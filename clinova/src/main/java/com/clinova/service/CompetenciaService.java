package com.clinova.service;

import com.clinova.dto.EducacionRequestDTO;
import com.clinova.dto.EducacionResponseDTO;
import com.clinova.dto.ExperienciaLaboralRequestDTO;
import com.clinova.dto.ExperienciaLaboralResponseDTO;
import com.clinova.entity.Educacion;
import com.clinova.entity.ExperienciaLaboral;
import com.clinova.entity.HojaVida;
import com.clinova.repository.EducacionRepository;
import com.clinova.repository.ExperienciaLaboralRepository;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetenciaService {

    private final EducacionRepository educacionRepository;
    private final ExperienciaLaboralRepository experienciaLaboralRepository;
    private final HojaVidaRepository hojaVidaRepository;

    @Transactional
    public EducacionResponseDTO agregarEducacion(Long hojaVidaId, EducacionRequestDTO request) {
        HojaVida hojaVida = obtenerHojaVida(hojaVidaId);

        Educacion educacion = Educacion.builder()
                .nivelEstudio(request.nivelEstudio())
                .institucion(request.institucion())
                .titulo(request.titulo())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .hojaVida(hojaVida)
                .build();

        Educacion guardada = educacionRepository.save(educacion);
        return mapearEducacionDTO(guardada);
    }

    @Transactional
    public ExperienciaLaboralResponseDTO agregarExperienciaLaboral(Long hojaVidaId, ExperienciaLaboralRequestDTO request) {
        HojaVida hojaVida = obtenerHojaVida(hojaVidaId);

        ExperienciaLaboral experiencia = ExperienciaLaboral.builder()
                .empresa(request.empresa())
                .cargo(request.cargo())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .funciones(request.funciones())
                .hojaVida(hojaVida)
                .build();

        ExperienciaLaboral guardada = experienciaLaboralRepository.save(experiencia);
        return mapearExperienciaDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<EducacionResponseDTO> obtenerEducacionPorHojaVida(Long hojaVidaId) {
        return educacionRepository.findByHojaVidaId(hojaVidaId).stream()
                .map(this::mapearEducacionDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExperienciaLaboralResponseDTO> obtenerExperienciaPorHojaVida(Long hojaVidaId) {
        return experienciaLaboralRepository.findByHojaVidaId(hojaVidaId).stream()
                .map(this::mapearExperienciaDTO)
                .collect(Collectors.toList());
    }

    private HojaVida obtenerHojaVida(Long hojaVidaId) {
        return hojaVidaRepository.findById(hojaVidaId)
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));
    }

    private EducacionResponseDTO mapearEducacionDTO(Educacion educacion) {
        return new EducacionResponseDTO(
                educacion.getId(),
                educacion.getNivelEstudio(),
                educacion.getInstitucion(),
                educacion.getTitulo(),
                educacion.getFechaInicio(),
                educacion.getFechaFin(),
                educacion.getHojaVida().getId()
        );
    }

    private ExperienciaLaboralResponseDTO mapearExperienciaDTO(ExperienciaLaboral experiencia) {
        return new ExperienciaLaboralResponseDTO(
                experiencia.getId(),
                experiencia.getEmpresa(),
                experiencia.getCargo(),
                experiencia.getFechaInicio(),
                experiencia.getFechaFin(),
                experiencia.getFunciones(),
                experiencia.getHojaVida().getId()
        );
    }
}