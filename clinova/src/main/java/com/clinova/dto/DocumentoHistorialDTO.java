package com.clinova.dto;

import java.time.LocalDateTime;

public record DocumentoHistorialDTO(
        Long id,
        Long documentoId,
        String version,
        String accion,
        String descripcion,
        String usuario,
        LocalDateTime fecha
) {}
