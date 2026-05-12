package com.clinova.dto;

import java.time.LocalDate;

public record ReporteCursoDTO(
        String documento,
        String nombres,
        String apellidos,
        String curso,
        String estado,
        LocalDate fechaRealizacion,
        LocalDate fechaExpiracion,
        String certificadoUrl
) {}
