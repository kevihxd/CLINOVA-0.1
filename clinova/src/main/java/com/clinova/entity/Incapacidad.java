package com.clinova.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "incapacidades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incapacidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private String epsArl;
    private String tipoIncapacidad;
    private String codigo;
    private String dx;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer diasOtorgados;
    private Integer diasAprobados;
    private LocalDate fechaReporteTH;
    private LocalDate fechaRadicado;

    private String estado;
    private String numeroRadicacion;

    private BigDecimal ibc;
    private Integer diasPagadosIps;
    private BigDecimal valorLiquidadoIps;
    private Integer diasPagadosEps;
    private BigDecimal valorLiquidadoEps;
    private Integer diasPagadosArl;

    @Column(name = "campo_30")
    private String campo30;

    @Column(name = "campo_60")
    private String campo60;

    @Column(name = "campo_90")
    private String campo90;

    @Column(name = "campo_180")
    private String campo180;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Archivo adjunto (PDF, etc.)
    private String nombreArchivo;
    private String rutaArchivo;

    private BigDecimal valorPago;
    private LocalDate fechaPago;
    private String numeroComprobantePago;

    /**
     * Calcula la cartera (Bucketing/Aging) dinámicamente en base a la fecha actual.
     * Retorna un arreglo: [campo30, campo60, campo90, campo180]
     */
    public String[] calcularCartera() {
        String c30 = "", c60 = "", c90 = "", c180 = "";
        if (this.fechaRadicado != null) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(this.fechaRadicado, LocalDate.now());
            BigDecimal liquidado = this.valorLiquidadoEps != null ? this.valorLiquidadoEps : BigDecimal.ZERO;
            BigDecimal pagado = this.valorPago != null ? this.valorPago : BigDecimal.ZERO;
            BigDecimal saldo = liquidado.subtract(pagado);

            if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                if (dias <= 30) {
                    c30 = saldo.toString();
                } else if (dias <= 60) {
                    c60 = saldo.toString();
                } else if (dias <= 90) {
                    c90 = saldo.toString();
                } else {
                    c180 = saldo.toString();
                }
            }
        }
        return new String[]{c30, c60, c90, c180};
    }
}
