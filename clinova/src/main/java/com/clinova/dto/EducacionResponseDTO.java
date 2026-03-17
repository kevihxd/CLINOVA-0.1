package com.clinova.dto;

import java.time.LocalDate;

public record EducacionResponseDTO(
        Long id,
        String nivelEstudio,
        String institucion,
        String titulo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Long hojaVidaId
) {}