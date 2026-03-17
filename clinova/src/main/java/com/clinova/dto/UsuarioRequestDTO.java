package com.clinova.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
        @NotBlank(message = "El username es obligatorio")
        String username,

        @NotBlank(message = "El password es obligatorio")
        @Size(min = 8, message = "El password debe tener al menos 8 caracteres")
        String password,

        @NotBlank(message = "Los nombres son obligatorios")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        String apellidos,

        @NotBlank(message = "El número de documento es obligatorio")
        String numeroDocumento,

        @NotNull(message = "El ID del tipo de documento es obligatorio")
        Long tipoDocumentoId
) {}