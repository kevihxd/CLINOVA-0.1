package com.clinova.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActaDTO(
        Long id,
        String titulo,
        LocalDate fecha,
        String tipo,
        String estado,
        String responsable,
        String contenidoHtml,
        LocalDateTime fechaCreacion
) {}