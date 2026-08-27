package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kawak_id", unique = true)
    private Long kawakId;

    private String alcance;
    private String codigo;
    private String confidencialidad;
    @Column(name = "control_cambios", columnDefinition = "LONGTEXT")
    private String controlCambios;
    @Column(columnDefinition = "LONGTEXT")
    private String descripcion;
    private String estado;

    @Column(name = "meses_revision")
    private Integer mesesRevision;

    @Column(name = "metodo_creacion")
    private String metodoCreacion;

    private String nombre;
    private String proceso;
    private String sede;
    private String tipo;
    private String ubicacion;
    
    @Column(name = "ubicacion_pdf")
    private String ubicacionPdf;
    
    private String version;

    @Column(name = "fecha_elaboracion")
    private String fechaElaboracion;

    @Column(name = "fecha_revision")
    private String fechaRevision;

    @Column(name = "fecha_aprobacion")
    private String fechaAprobacion;

    @Column(length = 1000)
    private String aprueba;

    @Column(name = "descarga_original", length = 1000)
    private String descargaOriginal;

    @Column(name = "descarga_pdf", length = 1000)
    private String descargaPdf;

    @Column(length = 1000)
    private String elabora;

    @Column(length = 1000)
    private String impresion;

    @Column(length = 1000)
    private String normas;

    @Column(name = "otros_procesos", length = 1000)
    private String otrosProcesos;

    private String plantilla;

    @Column(length = 1000)
    private String revisa;

    @Column(length = 1000)
    private String visualizacion;

    // --- Campos de Retención Documental (TRD) y Configuración Kawak (Transient para compatibilidad con BD) ---
    @jakarta.persistence.Transient
    private String requiereAprobacion;

    @jakarta.persistence.Transient
    private String permisoVisualizacionRegistros;

    @jakarta.persistence.Transient
    private String edicionAprobadores;

    @jakarta.persistence.Transient
    private String edicionOtros;

    @jakarta.persistence.Transient
    private String edicionSolicitante;

    @jakarta.persistence.Transient
    private String reiniciarCicloAprobacion;

    @jakarta.persistence.Transient
    private String requiereSeguimiento;

    @jakarta.persistence.Transient
    private String evaluarGestion;

    @jakarta.persistence.Transient
    private String retencionDonde;

    @jakarta.persistence.Transient
    private String retencionComo;

    @jakarta.persistence.Transient
    private String retencionRecuperacion;

    @jakarta.persistence.Transient
    private String retencionTiempo;

    @jakarta.persistence.Transient
    private String disposicionFinal;

    @jakarta.persistence.Transient
    private String quienDiligencia;

    @jakarta.persistence.Transient
    private String quienProtege;

    @jakarta.persistence.Transient
    private String quienDisposicion;

    @jakarta.persistence.Transient
    private String logo;

    // --- Archivo Físico Kawak Backup ---
    @Column(name = "ruta_archivo_local", length = 500)
    private String rutaArchivoLocal;

    @Column(name = "extension_archivo", length = 10)
    private String extensionArchivo;

    // --- Campos de Estado y Cálculo ---
    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("diasFaltantes")
    private Integer diasFaltantes;

    @PostLoad
    private void calcularDiasFaltantes() {
        String fechaBase = (fechaAprobacion != null && !fechaAprobacion.trim().isEmpty()) ? fechaAprobacion
                : (fechaRevision != null && !fechaRevision.trim().isEmpty()) ? fechaRevision
                : fechaElaboracion;
        int meses = (mesesRevision != null && mesesRevision > 0) ? mesesRevision : 12;

        if (fechaBase != null && !fechaBase.trim().isEmpty() && !"N/A".equalsIgnoreCase(fechaBase.trim())) {
            try {
                String clean = fechaBase.trim();
                int spaceIdx = clean.indexOf(' ');
                if (spaceIdx > 0) clean = clean.substring(0, spaceIdx);
                int tIdx = clean.indexOf('T');
                if (tIdx > 0) clean = clean.substring(0, tIdx);

                java.time.LocalDate fecha = null;
                String[] parts = clean.split("[/-]");
                if (parts.length == 3) {
                    int p0 = Integer.parseInt(parts[0]);
                    int p1 = Integer.parseInt(parts[1]);
                    int p2 = Integer.parseInt(parts[2]);

                    int year, month, day;
                    if (p0 > 1000) {
                        year = p0;
                        if (p1 > 12) { day = p1; month = p2; }
                        else { month = p1; day = p2; }
                    } else if (p2 > 1000) {
                        year = p2;
                        if (p0 > 12) { day = p0; month = p1; }
                        else if (p1 > 12) { day = p1; month = p0; }
                        else { day = p0; month = p1; }
                    } else {
                        year = 0; month = 0; day = 0;
                    }

                    if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                        fecha = java.time.LocalDate.of(year, month, day);
                    }
                }

                if (fecha != null) {
                    java.time.LocalDate vencimiento = fecha.plusMonths(meses);
                    this.diasFaltantes = (int) java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), vencimiento);
                } else {
                    this.diasFaltantes = null;
                }
            } catch (Exception e) {
                this.diasFaltantes = null;
            }
        }
    }
}