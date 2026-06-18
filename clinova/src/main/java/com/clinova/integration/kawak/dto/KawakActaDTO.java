package com.clinova.integration.kawak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Campos exactos que devuelve GET /api/v1/actas según el Swagger de Kawak.
 *
 * Ejemplo de un registro:
 * {
 *   "id": 100000,
 *   "nombre": "Mi acta confidencial",
 *   "codigo": "MCO-##-#",
 *   "fecha_inicio": "2021-05-03 14:05:03",
 *   "feha_final": "2021-05-03 15:05:03",   <-- typo intencional de Kawak
 *   "sede": "Bogotá",
 *   "proceso": "Mejoramiento continuo",
 *   "quien_cita": "Gestión del Conocimiento",
 *   "elaborador": "Gestión del Conocimiento",
 *   "tipo": null,
 *   "AREA": null,
 *   "lugar": null,
 *   "es_confidencial": "Si",
 *   "estado": "Documento de trabajo",
 *   "requiere_aprobacion": "No",
 *   "convocados_y_asistentes": null,
 *   "contenido": "<p>|Esta es mi acta...</p>"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KawakActaDTO(

        @JsonProperty("id")
        Long id,

        @JsonProperty("nombre")
        String nombre,

        @JsonProperty("codigo")
        String codigo,

        @JsonProperty("fecha_inicio")
        String fechaInicio,

        // Kawak tiene un typo: "feha_final" (sin 'c')
        @JsonProperty("feha_final")
        String fechaFinal,

        @JsonProperty("sede")
        String sede,

        @JsonProperty("proceso")
        String proceso,

        @JsonProperty("quien_cita")
        String quienCita,

        @JsonProperty("elaborador")
        String elaborador,

        @JsonProperty("tipo")
        String tipo,

        // Kawak envía "AREA" en mayúsculas
        @JsonProperty("AREA")
        String area,

        @JsonProperty("lugar")
        String lugar,

        @JsonProperty("es_confidencial")
        String esConfidencial,

        @JsonProperty("estado")
        String estado,

        @JsonProperty("requiere_aprobacion")
        String requiereAprobacion,

        @JsonProperty("convocados_y_asistentes")
        String convocadosYAsistentes,

        @JsonProperty("contenido")
        String contenido
) {}
