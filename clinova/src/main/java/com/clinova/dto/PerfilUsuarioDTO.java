package com.clinova.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PerfilUsuarioDTO {
    private Long id;
    private String username;
    private String rol;

    // Persona
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String tipoDocumento;
    private String numeroDocumento;
    private String fechaNacimiento;
    private String lugarNacimiento;
    private String direccionResidencia;
    private String numeroTelefono;
    private String correoElectronico;

    // Cargo
    private String cargoNombre;

    // HojaVida
    private String sedeNombre;
    private String fechaIngreso;
    private String fechaUltimaEdicion;
    private String estado;
}
