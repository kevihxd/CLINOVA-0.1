package com.clinova.dto;

public record ReporteDocumentoDTO(
        Long id,
        String codigo,
        String nombre,
        String tipo,
        String proceso,
        String sede,
        String version,
        String estado,
        String fechaElaboracion,
        String fechaRevision,
        String fechaAprobacion,
        String elabora,
        String revisa,
        String aprueba,
        String metodoCreacion,
        String alcance,
        String confidencialidad,
        String normas,
        Integer mesesRevision,
        String visualizacion
) {}
