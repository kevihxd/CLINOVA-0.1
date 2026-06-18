package com.clinova.integration.kawak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que representa un documento tal como lo devuelve la API de Kawak.
 * Ajusta los @JsonProperty según el Swagger de Kawak.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KawakDocumentoDTO(

        @JsonProperty("id")
        Long id,

        @JsonProperty("nombre")
        String nombre,

        @JsonProperty("codigo")
        String codigo,

        @JsonProperty("tipo")
        String tipo,

        @JsonProperty("proceso")
        String proceso,

        @JsonProperty("estado")
        String estado,

        @JsonProperty("version")
        String version,

        @JsonProperty("sede")
        String sede,

        @JsonProperty("alcance")
        String alcance,

        @JsonProperty("confidencialidad")
        String confidencialidad,

        @JsonProperty("meses_revision")
        Integer mesesRevision,

        @JsonProperty("elabora")
        String elabora,

        @JsonProperty("revisa")
        String revisa,

        @JsonProperty("aprueba")
        String aprueba,

        @JsonProperty("visualizacion")
        String visualizacion,

        @JsonProperty("impresion")
        String impresion,

        @JsonProperty("descarga_original")
        String descargaOriginal,

        @JsonProperty("descarga_pdf")
        String descargaPdf,

        @JsonProperty("normas")
        String normas,

        @JsonProperty("otros_procesos")
        String otrosProcesos,

        @JsonProperty("fecha_elaboracion")
        String fechaElaboracion,

        @JsonProperty("fecha_revision")
        String fechaRevision,

        @JsonProperty("fecha_aprobacion")
        String fechaAprobacion,

        @JsonProperty("ubicacion")
        String ubicacion,

        @JsonProperty("plantilla")
        String plantilla
) {}
