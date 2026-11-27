package com.clinova.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "apoyo_terapeutico_adjuntos",
    indexes = {
        @Index(name = "idx_at_adj_ingreso", columnList = "ingreso_id"),
        @Index(name = "idx_at_adj_tipo", columnList = "tipo_adjunto"),
        @Index(name = "idx_at_adj_vencimiento", columnList = "fecha_vencimiento")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApoyoTerapeuticoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingreso_id", nullable = false)
    @JsonIgnore
    private ApoyoTerapeuticoIngreso ingreso;

    @Column(name = "tipo_adjunto", nullable = false, length = 40)
    private String tipoAdjunto; // CONSENTIMIENTO_INFORMADO | PLANILLA_FIRMAS | PLAN_CASERO

    @Column(name = "nombre_archivo_original", nullable = false, length = 255)
    private String nombreArchivoOriginal;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "fecha_documento", nullable = false)
    private LocalDate fechaDocumento;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "vigente", nullable = false)
    @Builder.Default
    private Boolean vigente = true;

    @Column(name = "sincronizado_kubapp", nullable = false)
    @Builder.Default
    private Boolean sincronizadoKubapp = false;

    @Column(name = "fecha_sync_kubapp")
    private LocalDateTime fechaSyncKubapp;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "subido_por", nullable = false, length = 150)
    private String subidoPor;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    protected void onCreate() {
        if (this.fechaSubida == null) this.fechaSubida = LocalDateTime.now();
        if (this.fechaDocumento == null) this.fechaDocumento = LocalDate.now();
        if (this.vigente == null) this.vigente = true;
        if (this.sincronizadoKubapp == null) this.sincronizadoKubapp = false;
    }
}
