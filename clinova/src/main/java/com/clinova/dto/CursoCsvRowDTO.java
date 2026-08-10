package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoCsvRowDTO {
    private String documentoEmpleado;
    private String nombreCurso;
    private LocalDate fechaRealizacion;
    private LocalDate fechaVencimientoFija;
    private int lineaArchivo;
}
