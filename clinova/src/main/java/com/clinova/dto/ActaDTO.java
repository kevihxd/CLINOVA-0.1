package com.clinova.dto;

import java.time.LocalDate;

public record ActaDTO(
        Long id,
        String titulo,
        String contenidoHtml,
        String estado,
        String tipo,
        String responsable,
        LocalDate fecha
) {}