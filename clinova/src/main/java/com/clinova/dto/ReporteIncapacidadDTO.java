package com.clinova.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReporteIncapacidadDTO(
    String nombre,
    String tipoDocumento,
    String numeroDocumento,
    String cargo,
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
    String hipervinculo,
    BigDecimal valorPago,
    LocalDate fechaPago,
    String numeroComprobantePago
) {}
