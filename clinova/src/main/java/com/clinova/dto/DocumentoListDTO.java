package com.clinova.dto;

/**
 * Data Transfer Object (DTO) optimizado para listar documentos.
 * Evita la transferencia de campos pesados (como textos largos, listas)
 * mejorando drásticamente los tiempos de respuesta del API.
 */
public record DocumentoListDTO(
        Long id,
        Long kawakId,
        String codigo,
        String nombre,
        String tipo,
        String proceso,
        String sede,
        String estado,
        String version,
        Integer mesesRevision,
        String metodoCreacion,
        String normas
) {
}
