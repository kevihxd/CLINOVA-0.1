package com.clinova.dto;

public record ReporteVacunacionDTO(
        String cedula,
        String nombres,
        String apellidos,
        String perfilVacunacion,
        String detalleVacunas,
        String estadoSemaforo
) {}