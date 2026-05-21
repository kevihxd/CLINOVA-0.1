package com.clinova.dto;

import java.time.LocalDateTime;

public record HojaVidaHistorialDTO(
        Long id,
        Long hojaVidaId,
        String accion,
        String descripcion,
        String usuario,
        LocalDateTime fecha
) {}
