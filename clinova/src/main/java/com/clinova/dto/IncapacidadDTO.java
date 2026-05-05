package com.clinova.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

public record IncapacidadDTO(
    Long id,
    Long usuarioId,
    String epsArl,
    String tipoIncapacidad,
    String codigo,
    String dx,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    Integer diasOtorgados,
    Integer diasAprobados,
    LocalDate fechaReporteTH,
    LocalDate fechaRadicado,
    String estado,
    String numeroRadicacion,
    BigDecimal ibc,
    Integer diasPagadosIps,
    BigDecimal valorLiquidadoIps,
    Integer diasPagadosEps,
    BigDecimal valorLiquidadoEps,
    Integer diasPagadosArl,
    String campo30,
    String campo60,
    String campo90,
    String campo180,
    String observaciones,
    String nombreArchivo,
    String rutaArchivo,
    BigDecimal valorPago,
    LocalDate fechaPago,
    String numeroComprobantePago
) {}
