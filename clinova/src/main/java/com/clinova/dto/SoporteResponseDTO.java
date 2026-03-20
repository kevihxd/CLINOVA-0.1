package com.clinova.dto;

import java.time.LocalDateTime;

public record SoporteResponseDTO(
        Long id,
        String tipoDocumento,
        String nombreArchivo,
        String rutaArchivo,
        Long tamano,
        LocalDateTime fechaCarga,
        String estado,
        Long hojaVidaId
) {}