package com.clinova.dto;

import java.time.LocalDateTime;

public record ActaHistorialDTO(
        Long id,
        Long actaId,
        String accion,
        String descripcion,
        String usuario,
        LocalDateTime fecha
) {}
