package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDTO {

        private String username;
        private String password;
        private String rol;
        private Long cargoId;

        private String tipoDocumento;
        private String numeroDocumento;
        private String primerNombre;
        private String segundoNombre;
        private String primerApellido;
        private String segundoApellido;
        private String fechaNacimiento;
        private String direccionResidencia;
        private String numeroTelefono;
        private String lugarNacimiento;
        private String correoElectronico;
        private String perfilVacunacion;

        private String arl;
        private String eps;
        private String afp;
        private String cajaCompensacion;
        private String fechaIngreso;
        private String tipoContrato;
        private Long sedeId;
        private Double salario;
        private String subsidioTransporte;
        private String estado;
        private String fechaRetiro;
        private String pesvFecha;
        private String motivoRetiro;
        private Long responsableEvaluacionId;
}