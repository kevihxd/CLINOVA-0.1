package com.clinova.dto;

public record ReporteTalentoHumanoDTO(
        String cedula,
        String nombres,
        String apellidos,
        String cargo,
        String tipoContrato,
        String estado
) {}