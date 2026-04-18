package com.clinova.dto;

import java.time.LocalDate;

public record CursoDTO(
        Long id,
        String nombre,
        String institucion,
        String fechaFinalizacion,
        Long soporteId,
        String nombreSoporte,
        String urlSoporte
) {}