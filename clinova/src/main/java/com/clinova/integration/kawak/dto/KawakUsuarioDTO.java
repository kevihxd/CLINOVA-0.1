package com.clinova.integration.kawak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Campos exactos que devuelve GET /api/v1/usuarios según el Swagger de Kawak.
 *
 * Ejemplo:
 * {
 *   "id": 1,
 *   "nombre": "John",
 *   "apellido": "Doe",
 *   "login": "johndoe",
 *   "cargo": "1 - Gerente",
 *   "tipo_usuario": "Usuario Estandar",
 *   "email": "john.doe@example.com",
 *   "idioma": "ES",
 *   "autenticacion": "bd",
 *   "area_dependencia": "Area 1",
 *   "jefe_inmediato": "2 - Jane Smith",
 *   "sedes": "1 - Sede Principal",
 *   "grupos_distribucion": "1 - Grupo A"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KawakUsuarioDTO(

        @JsonProperty("id")
        Long id,

        @JsonProperty("nombre")
        String nombre,

        @JsonProperty("apellido")
        String apellido,

        @JsonProperty("login")
        String login,

        @JsonProperty("cargo")
        String cargo,

        @JsonProperty("tipo_usuario")
        String tipoUsuario,

        @JsonProperty("email")
        String email,

        @JsonProperty("idioma")
        String idioma,

        @JsonProperty("autenticacion")
        String autenticacion,

        @JsonProperty("area_dependencia")
        String areaDependencia,

        @JsonProperty("jefe_inmediato")
        String jefeInmediato,

        @JsonProperty("sedes")
        String sedes,

        @JsonProperty("grupos_distribucion")
        String gruposDistribucion
) {}
