package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoSemaforoDTO {
    private Long cursoMaestroId;
    private String nombreCurso;
    private String fechaRealizacion;
    private String fechaExpiracion;
    private Long diasRestantes;
    private String estado;
}
