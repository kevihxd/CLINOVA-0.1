package com.clinova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EducacionRequestDTO(
        @NotBlank(message = "El nivel de estudio es obligatorio")
        String nivelEstudio,

        @NotBlank(message = "La institución es obligatoria")
        String institucion,

        @NotBlank(message = "El título es obligatorio")
        String titulo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        LocalDate fechaFin
) {}