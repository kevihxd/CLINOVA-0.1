package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaResumenDTO {
    private Long id;
    private String numeroDocumento;
    private String primerNombre;
    private String primerApellido;
}