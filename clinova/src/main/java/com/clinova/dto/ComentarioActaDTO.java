package com.clinova.dto;

import java.time.LocalDateTime;

public record ComentarioActaDTO(
        Long id,
        String contenido,
        LocalDateTime fechaCreacion,
        String autorNombre,
        String autorUsername
) {}