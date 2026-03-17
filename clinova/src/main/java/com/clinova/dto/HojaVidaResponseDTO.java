package com.clinova.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HojaVidaResponseDTO(
        Long id,
        String nombres,
        String apellidos,
        String cedula,
        LocalDate fechaNacimiento,
        String direccionResidencia,
        String telefono,
        String contactoEmergencia,
        String telefonoContactoEmergencia,
        String arl,
        String eps,
        String afp,
        String fotoUrl,
        LocalDate fechaIngreso,
        String estado,
        String tipoContrato,
        LocalDate fechaRetiro,
        String correoElectronico,
        String pesv,
        Long usuarioId,
        Long responsableEvaluacionId,
        LocalDateTime fechaUltimaEdicion,
        String usuarioUltimaEdicion,
        List<CargoDTO> cargos,
        List<SedeDTO> sedes
) {}