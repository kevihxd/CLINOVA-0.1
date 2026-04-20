package com.clinova.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record HojaVidaRequestDTO(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no deben exceder los 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no deben exceder los 100 caracteres")
        String apellidos,

        @NotBlank(message = "La cédula es obligatoria")
        @Size(min = 5, max = 20, message = "La cédula debe tener entre 5 y 20 caracteres")
        String cedula,

        LocalDate fechaNacimiento,

        String direccionResidencia,
        String telefono,
        String contactoEmergencia,
        String telefonoContactoEmergencia,
        String arl,
        String eps,
        String afp,
        String cajaCompensacion,
        Double salario,
        String subsidioTransporte,

        LocalDate fechaIngreso,

        String estado,
        String tipoContrato,
        LocalDate fechaRetiro,
        String motivoRetiro,

        @Email(message = "Debe ser un correo electrónico válido")
        String correoElectronico,

        String pesv,
        Long usuarioId,
        Long responsableEvaluacionId,
        String usuarioUltimaEdicion,

        String perfilVacunacion,
        String detalleVacunas,

        List<Long> cargosIds,
        List<Long> sedesIds
) {}