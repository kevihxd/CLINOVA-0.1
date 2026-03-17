package com.clinova.controller;

import com.clinova.dto.EducacionRequestDTO;
import com.clinova.dto.EducacionResponseDTO;
import com.clinova.dto.ExperienciaLaboralRequestDTO;
import com.clinova.dto.ExperienciaLaboralResponseDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.CompetenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hojas-vida/{hojaVidaId}/competencias")
@RequiredArgsConstructor
public class CompetenciaController {

    private final CompetenciaService competenciaService;

    @PostMapping("/educacion")
    @ResponseStatus(HttpStatus.CREATED)
    public StructureResponses<EducacionResponseDTO> agregarEducacion(
            @PathVariable Long hojaVidaId,
            @Valid @RequestBody EducacionRequestDTO request) {
        EducacionResponseDTO response = competenciaService.agregarEducacion(hojaVidaId, request);
        return new StructureResponses<>("SUCCESS", "Educación agregada exitosamente", response);
    }

    @PostMapping("/experiencia")
    @ResponseStatus(HttpStatus.CREATED)
    public StructureResponses<ExperienciaLaboralResponseDTO> agregarExperiencia(
            @PathVariable Long hojaVidaId,
            @Valid @RequestBody ExperienciaLaboralRequestDTO request) {
        ExperienciaLaboralResponseDTO response = competenciaService.agregarExperienciaLaboral(hojaVidaId, request);
        return new StructureResponses<>("SUCCESS", "Experiencia laboral agregada exitosamente", response);
    }

    @GetMapping("/educacion")
    public StructureResponses<List<EducacionResponseDTO>> obtenerEducacion(@PathVariable Long hojaVidaId) {
        List<EducacionResponseDTO> response = competenciaService.obtenerEducacionPorHojaVida(hojaVidaId);
        return new StructureResponses<>("SUCCESS", "Educación obtenida exitosamente", response);
    }

    @GetMapping("/experiencia")
    public StructureResponses<List<ExperienciaLaboralResponseDTO>> obtenerExperiencia(@PathVariable Long hojaVidaId) {
        List<ExperienciaLaboralResponseDTO> response = competenciaService.obtenerExperienciaPorHojaVida(hojaVidaId);
        return new StructureResponses<>("SUCCESS", "Experiencia laboral obtenida exitosamente", response);
    }
}