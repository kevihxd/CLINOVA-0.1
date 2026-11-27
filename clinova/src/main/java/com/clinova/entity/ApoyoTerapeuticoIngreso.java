package com.clinova.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "apoyo_terapeutico_ingresos",
    indexes = {
        @Index(name = "idx_at_ing_numero", columnList = "numero_ingreso"),
        @Index(name = "idx_at_ing_doc", columnList = "documento_paciente"),
        @Index(name = "idx_at_ing_nombre", columnList = "nombre_paciente")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApoyoTerapeuticoIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ingreso", nullable = false, unique = true, length = 60)
    private String numeroIngreso;

    @Column(name = "tipo_documento_paciente", length = 20)
    private String tipoDocumentoPaciente;

    @Column(name = "documento_paciente", length = 40)
    private String documentoPaciente;

    @Column(name = "nombre_paciente", nullable = false, length = 255)
    private String nombrePaciente;

    @Column(name = "especialidad", length = 100)
    private String especialidad;

    @Column(name = "sede", length = 120)
    private String sede;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "estado", nullable = false, length = 30)
    @Builder.Default
    private String estado = "ACTIVO";

    @Column(name = "origen_dato", nullable = false, length = 30)
    @Builder.Default
    private String origenDato = "MANUAL";

    @Column(name = "kubapp_ref_id", length = 100)
    private String kubappRefId;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_por", length = 150)
    private String creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "ingreso", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<ApoyoTerapeuticoAdjunto> adjuntos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.fechaCreacion == null) this.fechaCreacion = now;
        if (this.fechaActualizacion == null) this.fechaActualizacion = now;
        if (this.estado == null || this.estado.isBlank()) this.estado = "ACTIVO";
        if (this.origenDato == null || this.origenDato.isBlank()) this.origenDato = "MANUAL";
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
