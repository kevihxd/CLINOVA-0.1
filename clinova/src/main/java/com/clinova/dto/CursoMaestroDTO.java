package com.clinova.dto;

import java.time.LocalDate;

public record CursoMaestroDTO(
        Long id,
        String nombre,
        String descripcion,
        LocalDate fechaLimiteGlobal,
        Boolean esGlobal,
        Integer mesesVigencia
) {}