package com.clinova.dto;

import java.time.LocalDateTime;

public record PlantillaDTO(
        Long id,
        String titulo,
        String descripcion,
        String contenidoHtml,
        LocalDateTime fechaCreacion
) {}