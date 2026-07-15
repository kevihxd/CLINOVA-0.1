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
        if (this.fechaAprobacion != null && !this.fechaAprobacion.trim().isEmpty() && this.mesesRevision != null) {
            try {
                // Suponiendo formato dd/MM/yyyy
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                java.time.LocalDate fechaAprob = java.time.LocalDate.parse(this.fechaAprobacion, formatter);
                java.time.LocalDate vencimiento = fechaAprob.plusMonths(this.mesesRevision);
                this.diasFaltantes = (int) java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), vencimiento);
            } catch (Exception e) {
                this.diasFaltantes = null;
            }
        }
    }
}