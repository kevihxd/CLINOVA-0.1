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
public class RegistroVacunaResponseDTO {
    private Long id;
    private PersonaResumenDTO persona;
    private VacunaDTO vacuna;
    private String detalleDosis;
    private LocalDate fechaAplicacion;
}