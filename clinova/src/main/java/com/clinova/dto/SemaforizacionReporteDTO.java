package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemaforizacionReporteDTO {
    private Long hojaVidaId;
    private Long usuarioId;
    private String nombreCompleto;
    private String identificacion;
    private String cargo;
    private String sede;
    
    // Contrato
    private String tipoContrato;
    private Long valorContrato;
    private String tiempoDuracionContrato;
    private String fechaContratoInicial;
    private String fechaFinalizacionContrato;
    private Long diasFinalizacionContrato;
    private String estadoContrato;
    
    // Cursos
    private List<CursoSemaforoDTO> cursos;
}
