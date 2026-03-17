package com.clinova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ExperienciaLaboralRequestDTO(
        @NotBlank(message = "El nombre de la empresa es obligatorio")
        String empresa,

        @NotBlank(message = "El cargo es obligatorio")
        String cargo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        LocalDate fechaFin,
        String funciones
) {}