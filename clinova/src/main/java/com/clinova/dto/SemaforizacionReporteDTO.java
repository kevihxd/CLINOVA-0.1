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
public class SemaforizacionReporteDTO {
    private Long usuarioId;
    private String nombreCompleto;
    private String documento;
    private String cargo;
    private String cursoRequerido;
    private String estadoCurso; // "VIGENTE", "POR_VENCER", "VENCIDO", "FALTANTE"
    private LocalDate fechaRealizacion;
    private LocalDate fechaVencimiento;
    private String soporteUrl;
    private String estadoEmpleado;
}