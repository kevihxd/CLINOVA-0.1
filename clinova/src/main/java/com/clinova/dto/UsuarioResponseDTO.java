package com.clinova.dto;

public record UsuarioResponseDTO(
        Long id,
        String username,
        String nombres,
        String apellidos,
        String numeroDocumento
) {}