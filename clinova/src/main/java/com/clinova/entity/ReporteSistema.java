package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_sistema")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tipo; // 'BUG', 'ERROR', 'REQUERIMIENTO'

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 100)
    private String modulo;

    @Column(length = 20)
    private String prioridad; // 'BAJA', 'MEDIA', 'ALTA', 'URGENTE'

    @Column(name = "url_origen", length = 255)
    private String urlOrigen;

    @Column(name = "usuario_reporta", length = 100)
    private String usuarioReporta;

    @Column(length = 50)
    private String estado; // 'PENDIENTE', 'EN_REVISION', 'RESUELTO'

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = "PENDIENTE";
        }
        if (this.prioridad == null) {
            this.prioridad = "MEDIA";
        }
    }
}
