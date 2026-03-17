package com.clinova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SoporteRequestDTO(
        @NotBlank(message = "El tipo de documento es obligatorio")
        String tipoDocumento,

        @NotNull(message = "El ID de la hoja de vida es obligatorio")
        Long hojaVidaId
) {}