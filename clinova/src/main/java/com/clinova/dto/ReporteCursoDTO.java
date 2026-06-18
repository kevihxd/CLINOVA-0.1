package com.clinova.dto;

import java.time.LocalDate;

public record ReporteCursoDTO(
        String documento,
        String nombres,
        String apellidos,
        String cargo,
        String sede,
        String curso,
        String descripcionCurso,
        Integer mesesVigencia,
        String estado,
        LocalDate fechaRealizacion,
        LocalDate fechaExpiracion,
        LocalDate fechaLimite,
        String certificadoUrl
) {}
