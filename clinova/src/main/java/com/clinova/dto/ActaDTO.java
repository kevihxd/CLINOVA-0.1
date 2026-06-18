package com.clinova.dto;

import java.time.LocalDate;

public record ActaDTO(
        Long id,
        String titulo,
        String contenidoHtml,
        String estado,
        String tipo,
        String responsable,
        LocalDate fecha,
        String proceso,
        String sede,
        String fechaInicio,
        String horaInicio,
        String fechaFin,
        String horaFin,
        String lugar,
        String enlaceVirtual,
        String quienCita,
        Boolean confidencial,
        String elaborador,
        String area,
        String palabrasClave,
        String compromisosAprobacion,
        String convertirDocumento,
        String requiereAprobacionActa
) {}