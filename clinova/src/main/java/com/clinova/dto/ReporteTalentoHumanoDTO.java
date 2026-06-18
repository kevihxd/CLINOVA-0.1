package com.clinova.dto;

import java.time.LocalDate;

public record ReporteTalentoHumanoDTO(
        String cedula,
        String nombres,
        String apellidos,
        String cargo,
        String sede,
        String tipoContrato,
        String estado,
        String arl,
        String eps,
        String afp,
        String cajaCompensacion,
        Double salario,
        String subsidioTransporte,
        LocalDate fechaIngreso,
        LocalDate fechaRetiro,
        String motivoRetiro,
        String telefono,
        String correoElectronico,
        String direccionResidencia,
        String contactoEmergencia,
        String telefonoContactoEmergencia
) {}