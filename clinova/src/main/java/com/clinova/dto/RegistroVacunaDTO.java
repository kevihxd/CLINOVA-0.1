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
public class RegistroVacunaDTO {
    private Long id;
    private Long personaId;
    private String nombreVacuna;
    private String detalleDosis;
    private LocalDate fechaAplicacion;
    private LocalDate fechaVencimiento;
}