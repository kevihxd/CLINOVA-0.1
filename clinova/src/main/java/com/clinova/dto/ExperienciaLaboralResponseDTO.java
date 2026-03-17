package com.clinova.dto;

import java.time.LocalDate;

public record ExperienciaLaboralResponseDTO(
        Long id,
        String empresa,
        String cargo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String funciones,
        Long hojaVidaId
) {}