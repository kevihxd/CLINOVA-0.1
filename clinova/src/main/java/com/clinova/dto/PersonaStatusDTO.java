package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaStatusDTO {
    private Long personaId;
    private String nombreCompleto;
    private String numeroDocumento;
    private String perfil;
    private List<RequisitoDetalleDTO> requisitos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequisitoDetalleDTO {
        private Long registroId;
        private Long vacunaId;
        private String nombre;
        private boolean completado;
        private LocalDate fechaAplicacion;
        private LocalDate fechaVencimiento;
        private boolean vencido;
        private String detalleDosis;
    }
}