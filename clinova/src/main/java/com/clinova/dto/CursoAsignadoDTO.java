package com.clinova.dto;

import java.time.LocalDate;

public record CursoAsignadoDTO(
        Long id,
        String cursoNombre,
        String descripcion,
        String usuarioNombre,
        String estado,
        LocalDate fechaRealizacion,
        LocalDate fechaExpiracion,
        LocalDate fechaLimite,
        String certificadoUrl,
        boolean permiteCarga,
        boolean esGlobal
) {}