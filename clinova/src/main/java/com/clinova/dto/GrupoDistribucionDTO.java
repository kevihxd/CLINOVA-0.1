package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoDistribucionDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private Integer totalIntegrantes;
    private LocalDateTime fechaCreacion;
    private List<Long> integrantesIds;
    private List<String> integrantesNombres;
    private List<Long> cargosIds;
    private List<String> cargosNombres;
}
