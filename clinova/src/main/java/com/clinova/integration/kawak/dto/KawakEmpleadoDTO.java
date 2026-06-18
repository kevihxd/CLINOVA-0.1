package com.clinova.integration.kawak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que representa un empleado/hoja de vida tal como lo devuelve la API de Kawak.
 * Ajusta los @JsonProperty según el Swagger de Kawak.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KawakEmpleadoDTO(

        @JsonProperty("id")
        Long id,

        @JsonProperty("nombres")
        String nombres,

        @JsonProperty("apellidos")
        String apellidos,

        @JsonProperty("cedula")
        String cedula,

        @JsonProperty("fecha_nacimiento")
        String fechaNacimiento,

        @JsonProperty("fecha_ingreso")
        String fechaIngreso,

        @JsonProperty("fecha_retiro")
        String fechaRetiro,

        @JsonProperty("estado")
        String estado,

        @JsonProperty("cargo")
        String cargo,

        @JsonProperty("tipo_contrato")
        String tipoContrato,

        @JsonProperty("correo_electronico")
        String correoElectronico,

        @JsonProperty("telefono")
        String telefono,

        @JsonProperty("direccion_residencia")
        String direccionResidencia,

        @JsonProperty("arl")
        String arl,

        @JsonProperty("eps")
        String eps,

        @JsonProperty("afp")
        String afp,

        @JsonProperty("caja_compensacion")
        String cajaCompensacion,

        @JsonProperty("salario")
        Double salario,

        @JsonProperty("sede")
        String sede,

        @JsonProperty("perfil_vacunacion")
        String perfilVacunacion,

        @JsonProperty("contacto_emergencia")
        String contactoEmergencia,

        @JsonProperty("telefono_contacto_emergencia")
        String telefonoContactoEmergencia
) {}
