package com.clinova.dto;

public record ReporteVacunacionDTO(
        String cedula,
        String nombres,
        String apellidos,
        String cargo,
        String sede,
        String arl,
        String eps,
        String perfilVacunacion,
        String detalleVacunas,
        String estadoSemaforo
) {}